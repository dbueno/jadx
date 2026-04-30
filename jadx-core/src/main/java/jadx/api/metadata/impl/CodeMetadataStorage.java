package jadx.api.metadata.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeAnnotation.AnnType;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.utils.Utils;

public class CodeMetadataStorage implements ICodeMetadata {

	public static ICodeMetadata build(Map<Integer, Integer> lines, Map<Integer, ICodeAnnotation> map) {
		return build(lines, Collections.<Integer, List<Integer>>emptyMap(), map);
	}

	public static ICodeMetadata build(Map<Integer, Integer> lines, Map<Integer, List<Integer>> lineCodeOffsets,
			Map<Integer, ICodeAnnotation> map) {
		if (map.isEmpty() && lines.isEmpty() && lineCodeOffsets.isEmpty()) {
			return ICodeMetadata.EMPTY;
		}
		Comparator<Integer> reverseCmp = Comparator.comparingInt(Integer::intValue).reversed();
		NavigableMap<Integer, ICodeAnnotation> navMap = new TreeMap<>(reverseCmp);
		navMap.putAll(map);
		return new CodeMetadataStorage(lines, lineCodeOffsets, navMap);
	}

	public static ICodeMetadata empty() {
		return new CodeMetadataStorage(Collections.<Integer, Integer>emptyMap(),
				Collections.<Integer, List<Integer>>emptyMap(),
				Collections.<Integer, ICodeAnnotation>emptyNavigableMap());
	}

	// <decomp file line number> -> <dex debug line number>
	private final Map<Integer, Integer> lines;
	private final Map<Integer, List<Integer>> lineCodeOffsets;
	private final Map<Integer, Integer> offsetLines;

	// <character index into the file> -> <code annotation>
	// the key is what is returned by AbstractCodeArea#getCaretPos() when clicking in a code panel.
	private final NavigableMap<Integer, ICodeAnnotation> navMap;

	private CodeMetadataStorage(Map<Integer, Integer> lines, Map<Integer, List<Integer>> lineCodeOffsets,
			NavigableMap<Integer, ICodeAnnotation> navMap) {
		this.lines = lines;
		this.lineCodeOffsets = lineCodeOffsets;
		this.offsetLines = buildOffsetLines(lineCodeOffsets);
		this.navMap = navMap;
	}

	private static Map<Integer, Integer> buildOffsetLines(Map<Integer, List<Integer>> lineCodeOffsets) {
		if (lineCodeOffsets.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Integer, Integer> offsetLines = new TreeMap<>();
		for (Map.Entry<Integer, List<Integer>> entry : lineCodeOffsets.entrySet()) {
			Integer line = entry.getKey();
			for (Integer offset : entry.getValue()) {
				if (offset != null && !offsetLines.containsKey(offset)) {
					offsetLines.put(offset, line);
				}
			}
		}
		return offsetLines;
	}

	@Override
	public ICodeAnnotation getAt(int position) {
		return navMap.get(position);
	}

	@Override
	public @Nullable ICodeAnnotation getClosestUp(int position) {
		Map.Entry<Integer, ICodeAnnotation> entryBefore = navMap.higherEntry(position);
		return entryBefore != null ? entryBefore.getValue() : null;
	}

	@Override
	public @Nullable ICodeAnnotation searchUp(int position, AnnType annType) {
		for (ICodeAnnotation v : navMap.tailMap(position, true).values()) {
			if (v.getAnnType() == annType) {
				return v;
			}
		}
		return null;
	}

	@Override
	public @Nullable ICodeAnnotation searchUp(int position, int limitPos, AnnType annType) {
		for (ICodeAnnotation v : navMap.subMap(position, true, limitPos, true).values()) {
			if (v.getAnnType() == annType) {
				return v;
			}
		}
		return null;
	}

	@Override
	public <T> @Nullable T searchUp(int startPos, BiFunction<Integer, ICodeAnnotation, T> visitor) {
		for (Map.Entry<Integer, ICodeAnnotation> entry : navMap.tailMap(startPos, true).entrySet()) {
			T value = visitor.apply(entry.getKey(), entry.getValue());
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public <T> @Nullable T searchDown(int startPos, BiFunction<Integer, ICodeAnnotation, T> visitor) {
		NavigableMap<Integer, ICodeAnnotation> map = navMap.headMap(startPos, true).descendingMap();
		for (Map.Entry<Integer, ICodeAnnotation> entry : map.entrySet()) {
			T value = visitor.apply(entry.getKey(), entry.getValue());
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public ICodeNodeRef getNodeAt(int position) {
		int nesting = 0;
		for (ICodeAnnotation ann : navMap.tailMap(position, true).values()) {
			switch (ann.getAnnType()) {
				case END:
					nesting++;
					break;

				case DECLARATION:
					ICodeNodeRef node = ((NodeDeclareRef) ann).getNode();
					AnnType nodeType = node.getAnnType();
					if (nodeType == AnnType.CLASS || nodeType == AnnType.METHOD) {
						if (nesting == 0) {
							return node;
						}
						nesting--;
					}
					break;
			}
		}
		return null;
	}

	@Override
	public ICodeNodeRef getNodeBelow(int position) {
		for (ICodeAnnotation ann : navMap.headMap(position, true).descendingMap().values()) {
			if (ann.getAnnType() == AnnType.DECLARATION) {
				ICodeNodeRef node = ((NodeDeclareRef) ann).getNode();
				AnnType nodeType = node.getAnnType();
				if (nodeType == AnnType.CLASS || nodeType == AnnType.METHOD) {
					return node;
				}
			}
		}
		return null;
	}

	@Override
	public NavigableMap<Integer, ICodeAnnotation> getAsMap() {
		return navMap;
	}

	@Override
	public Map<Integer, Integer> getLineMapping() {
		return lines;
	}

	@Override
	public Map<Integer, List<Integer>> getLineCodeOffsets() {
		return lineCodeOffsets;
	}

	@Override
	public Integer getLineForCodeOffset(int codeOffset) {
		return offsetLines.get(codeOffset);
	}

	@Override
	public String toString() {
		return "CodeMetadata{\nlines=" + lines
				+ "\nlineCodeOffsets=" + lineCodeOffsets
				+ "\nannotations=\n " + Utils.listToString(navMap.descendingMap().entrySet(), "\n ") + "\n}";
	}
}
