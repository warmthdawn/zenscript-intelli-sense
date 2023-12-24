package treesitter;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class TSTreeCursor extends Structure {
	/** C type : const void* */
	public Pointer tree;
	/** C type : const void* */
	public Pointer id;
	/** C type : uint32_t[2] */
	public int[] context = new int[2];
	public TSTreeCursor() {
		super();
	}
	protected List<String> getFieldOrder(){
		return Arrays.asList("tree", "id", "context");
	}
	/**
	 * @param tree C type : const void*<br>
	 * @param id C type : const void*<br>
	 * @param context C type : uint32_t[2]
	 */
	public TSTreeCursor(Pointer tree, Pointer id, int context[]) {
		super();
		this.tree = tree;
		this.id = id;
		if ((context.length != this.context.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.context = context;
	}
	public static class ByReference extends TSTreeCursor implements Structure.ByReference {
		
	};
	public static class ByValue extends TSTreeCursor implements Structure.ByValue {
		
	};
}
