package raylas.zen.treesitter

import com.sun.jna.Memory
import com.sun.jna.Pointer
import raylras.zen.util.Position
import raylras.zen.util.Range
import treesitter.TSLogger
import treesitter.TSPoint
import treesitter.TSRange
import treesitter.TreeSitterLibrary
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


val ts: TreeSitterLibrary = TreeSitterLibrary.INSTANCE


fun Pointer.getStr(): String {
    return getString(0)
}

fun Pointer.releaseStr(): String {
    val str = this.getString(0)
    ts.free_memory(this)
    return str
}

fun Int.toSymbolType(): TSSymbolType {
    return when (this) {
        TreeSitterLibrary.TSSymbolType.TSSymbolTypeRegular -> TSSymbolType.Regular
        TreeSitterLibrary.TSSymbolType.TSSymbolTypeAnonymous -> TSSymbolType.Anonymous
        else -> TSSymbolType.Auxiliary
    }
}

fun Byte.toBool(): Boolean {
    return this != 0.toByte()
}

class LoggerImpl(
    val callback: LogCallback
) : TSLogger.log_callback {
    override fun apply(payload: Pointer?, log_type: Int, buffer: Pointer) {
        val type = if (log_type == TreeSitterLibrary.TSLogType.TSLogTypeLex) {
            TSLogType.Lexer
        } else {
            TSLogType.Parser
        }
        callback(type, buffer.getStr())
    }

}

fun Pair<Int, Int>.toTSRange(): TSRange {
    val tsRange = TSRange()
    tsRange.start_byte = first
    tsRange.end_byte = second
    return tsRange
}

fun TSPoint.toPosition(): Position {
    return Position.of(this.row, this.column)
}

fun Position.toTSPoint(): TSPoint.ByValue {
    val tsPoint = TSPoint.ByValue()
    tsPoint.row = this.line
    tsPoint.column = this.column

    return tsPoint
}

fun TSRange.toRange(): Range {
    return Range(this.start_point.toPosition(), this.end_point.toPosition())
}


fun ByteArray.toCString(): Memory {
    val memory = Memory(size.toLong() + 1)
    memory.write(0, this, 0, this.size)
    memory.setByte(size.toLong(), 0.toByte())
    return memory;
}

fun String.toNativeShared(): SharedString {
    val bytes = this.toByteArray()
    return SharedString(bytes.toCString(), bytes.size)
}

@OptIn(ExperimentalContracts::class)
inline fun <R> String.useInC(block: (Memory, Int) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    var memory: Memory? = null
    var len = 0
    try {
        val bytes = this.toByteArray()
        len = bytes.size + 1
        memory = bytes.toCString()
        return block(memory, len)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        if (exception == null) {
            memory?.close()
        } else {
            try {
                memory?.close()
            } catch (closeException: Throwable) {
                // cause.addSuppressed(closeException) // ignored here
            }
        }
    }
}

class SharedString(
    private val memory: Memory,
    val len: Int,
) {
    val str: Pointer get() = memory
    private val refCount = AtomicInteger()

    fun inc() {
        refCount.incrementAndGet()
    }

    fun release() {
        if (refCount.decrementAndGet() <= 0) {
            memory.close()
        }
    }
}
