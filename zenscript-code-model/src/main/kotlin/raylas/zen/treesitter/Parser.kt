package raylas.zen.treesitter

import treesitter.NativeSize
import treesitter.NativeSizeByReference
import treesitter.TSLogger
import treesitter.TreeSitterLibrary
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.DurationUnit

enum class TSLogType {
    Parser,
    Lexer,
}

class Parser constructor(
    internal val ptr: TreeSitterLibrary.TSParser,
    private var _language: Language
) : AutoCloseable {

    private var cancellationToken: NativeSizeByReference? = null

        private set

    constructor(language: Language, cancellable: Boolean = false) : this(ts.ts_parser_new(), language) {
        if (cancellable) {
            cancellationToken = NativeSizeByReference()
            ts.ts_parser_set_cancellation_flag(this.ptr, cancellationToken)
        }
        ts.ts_parser_set_language(this.ptr, language.ptr)
    }

    fun setLanguage(language: Language): Boolean = ts.ts_parser_set_language(this.ptr, language.ptr) != 0.toByte()
    val language get() = _language
    fun setLogger(callback: LogCallback?) {
        if (callback == null) {
            ts.ts_parser_set_logger(this.ptr, null)
            return
        }
        val impl = LoggerImpl(callback)
        val log = TSLogger.ByValue()
        log.payload = null
        log.log = impl
        ts.ts_parser_set_logger(this.ptr, log)
    }

    fun setIncludedRanges(vararg ranges: Pair<Int, Int>) {
        val data = ranges.map { it.toTSRange() }.toTypedArray()
        ts.ts_parser_set_included_ranges(this.ptr, data, data.size)
    }

    fun reset() {
        ts.ts_parser_reset(this.ptr)
    }

    var timeout: Duration
        get() {
            val timeMarco = ts.ts_parser_timeout_micros(this.ptr)
            return timeMarco.microseconds
        }
        set(value) {
            val timeoutMarcos = value.toLong(DurationUnit.MICROSECONDS)
            ts.ts_parser_set_timeout_micros(this.ptr, timeoutMarcos)
        }

    var cancelled: Boolean
        get() {
            return (cancellationToken ?: return false)
                .value.toLong() != 0L
        }
        set(value) {
            cancellationToken ?: return
            cancellationToken!!.value = NativeSize(if (value) 1 else 0)
        }

    override fun close() {
        ts.ts_parser_delete(this.ptr)
        cancellationToken = null
    }

    fun parse(text: String, oldTree: Tree?): Tree {
        val nativeString = text.toNativeShared()
        val tsTree = ts.ts_parser_parse_string(this.ptr, oldTree?.ptr, nativeString.str, nativeString.len)
        return Tree(tsTree, nativeString, this.language)
    }


}

