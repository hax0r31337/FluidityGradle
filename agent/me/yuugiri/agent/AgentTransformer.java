package me.yuugiri.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.BufferedReader;
import java.io.FileReader;
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

    public byte[] transform(ClassLoader classLoader, String string, Class<?> clazz, ProtectionDomain protectionDomain, byte[] byArray) {
        ClassNode classNode = this.readClass(byArray);

        {
            ArrayList<String> parent = new ArrayList<>();
            if (classNode.superName != null && !classNode.superName.equals("java/lang/Object")) {
                tryCallClass(classNode.superName);
                parent.add(classNode.superName);
            }
            for (Object intf : classNode.interfaces) {
                String str = (String) intf;
                tryCallClass(str);
                parent.add(str);
            }
            parentMap.put(classNode.name, parent);
        }

        for (int i = 0; i < classNode.methods.size(); ++i) {
            MethodNode methodNode = (MethodNode)classNode.methods.get(i);
            methodNode.name = getMergedMap(classNode.name).getWithFallback(methodNode.name+methodNode.desc, methodNode.name);
            for (int j = 0; j < methodNode.instructions.size(); ++j) {
                AbstractInsnNode ain = methodNode.instructions.get(j);
                if (ain instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) ain;
                    min.name = getMergedMap(min.owner).getWithFallback(min.name+min.desc, min.name);
                } else if (ain instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) ain;
                    fin.name = getMergedMap(fin.owner).getWithFallback(fin.name, fin.name);
                }
            }
        }
        return this.writeClass(classNode);
    }

    private final HashMap<String, ArrayList<String>> parentMap = new HashMap<>();
    private final HashMap<String, MergedMap> mergedMaps = new HashMap<>();
    private final MergedMap emptyMap = new MergedMap(new ArrayList<>());

    private MergedMap getMergedMap(String name) {
        if (mergedMaps.containsKey(name)) {
            return mergedMaps.get(name);
        }
        List<String> parents = parentTree(name);
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

    private List<String> parentTree(String name) {
        ArrayList<String> list = new ArrayList<>();

        list.add(name);
        if (!parentMap.containsKey(name)) return list;
        for (String str : parentMap.get(name)) {
//            if (str.equals("java/lang/Object")) continue;
            list.addAll(parentTree(str));
        }

        return list;
    }

    private void tryCallClass(String name) {
        if (parentMap.containsKey(name)) return;
//        try {
//            System.out.println(name);
//            Class.forName(name.replace('/', '.'));
//        } catch (Throwable t) {}
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