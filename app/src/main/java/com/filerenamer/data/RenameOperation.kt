package com.filerenamer.data

enum class RenameType {
    ADD_PREFIX,
    ADD_SUFFIX,
    REMOVE_FIRST_N,
    REMOVE_LAST_N,
    REPLACE_FIRST_N,
    REPLACE_LAST_N,
    INSERT_AT_N_FROM_START,
    DELETE_AT_N_FROM_START,
    INSERT_AT_N_FROM_END,
    DELETE_AT_N_FROM_END
}

data class RenameOperation(
    val type: RenameType,
    val text: String = "",       // 添加前缀/后缀时的文本，或替换时的替换文本，或插入时的插入文本
    val charCount: Int = 0,      // 删除前N/后N个字符时的数量，或替换时的替换长度，或删除时的删除长度
    val position: Int = 0        // 从前往后/从后往前的位置（第n个位置）
)
