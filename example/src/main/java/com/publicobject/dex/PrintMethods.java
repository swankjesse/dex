package com.publicobject.dex;

import com.android.dex.ClassData;
import com.android.dex.ClassDef;
import com.android.dex.Dex;
import com.android.dex.MethodId;
import com.android.dx.io.CodeReader;
import java.io.File;
import java.io.IOException;

/**
 * Prints calls from declared methods to target methods in a giant tree.
 *
 * <pre>   {@code
 *
 * Lokio/Buffer;.read
 *   Ljava/lang/IllegalArgumentException;.<init>
 *   Ljava/lang/StringBuilder;.<init>
 *   Ljava/lang/StringBuilder;.append
 *   Ljava/lang/StringBuilder;.append
 *   Ljava/lang/StringBuilder;.toString
 *   Ljava/lang/IllegalArgumentException;.<init>
 *   Lokio/Buffer;.write
 *
 * }</pre>
 */
public final class PrintMethods {
  public static void main(String[] args) throws IOException {
    Dex dex = new Dex(new File(args[0]));

    for (MethodId methodId : dex.methodIds()) {
      System.out.println(methodId.toString());
    }

    for (ClassDef classDef : dex.classDefs()) {
      if (classDef.getClassDataOffset() == 0) continue;

      ClassData classData = dex.readClassData(classDef);
      String sourceClassName = dex.typeNames().get(classDef.getTypeIndex());

      for (ClassData.Method method : classData.allMethods()) {
        MethodId sourceMethodId = dex.methodIds().get(method.getMethodIndex());
        String sourceMethodName = dex.strings().get(sourceMethodId.getNameIndex());
        System.out.println(sourceClassName + "." + sourceMethodName
            + "; code size=" + codeSize(dex, method));

        if (method.getCodeOffset() == 0) continue;

        CodeReader codeReader = new CodeReader();
        codeReader.setMethodVisitor((all, one) -> {
          MethodId targetMethodId = dex.methodIds().get(one.getIndex());
          String targetTypeName = dex.typeNames().get(targetMethodId.getDeclaringClassIndex());
          String targetMethodName = dex.strings().get(targetMethodId.getNameIndex());
          System.out.println("  " + targetTypeName + "." + targetMethodName);
        });
        codeReader.visitAll(dex.readCode(method).getInstructions());
      }
    }
  }

  private static int codeSize(Dex dex, ClassData.Method method) {
    int codeOffset = method.getCodeOffset();
    if (codeOffset == 0) return 0;

    Dex.Section section = dex.open(codeOffset);
    section.readCode(); // Read the code so that we can advance its position.
    return section.getPosition() - codeOffset;
  }
}
