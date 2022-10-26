package me.yuugiri.fluiditygradle.asm;

import org.objectweb.asm.*;

public class ClassDump implements Opcodes {

    public static byte[] dump(final String targetMain, final String targetJarPath, final String remapperFilePath) {
        final ClassWriter cw = new ClassWriter(0);

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "me/yuugiri/agent/AgentLoader", null, "java/lang/Object", null);
        {
            final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/management/ManagementFactory", "getRuntimeMXBean", "()Ljava/lang/management/RuntimeMXBean;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/management/RuntimeMXBean", "getName", "()Ljava/lang/String;", true);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitIntInsn(BIPUSH, 64);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "indexOf", "(I)I", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/sun/tools/attach/VirtualMachine", "attach", "(Ljava/lang/String;)Lcom/sun/tools/attach/VirtualMachine;", false);
            mv.visitInsn(DUP);
            mv.visitLdcInsn(targetJarPath);
            mv.visitLdcInsn(remapperFilePath);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/tools/attach/VirtualMachine", "loadAgent", "(Ljava/lang/String;Ljava/lang/String;)V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/tools/attach/VirtualMachine", "detach", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, targetMain, "main", "([Ljava/lang/String;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
