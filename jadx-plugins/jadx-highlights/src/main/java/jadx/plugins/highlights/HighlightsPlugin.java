package jadx.plugins.highlights;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.gui.tree.ITreeNode;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.JadxGuiContext;

public class HighlightsPlugin implements JadxPlugin {
	private static final Logger LOG = LoggerFactory.getLogger(HighlightsPlugin.class);

	private static final String PLUGIN_ID = "highlights";
	private static final String MENU_TITLE = "Load highlights file";
	private static final Gson GSON = new Gson();

	private JadxPluginContext context;
	private JadxGuiContext guiContext;
	private volatile Map<String, Set<Integer>> loadedHighlights = Map.of();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(PLUGIN_ID, "Highlights Loader", "load source line highlights from a JSON file");
	}

	@Override
	public void init(JadxPluginContext context) {
		this.context = context;
		JadxGuiContext gui = context.getGuiContext();
		if (gui == null) {
			return;
		}
		this.guiContext = gui;
		gui.addTreeNodeDecorator(this::buildTreeLabelSuffix, this::buildTreeTooltipSuffix);
		gui.addActiveNodeChangeListener(this::applyHighlightsForActiveNode);
		gui.addMenuAction(MENU_TITLE, this::loadHighlightsFile);
	}

	private void loadHighlightsFile() {
		Path highlightsPath = chooseHighlightsPath();
		if (highlightsPath == null) {
			return;
		}
		HighlightsFile highlightsFile;
		try {
			highlightsFile = readHighlightsFile(highlightsPath);
		} catch (Exception e) {
			showError("Failed to load highlights file:\n" + highlightsPath + "\n\n" + e.getMessage());
			return;
		}
		Map<String, Set<Integer>> groupedHighlights;
		try {
			groupedHighlights = groupHighlights(highlightsFile);
		} catch (Exception e) {
			showError("Invalid highlights file:\n" + highlightsPath + "\n\n" + e.getMessage());
			return;
		}
		loadedHighlights = groupedHighlights;
		guiContext.reloadTree();
		applyHighlightsForActiveNode();
		showInfo(buildLoadSummaryMessage(highlightsPath, groupedHighlights));
	}

	private @Nullable String buildTreeLabelSuffix(ITreeNode treeNode) {
		Set<Integer> lines = resolveLinesForTreeNode(treeNode);
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		return " [H]";
	}

	private @Nullable String buildTreeTooltipSuffix(ITreeNode treeNode) {
		Set<Integer> lines = resolveLinesForTreeNode(treeNode);
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		return "Highlights: lines " + formatLines(lines);
	}

	private void applyHighlightsForActiveNode() {
		Map<String, Set<Integer>> highlights = loadedHighlights;
		if (highlights.isEmpty()) {
			return;
		}
		ICodeNodeRef nodeRef = guiContext.getActiveNode();
		if (nodeRef == null) {
			return;
		}
		JavaNode javaNode = context.getDecompiler().getJavaNodeByRef(nodeRef);
		if (!(javaNode instanceof JavaClass)) {
			return;
		}
		JavaClass javaClass = (JavaClass) javaNode;
		Set<Integer> requestedLines = resolveLinesForClass(javaClass, highlights);
		if (requestedLines == null || requestedLines.isEmpty()) {
			return;
		}
		JTextArea textArea = guiContext.getActiveTextArea();
		if (textArea == null) {
			return;
		}
		Set<Integer> editorLines = mapRequestedLinesToEditorLines(javaClass, requestedLines);
		try {
			highlightLines(textArea, editorLines);
		} catch (BadLocationException e) {
			LOG.warn("Failed to apply highlights for class {}", javaClass.getFullName(), e);
		}
	}

	private void highlightLines(JTextArea textArea, Set<Integer> lines) throws BadLocationException {
		try {
			runOnUiThreadAndWait(() -> {
				guiContext.clearHighlights();
				boolean caretMoved = false;
				for (Integer line : lines) {
					if (line == null || line <= 0) {
						continue;
					}
					int lineIndex = line - 1;
					if (lineIndex >= textArea.getLineCount()) {
						continue;
					}
					try {
						int start = textArea.getLineStartOffset(lineIndex);
						int end = textArea.getLineEndOffset(lineIndex);
						guiContext.addHighlight(start, end);
						if (!caretMoved) {
							textArea.setCaretPosition(start);
							caretMoved = true;
						}
					} catch (BadLocationException e) {
						throw new RuntimeException(e);
					}
				}
			});
		} catch (RuntimeException e) {
			if (e.getCause() instanceof BadLocationException) {
				throw (BadLocationException) e.getCause();
			}
			throw e;
		}
	}

	private Map<String, Set<Integer>> groupHighlights(HighlightsFile highlightsFile) {
		if (highlightsFile == null || highlightsFile.highlights == null || highlightsFile.highlights.isEmpty()) {
			throw new IllegalArgumentException("Missing 'highlights' entries");
		}
		Map<String, Set<Integer>> grouped = new LinkedHashMap<>();
		for (HighlightEntry highlight : highlightsFile.highlights) {
			if (highlight == null || highlight.path == null || highlight.path.isBlank()) {
				throw new IllegalArgumentException("Each highlight entry must define a non-empty 'path'");
			}
			if (highlight.lines == null || highlight.lines.isEmpty()) {
				throw new IllegalArgumentException("Highlight entry for '" + highlight.path + "' must define 'lines'");
			}
			grouped.computeIfAbsent(toClassName(highlight.path), key -> new LinkedHashSet<>())
					.addAll(highlight.lines);
		}
		return grouped;
	}

	private @Nullable Set<Integer> resolveLinesForClass(JavaClass javaClass, Map<String, Set<Integer>> highlights) {
		Set<Integer> lines = highlights.get(javaClass.getFullName());
		if (lines != null) {
			return lines;
		}
		return highlights.get(javaClass.getRawName());
	}

	private @Nullable Set<Integer> resolveLinesForTreeNode(ITreeNode treeNode) {
		Map<String, Set<Integer>> highlights = loadedHighlights;
		if (highlights.isEmpty()) {
			return null;
		}
		ICodeNodeRef nodeRef = treeNode.getCodeNodeRef();
		if (nodeRef == null) {
			return null;
		}
		JavaNode javaNode = context.getDecompiler().getJavaNodeByRef(nodeRef);
		if (!(javaNode instanceof JavaClass)) {
			return null;
		}
		return resolveLinesForClass((JavaClass) javaNode, highlights);
	}

	private String formatLines(Set<Integer> lines) {
		return lines.stream()
				.filter(line -> line != null && line > 0)
				.sorted()
				.map(String::valueOf)
				.collect(Collectors.joining(", "));
	}

	private Set<Integer> mapRequestedLinesToEditorLines(JavaClass javaClass, Set<Integer> requestedLines) {
		Map<Integer, Integer> lineMapping = javaClass.getCodeInfo().getCodeMetadata().getLineMapping();
		if (lineMapping.isEmpty()) {
			return requestedLines;
		}
		Set<Integer> editorLines = new LinkedHashSet<>();
		for (Integer requestedLine : requestedLines) {
			if (requestedLine == null || requestedLine <= 0) {
				continue;
			}
			boolean mapped = false;
			for (Map.Entry<Integer, Integer> entry : lineMapping.entrySet()) {
				if (requestedLine.equals(entry.getValue())) {
					editorLines.add(entry.getKey());
					mapped = true;
				}
			}
			if (!mapped) {
				editorLines.add(requestedLine);
			}
		}
		return editorLines;
	}

	private static String toClassName(String sourcePath) {
		String normalized = sourcePath.replace('\\', '/');
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		if (!normalized.endsWith(".java")) {
			throw new IllegalArgumentException("Path must point to a .java file: " + sourcePath);
		}
		return normalized.substring(0, normalized.length() - ".java".length()).replace('/', '.');
	}

	private HighlightsFile readHighlightsFile(Path highlightsPath) throws IOException {
		try (Reader reader = Files.newBufferedReader(highlightsPath)) {
			HighlightsFile file = GSON.fromJson(reader, HighlightsFile.class);
			if (file == null) {
				throw new IllegalArgumentException("File is empty");
			}
			return file;
		}
	}

	private @Nullable Path chooseHighlightsPath() {
		final Path[] selectedPath = new Path[1];
		runOnUiThreadAndWait(() -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle(MENU_TITLE);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
			int result = chooser.showOpenDialog(guiContext.getMainFrame());
			if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
				selectedPath[0] = chooser.getSelectedFile().toPath();
			}
		});
		return selectedPath[0];
	}

	private void showInfo(String message) {
		runOnUiThread(() -> JOptionPane.showMessageDialog(guiContext.getMainFrame(), message, "Highlights loaded",
				JOptionPane.INFORMATION_MESSAGE));
	}

	private void showError(String message) {
		LOG.warn("Highlights plugin error: {}", message);
		runOnUiThread(() -> JOptionPane.showMessageDialog(guiContext.getMainFrame(), message, "Highlights load failed",
				JOptionPane.ERROR_MESSAGE));
	}

	private String buildLoadSummaryMessage(Path highlightsPath, Map<String, Set<Integer>> groupedHighlights) {
		int lineCount = groupedHighlights.values().stream().mapToInt(Set::size).sum();
		return "Loaded highlights file:\n"
				+ highlightsPath.toAbsolutePath()
				+ "\n\nTracked files: " + groupedHighlights.size()
				+ "\nTracked lines: " + lineCount
				+ "\n\nHighlights will appear when you open the matching source files.";
	}

	private static void runOnUiThread(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
			return;
		}
		SwingUtilities.invokeLater(runnable);
	}

	private static void runOnUiThreadAndWait(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
			return;
		}
		try {
			SwingUtilities.invokeAndWait(runnable);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final class HighlightsFile {
		List<HighlightEntry> highlights;
	}

	private static final class HighlightEntry {
		String path;
		List<Integer> lines;
	}
}
