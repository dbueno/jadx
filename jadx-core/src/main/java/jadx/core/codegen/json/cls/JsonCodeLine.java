package jadx.core.codegen.json.cls;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public class JsonCodeLine {
	private String code;
	private String offset;
	private List<String> offsets;
	private Integer sourceLine;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getOffset() {
		return offset;
	}

	public void setOffset(String offset) {
		this.offset = offset;
	}

	public List<String> getOffsets() {
		return offsets;
	}

	public void setOffsets(List<String> offsets) {
		this.offsets = offsets;
	}

	public Integer getSourceLine() {
		return sourceLine;
	}

	public void setSourceLine(@Nullable Integer sourceLine) {
		this.sourceLine = sourceLine;
	}
}
