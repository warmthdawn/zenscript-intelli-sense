package raylas.zen.treesitter

import com.sun.jna.ptr.IntByReference
import raylras.zen.util.Position
import raylras.zen.util.Range
import treesitter.TSInputEdit
import treesitter.TSRange
import treesitter.TreeSitterLibrary

class Tree(
    internal val ptr: TreeSitterLibrary.TSTree,
    private val textMemory: SharedString,
    val language: Language
) : AutoCloseable, Cloneable {
    init {
        textMemory.inc()
    }
    override fun close() {
        ts.ts_tree_delete(this.ptr)
        textMemory.release()
    }

    val rootNode : Node get() {
        val impl = ts.ts_tree_root_node(ptr)
        return Node(impl, this)
    }

    fun rootNodeWithOffset(bytes: Int, position: Position) {
        val impl = ts.ts_tree_root_node_with_offset(ptr, bytes, position.toTSPoint())
        Node(impl, this)
    }

    fun edit(edit: InputEdit) {
        ts.ts_tree_edit(ptr, edit.toTSEdit())
    }

    fun walk(): TreeCursor {
        return TreeCursor(this.rootNode)
    }

    fun changedRanges(other: Tree): Array<Range> {
        val len = IntByReference()
        val rangesPtr = ts.ts_tree_get_changed_ranges(this.ptr, other.ptr, len)
        val tsRanges = Array(len.value) { TSRange() }
        rangesPtr.toArray(tsRanges)
        val result = tsRanges.map {
            it.toRange()
        }.toTypedArray()

        ts.free_memory(rangesPtr.pointer)
        return result
    }

    fun includedRanges(): Array<Range> {
        val len = IntByReference()
        val rangesPtr = ts.ts_tree_included_ranges(this.ptr, len)
        val tsRanges = Array(len.value) { TSRange() }
        rangesPtr.toArray(tsRanges)
        val result = tsRanges.map {
            it.toRange()
        }.toTypedArray()

        ts.free_memory(rangesPtr.pointer)
        return result
    }

    override fun clone(): Tree {
        val clone = ts.ts_tree_copy(ptr)
        return Tree(clone, textMemory, language)
    }
}

data class InputEdit(
    val start_byte: Int,
    val old_end_byte: Int,
    val new_end_byte: Int,

    val start_point: Position,
    val old_end_point: Position,
    val new_end_point: Position,
) {

    fun toTSEdit(): TSInputEdit {
        val edit = TSInputEdit()
        edit.start_byte = start_byte
        edit.old_end_byte = old_end_byte
        edit.new_end_byte = new_end_byte

        edit.start_point = start_point.toTSPoint()
        edit.old_end_point = old_end_point.toTSPoint()
        edit.new_end_point = new_end_point.toTSPoint()

        return edit
    }
}