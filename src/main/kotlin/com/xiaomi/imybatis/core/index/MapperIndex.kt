package com.xiaomi.imybatis.core.index

import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.indexing.FileContent
import java.io.DataOutput
import java.io.DataInput

/**
 * Index key for Mapper interfaces
 */
object MapperIndex {
    val NAME: ID<String, Void?> = ID.create("imybatis.mapper.index")
}

/**
 * Index for Mapper interfaces
 * Maps Mapper interface FQN to its methods and annotations
 */
class MapperIndexExtension : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void?> = MapperIndex.NAME

    override fun getKeyDescriptor(): KeyDescriptor<String> = object : KeyDescriptor<String> {
        override fun save(out: DataOutput, value: String) {
            out.writeUTF(value)
        }

        override fun read(input: DataInput): String {
            return input.readUTF()
        }

        override fun getHashCode(value: String): Int = value.hashCode()

        override fun isEqual(val1: String, val2: String): Boolean = val1 == val2
    }

    override fun getIndexer(): com.intellij.util.indexing.DataIndexer<String, Void?, FileContent> {
        return MapperIndexer()
    }

    override fun getVersion(): Int = 1

    override fun getInputFilter(): com.intellij.util.indexing.FileBasedIndex.InputFilter {
        return com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter(com.intellij.ide.highlighter.JavaFileType.INSTANCE)
    }

    override fun dependsOnFileContent(): Boolean = true
}
