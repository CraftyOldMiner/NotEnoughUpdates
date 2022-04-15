package io.github.moulberry.notenoughupdates.testutil;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ClassTweaker {

	private final ClassPool classPool = ClassPool.getDefault();
	private final CtClass theClass;

	public ClassTweaker(String classFilePath) throws IOException {
		File inputClassFile = new File(classFilePath);
		if (!inputClassFile.exists()) {
			throw new FileNotFoundException(classFilePath + " does not exist");
		}
		this.theClass = classPool.makeClass(Files.newInputStream(inputClassFile.toPath()));
	}

	private ClassTweaker(CtClass theClass) {
		this.theClass = theClass;
	}

	public void writeToFile(String outputFilePath) throws IOException {
		File outputFile = new File(outputFilePath);
		File parentDir = new File(outputFile.getParent());
		if (!parentDir.exists() && !parentDir.mkdirs()) {
			throw new IOException("Could not create directory: " + parentDir.getPath());
		}

		ClassFile classFile = theClass.getClassFile();
		classFile.write(new DataOutputStream(new FileOutputStream(outputFilePath, false)));
	}

	public void newMethodFromFile(String filePath) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(filePath));
		String sourceCode = new String(encoded);
		try {
			CtMethod newMethod = CtNewMethod.make(sourceCode, theClass);
			theClass.addMethod(newMethod);
		} catch (CannotCompileException e) {
			throw new RuntimeException(e);
		}
	}

	public ClassTweaker getChildClassTweaker(String className) {
		try {
			CtClass[] nestedClasses = theClass.getNestedClasses();
			for (CtClass nestedClass : nestedClasses) {
				if (nestedClass.getSimpleName().equals(className) || nestedClass.getName().equals(className)) {
					return new ClassTweaker(nestedClass);
				}
			}

			throw new NotFoundException("Child class could not be found: " + className);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateMethodBody(String methodName, String descriptor, String newBody) {
		try {
			CtMethod theMethod = theClass.getMethod(methodName, descriptor);
			theMethod.setBody(newBody);
		} catch (NotFoundException | CannotCompileException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateConstructorBody(String newBody) {
		try {
			CtConstructor[] constructors = theClass.getDeclaredConstructors();
			for (CtConstructor constructor : constructors) {
				constructor.setBody(newBody);
			}
		} catch (CannotCompileException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateFieldInitializer(String fieldName, String newBody) {
		try {
			CtConstructor classInit = theClass.getClassInitializer();
			CtField originalField = theClass.getField(fieldName);
			originalField.setName(fieldName + "Old");
			theClass.removeField(originalField);
//			CtField newField = new CtField(CtClass.booleanType, fieldName, theClass);
//			theClass.addField(newField, newBody);
//		} catch (CannotCompileException e) {
//			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void makeNestedConstuctorsPublic() {
		try {
			CtClass[] nestedClasses =  theClass.getNestedClasses();
			for (CtClass nestedClass : nestedClasses) {
				for (CtConstructor constructor : nestedClass.getDeclaredConstructors()) {
					final int modifiers = constructor.getModifiers();
					if (!Modifier.isPublic(modifiers)) {
						constructor.setModifiers(Modifier.setPublic(modifiers));
					}
				}
				nestedClass.toClass();
			}
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		} catch (CannotCompileException e) {
			throw new RuntimeException(e);
		}
	}

	public MethodTweaker createMethodTweaker(String methodName, String methodDescriptor) {
		return new MethodTweaker(methodName, methodDescriptor);
	}

	public class MethodTweaker {
		private final String methodName;
		private final ArrayList<MethodTweak> methodTweaks = new ArrayList<>();
		private final MethodInfo theMethodInfo;

		// methodName can be:
		//  - A normal method name
		//  - "<clinit>" to get the class initializer
		//  - The short class name to get the constructor with the provided method descriptor
		private MethodTweaker(String methodName, String methodDescriptor) {
			this.methodName = methodName;
			try {
				if (methodName.equals("<clinit>")) {
					CtConstructor initializer = ClassTweaker.this.theClass.getClassInitializer();
					theMethodInfo = initializer.getMethodInfo();
				} else if (ClassTweaker.this.theClass.getSimpleName().equals(methodName)) {
					CtConstructor initializer = ClassTweaker.this.theClass.getConstructor(methodDescriptor);
					theMethodInfo = initializer.getMethodInfo();
				}
				else {
					CtMethod theMethod = ClassTweaker.this.theClass.getMethod(methodName, methodDescriptor);
					theMethodInfo = theMethod.getMethodInfo();
				}
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		public void addTweak(int tweakOffset, int[] newOpcodes) {
			if (tweakOffset + newOpcodes.length > theMethodInfo.getCodeAttribute().getCodeLength()) {
				throw new IllegalArgumentException("Tweak is outside of method bounds. Method name: " + methodName);
			}
			methodTweaks.add(new MethodTweak(tweakOffset, newOpcodes));
		}

		public void applyTweaks() {
			CodeAttribute codeAttribute = theMethodInfo.getCodeAttribute();
			CodeIterator codeIterator = codeAttribute.iterator();
			for (MethodTweak methodTweak : methodTweaks) {
				for (int i = 0; i < methodTweak.opCodes.length; i++) {
					codeIterator.writeByte((byte) methodTweak.opCodes[i], methodTweak.offset + i);
				}
			}
			try {
				theMethodInfo.rebuildStackMap(classPool);
			} catch (BadBytecode e) {
				throw new RuntimeException(e);
			}
		}

		public void addNopTweak(int tweakOffset, int nopCount) {
			int[] newOpCodes = new int[nopCount];
			for (int i=0; i < nopCount; i++) {
				newOpCodes[i] = Opcode.NOP;
			}
			addTweak(tweakOffset, newOpCodes);
		}
	}

	private static class MethodTweak {
		private final int offset;
		private final int[] opCodes;

		private MethodTweak(int offset, int[] opCodes) {
			this.offset = offset;
			this.opCodes = opCodes;
		}

	}


}
