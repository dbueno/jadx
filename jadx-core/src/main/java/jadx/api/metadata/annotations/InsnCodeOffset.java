package jadx.api.metadata.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.api.metadata.ICodeAnnotation;
import jadx.core.dex.nodes.InsnNode;

public class InsnCodeOffset implements ICodeAnnotation {

	public static void attach(ICodeWriter code, InsnNode insn) {
		if (insn == null) {
			return;
		}
		if (code.isMetadataSupported()) {
			InsnCodeOffset ann = from(insn);
			if (ann != null) {
				code.attachLineAnnotation(ann);
			}
		}
	}

	public static void attach(ICodeWriter code, int offset) {
		if (offset >= 0 && code.isMetadataSupported()) {
			code.attachLineAnnotation(new InsnCodeOffset(offset));
		}
	}

	@Nullable
	public static InsnCodeOffset from(InsnNode insn) {
		int offset = insn.getOffset();
		if (offset < 0) {
			return null;
		}
		List<Integer> offsets = new ArrayList<>();
		insn.visitInsns(currentInsn -> {
			for (Integer sourceOffset : currentInsn.getSourceOffsets()) {
				if (!offsets.contains(sourceOffset)) {
					offsets.add(sourceOffset);
				}
			}
		});
		return new InsnCodeOffset(offset, offsets);
	}

	private final int offset;
	private final List<Integer> offsets;

	public InsnCodeOffset(int offset) {
		this(offset, Collections.singletonList(offset));
	}

	public InsnCodeOffset(int offset, List<Integer> offsets) {
		this.offset = offset;
		this.offsets = Collections.unmodifiableList(new ArrayList<>(offsets));
	}

	public int getOffset() {
		return offset;
	}

	public List<Integer> getOffsets() {
		return offsets;
	}

	@Override
	public AnnType getAnnType() {
		return AnnType.OFFSET;
	}

	@Override
	public String toString() {
		return "offset=" + offset + ", sourceOffsets=" + offsets;
	}
}
