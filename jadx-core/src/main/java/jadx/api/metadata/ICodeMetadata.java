package jadx.api.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.impl.CodeMetadataStorage;

public interface ICodeMetadata {

	ICodeMetadata EMPTY = CodeMetadataStorage.empty();

	@Nullable
	ICodeAnnotation getAt(int position);

	@Nullable
	ICodeAnnotation getClosestUp(int position);

	@Nullable
	ICodeAnnotation searchUp(int position, ICodeAnnotation.AnnType annType);

	@Nullable
	ICodeAnnotation searchUp(int position, int limitPos, ICodeAnnotation.AnnType annType);

	/**
	 * Iterate code annotations from {@code startPos} to smaller positions.
	 *
	 * @param visitor
	 *                return not null value to stop iterations
	 */
	@Nullable
	<T> T searchUp(int startPos, BiFunction<Integer, ICodeAnnotation, T> visitor);

	/**
	 * Iterate code annotations from {@code startPos} to higher positions.
	 *
	 * @param visitor
	 *                return not null value to stop iterations
	 */
	@Nullable
	<T> T searchDown(int startPos, BiFunction<Integer, ICodeAnnotation, T> visitor);

	/**
	 * Get current node at position (can be enclosing class or method)
	 */
	@Nullable
	ICodeNodeRef getNodeAt(int position);

	/**
	 * Any definition of class or method below position
	 */
	@Nullable
	ICodeNodeRef getNodeBelow(int position);

	Map<Integer, ICodeAnnotation> getAsMap();

	Map<Integer, Integer> getLineMapping();

	default Map<Integer, List<Integer>> getLineCodeOffsets() {
		return Collections.emptyMap();
	}

	default List<Integer> getCodeOffsetsForLine(int decompiledLine) {
		List<Integer> offsets = getLineCodeOffsets().get(decompiledLine);
		return offsets == null ? Collections.<Integer>emptyList() : offsets;
	}

	@Nullable
	default Integer getLineForCodeOffset(int codeOffset) {
		for (Map.Entry<Integer, List<Integer>> entry : getLineCodeOffsets().entrySet()) {
			if (entry.getValue().contains(codeOffset)) {
				return entry.getKey();
			}
		}
		return null;
	}
}
