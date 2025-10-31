package jadx.core.dex.visitors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.BytecodeInfoAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class SaveCode {
	private static final Logger LOG = LoggerFactory.getLogger(SaveCode.class);

	private SaveCode() {
	}

	public static void save(File dir, ClassNode cls, ICodeInfo code) {
		if (cls.contains(AFlag.DONT_GENERATE)) {
			return;
		}
		if (code == null) {
			throw new JadxRuntimeException("Code not generated for class " + cls.getFullName());
		}
		if (code == ICodeInfo.EMPTY) {
			return;
		}
		String codeStr = code.getCodeStr();
		if (codeStr.isEmpty()) {
			return;
		}
		JadxArgs args = cls.root().getArgs();
		if (args.isSkipFilesSave()) {
			return;
		}
		String fileName = cls.getClassInfo().getAliasFullPath() + getFileExtension(cls.root());
		if (!args.getSecurity().isValidEntryName(fileName)) {
			return;
		}
		save(codeStr, new File(dir, fileName));
		String sarifFileName = cls.getClassInfo().getAliasFullPath() + ".json";
		saveDecompMap(code, new File(dir, ".maps"), sarifFileName, fileName);
	}

	public static void save(ICodeInfo codeInfo, File file) {
		save(codeInfo.getCodeStr(), file);
	}

	public static void save(String code, File file) {
		File outFile = FileUtils.prepareFile(file);
		try (PrintWriter out = new PrintWriter(outFile, StandardCharsets.UTF_8)) {
			out.println(code);
		} catch (Exception e) {
			LOG.error("Save file error", e);
		}
	}

	public static String getFileExtension(RootNode root) {
		JadxArgs.OutputFormatEnum outputFormat = root.getArgs().getOutputFormat();
		switch (outputFormat) {
			case JAVA:
				return ".java";

			case JSON:
				return ".json";

			default:
				throw new JadxRuntimeException("Unknown output format: " + outputFormat);
		}
	}

	private static String formatBytecodeInfo(BytecodeInfoAttr bc) {
		return "  \"binary\": [{\n" +
				"    \"physicalLocation\": {\n" +
				"      \"artifactLocation\": { \"uri\": \"" + bc.getFile() + "\", \"uriBaseId\": \"BINROOT\" },\n" +
				"      \"region\": { \"byteOffset\": " + bc.getOffset() + ", \"byteLength\": " + bc.getLength() + " }\n" +
				"    }\n" +
				"  }]";
	}

	private static String formatDecompInfo(String filename, int line) {
		return "  \"source\": [{\n" +
				"    \"physicalLocation\": {\n" +
				"      \"artifactLocation\": { \"uri\": \"" + filename + "\", \"uriBaseId\": \"SRCROOT\" },\n" +
				"      \"region\": { \"startLine\": " + line + " }\n" +
				"    }\n" +
				"  }]";
	}

	public static void saveDecompMap(ICodeInfo code, File dir, String outFile, String javaFile) {
		File filePath = FileUtils.prepareFile(new File(dir, outFile));
		String contents =
				code.getCodeMetadata().getDecompMap().entrySet().stream()
						.map(entry -> entry.getValue().stream()
								.map(annot -> "{\n" +
										formatBytecodeInfo((BytecodeInfoAttr) annot) + ",\n" +
										formatDecompInfo(javaFile, entry.getKey()) + "\n}")
								.collect(Collectors.joining(", ")))
						.collect(Collectors.joining(", "));
		try (FileWriter fileWriter = new FileWriter(filePath)) {
			fileWriter.write("{\n");
			fileWriter.write("\"version\": 1,\n");
			fileWriter.write("\"tool\": \"jadx\",\n");
			fileWriter.write("\"mappings\": [\n");
			fileWriter.write(contents);
			fileWriter.write("\n]\n");
			fileWriter.write("}");
		} catch (IOException e) {
			System.err.println("An error occurred while writing to the file: " + e.getMessage());
		}
	}
}
