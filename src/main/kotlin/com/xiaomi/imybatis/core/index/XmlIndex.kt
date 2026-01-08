package com.xiaomi.imybatis.core.index

import com.intellij.util.indexing.ID

/**
 * Index key for Mapper XML files
 */
object XmlIndex {
    val NAME: ID<String, Void?> = ID.create("imybatis.xml.index")
}
