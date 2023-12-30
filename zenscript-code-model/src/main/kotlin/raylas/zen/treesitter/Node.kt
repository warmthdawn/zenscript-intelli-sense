package raylas.zen.treesitter

import raylras.zen.util.Position
import raylras.zen.util.Range
import treesitter.TSNode

class Node(
    internal val impl: TSNode.ByValue,
    internal val tree: Tree
) {

    val language: Language get() = tree.language

    val symbolId: Short get() = ts.ts_node_symbol(impl)

    val symbolName: String get() = language.symbolName(symbolId)


    val isNull: Boolean get() = impl.id == null
    val isNamed: Boolean get() = ts.ts_node_is_named(impl).toBool()
    val isExtra: Boolean get()  = ts.ts_node_is_extra(impl).toBool()
    val isError: Boolean  get()  = ts.ts_node_is_error(impl).toBool()
    val hasChanges: Boolean  get() = ts.ts_node_has_changes(impl).toBool()
    val hasError: Boolean  get() = ts.ts_node_has_error(impl).toBool()
    val isMissing: Boolean  get() = ts.ts_node_is_missing(impl).toBool()

    val startByte: Int get() = impl.context[0]
    val startPos: Position get() = Position.of(impl.context[1], impl.context[2])
    val endByte: Int get() = ts.ts_node_end_byte(impl)
    val endPos: Position get() = ts.ts_node_end_point(impl).toPosition()
    val range: Range get() = Range(startPos, endPos)


    fun child(index: Int): Node {
        val childImpl = ts.ts_node_child(impl, index)
        return Node(childImpl, tree)
    }

    val childCount: Int get() = ts.ts_node_child_count(impl)

    fun namedChild(index: Int): Node {
        val childImpl = ts.ts_node_named_child(impl, index)
        return Node(childImpl, tree)
    }

    val namedChildCount: Int get() = ts.ts_node_named_child_count(impl)


    override fun toString(): String {
        return ts.ts_node_string(impl).releaseStr()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is Node) {
            return false
        }
        return impl.id == other.impl.id && tree == other.tree
    }

    override fun hashCode(): Int {
        var result = impl.id.hashCode()
        result = 31 * result + tree.hashCode()
        return result
    }




}