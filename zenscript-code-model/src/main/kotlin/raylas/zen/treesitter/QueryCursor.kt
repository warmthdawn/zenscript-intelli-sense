package raylas.zen.treesitter

import treesitter.TreeSitterLibrary

class QueryCursor(
    internal val ptr: TreeSitterLibrary.TSQueryCursor
) {

    constructor() : this(ts.ts_query_cursor_new())

}