package me.yuugiri.fluiditygradle.remap

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class ClassInheritanceManager() {

    val classes = mutableMapOf<String, InheritanceNode>()
    private val parentCache = mutableMapOf<String, List<String>>()
    private val childCache = mutableMapOf<String, List<String>>()

    private val inheritanceVisitor = InheritanceVisitor {
        classes[it.name] = it
    }

    constructor(jarIn: File) : this() {
        val jis = ZipInputStream(ByteArrayInputStream(jarIn.readBytes()))

        val buffer = ByteArray(1024)
        while (true) {
            val entry = jis.nextEntry ?: break
            if (entry.isDirectory || !entry.name.endsWith(".class")) continue
            val bos = ByteArrayOutputStream()
            var n: Int
            while (jis.read(buffer).also { n = it } != -1) {
                bos.write(buffer, 0, n)
            }
            bos.close()
            addClass(bos.toByteArray())
        }

        jis.close()
    }

    fun addClass(classNode: ClassNode) {
        classes[classNode.name] = InheritanceNode(classNode.name, classNode.access, classNode.superName, classNode.interfaces.toTypedArray())
    }

    fun addClass(byteArray: ByteArray) {
        val cr = ClassReader(byteArray)
        cr.accept(inheritanceVisitor, 0)
    }

    fun searchChildTree(name: String): List<String> {
        val list = mutableListOf<String>()

        getChild(name).forEach {
            list.addAll(searchChildTree(it))
        }

        list.add(name)

        return list
    }

    fun getChild(name: String): List<String> {
        childCache[name]?.also {
            return it
        }
        val list = mutableListOf<String>()

        val klass = classes[name] ?: return list
        if ((klass.access and Opcodes.ACC_INTERFACE) == 0) {
            classes.values.forEach {
                if (it.superName == name) {
                    list.add(it.name)
                }
            }
        } else {
            classes.values.forEach {
                if (it.interfaces.any { it == name }) {
                    list.add(it.name)
                }
            }
        }

        return list.also {
            childCache[name] = it
        }
    }

    fun searchParentTree(owner: String): List<String> {
        val list = mutableListOf<String>()

        getParent(owner).filter { it != "java/lang/Object" }.forEach {
            list.addAll(searchParentTree(it))
        }

        list.add(owner)

        return list
    }

    fun getParent(owner: String): List<String> {
        parentCache[owner]?.also {
            return it
        }
        return getParentFresh(owner).also {
            parentCache[owner] = it
        }
    }

    fun getParentFresh(owner: String): List<String> {
        val list = mutableListOf<String>()
        val klass = classes[owner] ?: return list

        list.addAll(klass.interfaces)
        klass.superName?.let { list.add(it) }

        return list
    }

    fun clearCache() {
        childCache.clear()
        parentCache.clear()
    }

    fun clear() {
        clearCache()
        classes.clear()
    }

    class InheritanceNode(val name: String, val access: Int, val superName: String?, val interfaces: Array<String>)

    class InheritanceVisitor(val inheritanceProvider: (InheritanceNode) -> Unit) : ClassVisitor(Opcodes.ASM6) {

        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<String>?) {
            name ?: return
            inheritanceProvider(InheritanceNode(name, access, superName, interfaces ?: emptyArray()))
            super.visit(version, access, name, signature, superName, interfaces)
        }
    }
}