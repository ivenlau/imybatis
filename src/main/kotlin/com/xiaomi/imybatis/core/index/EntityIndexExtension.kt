package com.xiaomi.imybatis.core.index

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.util.indexing.FileBasedIndex.InputFilter
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.KeyDescriptor
import java.io.DataOutput
import java.io.DataInput

/**
 * Index extension for Entity classes
 */
class EntityIndexExtension : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void?> = EntityIndex.NAME

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
        return EntityIndexer()
    }

    override fun getVersion(): Int = 1

    override fun getInputFilter(): InputFilter {
        return InputFilter { file -> file.fileType == JavaFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true
}
