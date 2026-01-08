package com.xiaomi.imybatis.core.index

import com.intellij.util.indexing.ID

/**
 * Index key for Entity classes
 */
object EntityIndex {
    val NAME: ID<String, Void?> = ID.create("imybatis.entity.index")
}
