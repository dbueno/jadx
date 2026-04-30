package jadx.core.dex.attributes.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;

public class SourceOffsetsAttr implements IJadxAttribute {

	private final List<Integer> offsets = new ArrayList<>(2);

	@Override
	public AType<SourceOffsetsAttr> getAttrType() {
		return AType.SOURCE_OFFSETS;
	}

	public void add(int offset) {
		if (offset < 0 || offsets.contains(offset)) {
			return;
		}
		offsets.add(offset);
	}

	public List<Integer> getOffsets() {
		return Collections.unmodifiableList(offsets);
	}

	@Override
	public String toString() {
		return "SOURCE_OFFSETS: " + offsets;
	}
}
