package jadx.gui.plugins.context;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.tree.ITreeNode;

public class TreeNodeDecorator {
	private final Function<ITreeNode, @Nullable String> labelSuffix;
	private final @Nullable Function<ITreeNode, @Nullable String> tooltipSuffix;

	public TreeNodeDecorator(Function<ITreeNode, @Nullable String> labelSuffix,
			@Nullable Function<ITreeNode, @Nullable String> tooltipSuffix) {
		this.labelSuffix = labelSuffix;
		this.tooltipSuffix = tooltipSuffix;
	}

	public @Nullable String getLabelSuffix(ITreeNode node) {
		return labelSuffix.apply(node);
	}

	public @Nullable String getTooltipSuffix(ITreeNode node) {
		if (tooltipSuffix == null) {
			return null;
		}
		return tooltipSuffix.apply(node);
	}
}
