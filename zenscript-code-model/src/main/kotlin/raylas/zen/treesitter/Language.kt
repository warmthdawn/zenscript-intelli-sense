package raylas.zen.treesitter

import treesitter.TreeSitterLibrary
import treesitter.TreeSitterLibrary.TSLanguage


enum class TSSymbolType(value: Int) {
    Regular(TreeSitterLibrary.TSSymbolType.TSSymbolTypeRegular),
    Anonymous(TreeSitterLibrary.TSSymbolType.TSSymbolTypeAnonymous),
    Auxiliary(TreeSitterLibrary.TSSymbolType.TSSymbolTypeAuxiliary),
}

typealias LogCallback = (TSLogType, String) -> Unit

class Language(
    internal val ptr: TSLanguage,
) {

    val version: Int by lazy {
        ts.ts_language_version(this.ptr)
    }

    val fieldCount: Int by lazy {
        ts.ts_language_field_count(this.ptr)
    }

    fun fieldName(fieldId: Short): String? = ts.ts_language_field_name_for_id(this.ptr, fieldId)?.getStr()

    fun fieldId(name: String): Short = name.useInC { str, len ->
        ts.ts_language_field_id_for_name(this.ptr, str, len)
    }

    fun symbolType(nodeId: Short): TSSymbolType = ts.ts_language_symbol_type(this.ptr, nodeId).toSymbolType()
    fun symbolName(nodeId: Short) = ts.ts_language_symbol_name(this.ptr, nodeId).getStr()
    fun symbolId(name: String, isNamed: Boolean = true) = name.useInC { str, len ->
        ts.ts_language_symbol_for_name(this.ptr, str, len, if (isNamed) 1 else 0)
    }


    companion object {
        val zenScript = Language(ts.tree_sitter_zenscript())
    }
}