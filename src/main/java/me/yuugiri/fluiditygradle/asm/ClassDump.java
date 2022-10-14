package me.yuugiri.fluiditygradle.asm;

import org.objectweb.asm.*;

public class ClassDump implements Opcodes {

    public static byte[] dump(final String targetMain, final String targetJarPath, final String remapperFilePath) {
        ClassWriter cw = new ClassWriter(0);
//        FieldVisitor fv;
        MethodVisitor mv;
//        AnnotationVisitor av0;

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "me/yuugiri/agent/AgentLoader", null, "java/lang/Object", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "me/yuugiri/agent/AgentLoader", "attach", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, targetMain, "main", "([Ljava/lang/String;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, "attach", "()V", null, null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/management/ManagementFactory", "getRuntimeMXBean", "()Ljava/lang/management/RuntimeMXBean;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/management/RuntimeMXBean", "getName", "()Ljava/lang/String;", true);
            mv.visitVarInsn(ASTORE, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitIntInsn(BIPUSH, 64);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "indexOf", "(I)I", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "com/sun/tools/attach/VirtualMachine", "attach", "(Ljava/lang/String;)Lcom/sun/tools/attach/VirtualMachine;", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn(targetJarPath);
            mv.visitLdcInsn(remapperFilePath);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/tools/attach/VirtualMachine", "loadAgent", "(Ljava/lang/String;Ljava/lang/String;)V", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/tools/attach/VirtualMachine", "detach", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
