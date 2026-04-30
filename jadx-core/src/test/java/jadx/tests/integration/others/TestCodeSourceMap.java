package jadx.tests.integration.others;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.JadxInternalAccess;
import jadx.api.JavaClass;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCodeSourceMap extends SmaliTest {

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmali();
		assertThat(cls).code().containsOne("return \"a\".length();");

		JavaClass javaClass = JadxInternalAccess.convertClassNode(jadxDecompiler, cls);
		String code = javaClass.getCode();
		String[] lines = code.split("\\R");
		int returnLine = -1;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains("return \"a\".length();")) {
				returnLine = i + 1;
				break;
			}
		}
		assertThat(returnLine).isGreaterThan(0);

		List<Integer> offsets = javaClass.getCodeOffsetsForLine(returnLine);
		assertThat(offsets).contains(0, 2, 5, 6);
		assertThat(javaClass.getDecompiledLineForCodeOffset(0)).isEqualTo(returnLine);
		assertThat(javaClass.getDecompiledLineForCodeOffset(2)).isEqualTo(returnLine);
		assertThat(javaClass.getDecompiledLineForCodeOffset(5)).isEqualTo(returnLine);
		assertThat(javaClass.getDecompiledLineForCodeOffset(6)).isEqualTo(returnLine);
	}
}
