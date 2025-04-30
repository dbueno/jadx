package jadx.core.dex.attributes.nodes;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.attributes.AType;

public class BytecodeInfoAttr extends PinnedAttribute implements ICodeAnnotation {
	private String file;
	private int offset;
	private int length;
	private String debug;

	public BytecodeInfoAttr(String file, int offset, int length) {
		this.file = file;
		this.offset = offset;
		this.length = length;
	}

	public String getFile() {
		return file;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	// XXX remove
	public String getDebug() {
		return debug;
	}

	public void setDebug(String debug) {
		this.debug = debug;
	}

	@Override
	public AType<BytecodeInfoAttr> getAttrType() {
		return AType.BYTECODE_INFO;
	}

	@Override
	public ICodeAnnotation.AnnType getAnnType() {
		return ICodeAnnotation.AnnType.BYTECODE;
	}

	@Override
	public String toString() {
		return "BYTECODE_INFO: " + file + ":" + offset + ":" + length;
	}
}
