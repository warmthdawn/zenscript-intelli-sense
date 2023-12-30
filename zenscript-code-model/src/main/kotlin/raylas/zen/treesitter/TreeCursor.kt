package raylas.zen.treesitter

import raylras.zen.util.Position
import treesitter.TSTreeCursor

class TreeCursor(
    cursor: TSTreeCursor,
    internal var tree: Tree,
): AutoCloseable {
    constructor(node: Node) : this(ts.ts_tree_cursor_new(node.impl), node.tree)

    internal val impl: TSTreeCursor.ByReference = TSTreeCursor.ByReference()
    init {
        impl.tree = cursor.tree
        impl.id = cursor.id
        impl.context = cursor.context
    }

    val node: Node get() = Node(ts.ts_tree_cursor_current_node(impl), tree)

    val fieldId: Short get() = ts.ts_tree_cursor_current_field_id(impl)
    val fieldName: String get() = ts.ts_tree_cursor_current_field_name(impl).getStr()


    fun gotoFirstChild(): Boolean = ts.ts_tree_cursor_goto_first_child(impl).toBool()
    fun gotoLastChild(): Boolean = ts.ts_tree_cursor_goto_last_child(impl).toBool()
    fun gotoParent(): Boolean = ts.ts_tree_cursor_goto_parent(impl).toBool()
    fun gotoNextSibling(): Boolean = ts.ts_tree_cursor_goto_next_sibling(impl).toBool()
    fun gotoPreviousSibling(): Boolean = ts.ts_tree_cursor_goto_previous_sibling(impl).toBool()
    fun gotoFirstChildForByte(index: Int): Long = ts.ts_tree_cursor_goto_first_child_for_byte(impl, index)
    fun gotoFirstChildForPoint(point: Position): Long = ts.ts_tree_cursor_goto_first_child_for_point(impl, point.toTSPoint())

    fun reset(node: Node) {
        ts.ts_tree_cursor_reset(impl, node.impl)
        this.tree = node.tree
    }

    override fun close() {
        ts.ts_tree_cursor_delete(impl)
    }
}