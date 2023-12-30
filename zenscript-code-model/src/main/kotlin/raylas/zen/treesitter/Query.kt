package raylas.zen.treesitter

import com.sun.jna.Memory
import com.sun.jna.ptr.IntByReference
import treesitter.TSQueryPredicateStep
import treesitter.TreeSitterLibrary.TSQuantifier
import treesitter.TreeSitterLibrary.TSQuery

class Query(internal val ptr: TSQuery, val language: Language): AutoCloseable {

    constructor(language: Language, pattern: String) : this(createObj(language, pattern), language)


    fun startByteForPattern(index: Int) = ts.ts_query_start_byte_for_pattern(ptr, index)

    val patternCount: Int = ts.ts_query_pattern_count(ptr)

    val captureNames: Array<String> by lazy {
        val count = ts.ts_query_capture_count(ptr)
        (0 until count).map {
            val len = IntByReference()
            val name = ts.ts_query_capture_name_for_id(ptr, it, len)

            val byteArray = name.getByteArray(0, len.value)
            String(byteArray)
        }.toTypedArray()
    }

    fun captureQuantifier(patternIndex: Int, captureIndex: Int): CaptureQuantifier {
        val result = ts.ts_query_capture_quantifier_for_id(ptr, patternIndex, captureIndex)
        return CaptureQuantifier.formIndex(result)
    }


    fun predicatesForPattern(patternIndex: Int) {
        val lenRef = IntByReference()
        val first = ts.ts_query_predicates_for_pattern(ptr, patternIndex, lenRef)

        val arr = Array(lenRef.value) { TSQueryPredicateStep() }
        first.toArray(arr)

        arr.map {
            it.type to it.value_id
        }

    }


    fun disableCapture(name: String) = name.useInC { cstr, len ->
        ts.ts_query_disable_capture(ptr, cstr, len)
    }

    fun disablePattern(patternIndex: Int) {
        ts.ts_query_disable_pattern(ptr, patternIndex)
    }

    fun isPatternRooted(patternIndex: Int): Boolean {
        return ts.ts_query_is_pattern_rooted(ptr, patternIndex).toBool()
    }

    fun isPatternNonLocal(patternIndex: Int): Boolean {
        return ts.ts_query_is_pattern_non_local(ptr, patternIndex).toBool()
    }


    fun isPatternGuaranteedAtStep(byteOffset: Int): Boolean {
        return ts.ts_query_is_pattern_guaranteed_at_step(ptr, byteOffset).toBool()
    }





    override fun close() {
        ts.ts_query_delete(ptr)
    }


    companion object {
        private fun createObj(language: Language, pattern: String): TSQuery {
            val errOffset = IntByReference()
            val errType = IntByReference()

            val bytes = pattern.toByteArray()
            val result = Memory(bytes.size.toLong()).use {
                 ts.ts_query_new(language.ptr, it, bytes.size, errOffset, errType)
            }
            return result ?: throw QueryException(errOffset.value, QueryErrorType.formIndex(errType.value))

        }

    }
}

enum class QueryErrorType(val type: Int) {
    None(0),
    Syntax(1),
    NodeType(2),
    Field(3),
    Capture(4),
    Structure(5),
    Language(6);

    companion object {
        private val map = entries.sortedBy { it.type }.toTypedArray()

        fun formIndex(index: Int): QueryErrorType = map[index]
    }
}

enum class CaptureQuantifier(val data: Int) {
    Zero(TSQuantifier.TSQuantifierZero),
    ZeroOrOne(TSQuantifier.TSQuantifierZeroOrOne),
    ZeroOrMore(TSQuantifier.TSQuantifierZeroOrMore),
    One(TSQuantifier.TSQuantifierOne),
    OneOrMore(TSQuantifier.TSQuantifierOneOrMore),

    ;

    companion object {
        private val map = entries.sortedBy { it.data }.toTypedArray()

        fun formIndex(index: Int): CaptureQuantifier = map[index]
    }
}

class QueryException(val offset: Int, val type: QueryErrorType) : RuntimeException()