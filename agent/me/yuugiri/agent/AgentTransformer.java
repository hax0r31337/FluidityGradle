package me.yuugiri.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentTransformer implements ClassFileTransformer {
    private static final HashMap<String, HashMap<String, String>> obfMap = new HashMap<>();

    public static void agentmain(String string, Instrumentation instrumentation) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(string));
        String string2 = bufferedReader.readLine();
        String curClass = "";
        while (string2 != null) {
            String[] stringArray = string2.split(" ");
            if (stringArray[0].equals("C:")) {
                curClass = stringArray[1];
                obfMap.put(curClass, new HashMap<>());
            } else {
                obfMap.get(curClass).put(stringArray[0], stringArray[1]);
            }
            string2 = bufferedReader.readLine();
        }
        bufferedReader.close();
        instrumentation.addTransformer(new AgentTransformer());
    }

    public byte[] transform(ClassLoader cl, String string, Class<?> clazz, ProtectionDomain protectionDomain, byte[] byArray) {
        ClassNode classNode = this.readClass(byArray);

        readParents(classNode);

        for (int i = 0; i < classNode.methods.size(); ++i) {
            MethodNode methodNode = (MethodNode)classNode.methods.get(i);
            methodNode.name = getMergedMap(cl, classNode.name).getWithFallback(methodNode.name+methodNode.desc, methodNode.name);
            for (int j = 0; j < methodNode.instructions.size(); ++j) {
                AbstractInsnNode ain = methodNode.instructions.get(j);
                if (ain instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) ain;
                    min.name = getMergedMap(cl, min.owner).getWithFallback(min.name+min.desc, min.name);
                } else if (ain instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) ain;
                    fin.name = getMergedMap(cl, fin.owner).getWithFallback(fin.name, fin.name);
                }
            }
        }
        return this.writeClass(classNode);
    }

    private void readParents(ClassNode classNode) {
        ArrayList<String> parent = new ArrayList<>();
        if (classNode.superName != null && !classNode.superName.equals("java/lang/Object")) {
            parent.add(classNode.superName);
        }
        for (Object intf : classNode.interfaces) {
            String str = (String) intf;
            parent.add(str);
        }
        parentMap.put(classNode.name, parent);
    }

    private final HashMap<String, ArrayList<String>> parentMap = new HashMap<>();
    private final HashMap<String, MergedMap> mergedMaps = new HashMap<>();
    private final MergedMap emptyMap = new MergedMap(new ArrayList<>());

    private MergedMap getMergedMap(ClassLoader cl, String name) {
        if (mergedMaps.containsKey(name)) {
            return mergedMaps.get(name);
        }
        List<String> parents = parentTree(cl, name);
        MergedMap mm;
        if (parents.isEmpty()) {
            mm = emptyMap;
        } else {
            List<Map<String, String>> map = new ArrayList<>();
            for (String str : parents) {
                if (obfMap.containsKey(str)) {
                    map.add(obfMap.get(str));
                }
            }
            mm = new MergedMap(map);
        }
        mergedMaps.put(name, mm);
        return mm;
    }

    private List<String> parentTree(ClassLoader cl, String name) {
        ArrayList<String> list = new ArrayList<>();

        list.add(name);
        tryCallClass(cl, name);
        if (!parentMap.containsKey(name)) return list;
        for (String str : parentMap.get(name)) {
//            if (str.equals("java/lang/Object")) continue;
            list.addAll(parentTree(cl, str));
        }

        return list;
    }

    private void tryCallClass(ClassLoader cl, String name) {
        if (parentMap.containsKey(name)) return;
        if (obfMap.containsKey(name)) return;
        try {
            InputStream is = cl.getResourceAsStream(name+".class");
            if (is == null) return;
            byte[] targetArray = new byte[is.available()];
            is.read(targetArray);

            ClassNode cn = readClass(targetArray);
            readParents(cn);
        } catch (Throwable ignored) {}
    }

    private ClassNode readClass(byte[] byArray) {
        ClassReader classReader = new ClassReader(byArray);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    private byte[] writeClass(ClassNode classNode) {
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}