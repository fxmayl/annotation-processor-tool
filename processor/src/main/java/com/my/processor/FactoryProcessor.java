package com.my.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.my.processor.Factory"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FactoryProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Messager messager;
    private Filer filer;
    private Elements elementUtils;
    private final Map<String, FactoryGroupedClasses> factoryClasses = new LinkedHashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        typeUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
    }

//    @Override
//    public Set<String> getSupportedAnnotationTypes() {
//        Set<String> annotations = new LinkedHashSet<>();
//        info("CanonicalName is %s", com.my.processor.Factory.class.getCanonicalName());
//        annotations.add(com.my.processor.Factory.class.getCanonicalName());
//        return annotations;
//    }
//
//    @Override
//    public SourceVersion getSupportedSourceVersion() {
//        return SourceVersion.latestSupported();
//    }

    /**
     * Checks if the annotated element observes our rules
     */
    private void checkValidClass(FactoryAnnotatedClass item) throws ProcessingException {

        // Cast to TypeElement, has more type specific methods
        TypeElement classElement = item.getTypeElement();

        if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
            throw new ProcessingException(classElement, "The class %s is not public.",
                    classElement.getQualifiedName().toString());
        }

        // Check if it's an abstract class
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new ProcessingException(classElement,
                    "The class %s is abstract. You can't annotate abstract classes with @%",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
        }

        // Check inheritance: Class must be child class as specified in @com.my.processor.Factory.type();
        TypeElement superClassElement = elementUtils.getTypeElement(item.getQualifiedFactoryGroupName());
        if (superClassElement.getKind() == ElementKind.INTERFACE) {
            // Check interface implemented
            if (!classElement.getInterfaces().contains(superClassElement.asType())) {
                throw new ProcessingException(classElement,
                        "The class %s annotated with @%s must implement the interface %s",
                        classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                        item.getQualifiedFactoryGroupName());
            }
        } else {
            // Check subclassing
            TypeElement currentClass = classElement;
            while (true) {
                /**
                 * getSuperclass()
                 * Returns the direct superclass of this type element.
                 * If this type element represents an interface or the class java.lang.Object,
                 * then a NoType with kind NONE is returned.
                 */
                TypeMirror superClassType = currentClass.getSuperclass();

                if (superClassType.getKind() == TypeKind.NONE) {
                    // Basis class (java.lang.Object) reached, so exit
                    throw new ProcessingException(classElement,
                            "The class %s annotated with @%s must inherit from %s",
                            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                            item.getQualifiedFactoryGroupName());
                }

                if (superClassType.toString().equals(item.getQualifiedFactoryGroupName())) {
                    // Required super class found
                    break;
                }

                // Moving up in inheritance tree
                currentClass = (TypeElement) typeUtils.asElement(superClassType);
            }
        }

        // Check if an empty public constructor is given
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructorElement = (ExecutableElement) enclosed;
                if (constructorElement.getParameters().size() == 0 &&
                        constructorElement.getModifiers().contains(Modifier.PUBLIC)) {
                    // Found an empty constructor
                    return;
                }
            }
        }

        // No empty constructor found
        throw new ProcessingException(classElement,
                "The class %s must provide an public empty default constructor",
                classElement.getQualifiedName().toString());
    }

//    @Override
//    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//        info("test process %s", roundEnv.processingOver());
//        try {
//            // Scan classes
//            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {
//
//                // Check if a class has been annotated with @com.my.processor.Factory
//                if (annotatedElement.getKind() != ElementKind.CLASS) {
//                    throw new ProcessingException(annotatedElement, "Only classes can be annotated with @%s",
//                            Factory.class.getSimpleName());
//                }
//
//                // We can cast it, because we know that it of ElementKind.CLASS
//                TypeElement typeElement = (TypeElement) annotatedElement;
//
//                FactoryAnnotatedClass annotatedClass = new FactoryAnnotatedClass(typeElement);
//
//                checkValidClass(annotatedClass);
//
//                // Everything is fine, so try to add
//                FactoryGroupedClasses factoryClass = factoryClasses.get(annotatedClass.getQualifiedFactoryGroupName());
//                if (factoryClass == null) {
//                    String qualifiedGroupName = annotatedClass.getQualifiedFactoryGroupName();
//                    factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
//                    factoryClasses.put(qualifiedGroupName, factoryClass);
//                }
//
//                // Checks if id is conflicting with another @com.my.processor.Factory annotated class with the same id
//                factoryClass.add(annotatedClass);
//            }
//
//            // Generate code
//            for (FactoryGroupedClasses factoryClass : factoryClasses.values()) {
//                factoryClass.generateCode(elementUtils, filer);
//            }
//            factoryClasses.clear();
//        } catch (ProcessingException e) {
//            error(e.getElement(), e.getMessage());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return true;
//    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {

            // ??????????????????@Factory???????????????????????????
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                error(annotatedElement, "Only classes can be annotated with @%s",
                        Factory.class.getSimpleName());
                // ????????????
                return true;
            }

            // ??????????????????????????????ElementKind.CLASS???????????????????????????????????????
            TypeElement typeElement = (TypeElement) annotatedElement;

            try {
                // throws IllegalArgumentException
                FactoryAnnotatedClass annotatedClass =
                        new FactoryAnnotatedClass(typeElement);

                // ????????????????????????????????????????????????
                if (!isValidClass(annotatedClass)) {
                    return true;
                }

                // ???????????????????????????????????????????????????
                FactoryGroupedClasses factoryClass =
                        factoryClasses.get(annotatedClass.getQualifiedFactoryGroupName());
                if (factoryClass == null) {
                    String qualifiedGroupName = annotatedClass.getQualifiedFactoryGroupName();
                    factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
                    factoryClasses.put(qualifiedGroupName, factoryClass);
                }

                // ??????????????????@Factory???????????????id???????????????
                // ??????IdAlreadyUsedException??????
                factoryClass.add(annotatedClass);

            } catch (IllegalArgumentException e) {
                // @Factory.id()??????
                error(typeElement, e.getMessage());
                return true;
            }
        }

        try {
            for (FactoryGroupedClasses factoryClass : factoryClasses.values()) {
                factoryClass.generateCode(elementUtils, filer);
            }

            // ??????factoryClasses
            factoryClasses.clear();
        } catch (IOException e) {
            error("", e.getMessage());
        }
        return true;
    }


    private boolean isValidClass(FactoryAnnotatedClass item) {

        // ?????????TypeElement, ???????????????????????????
        TypeElement classElement = item.getTypeElement();

        if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
            error(classElement, "The class %s is not public.",
                    classElement.getQualifiedName().toString());
            return false;
        }

        // ??????????????????????????????
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            error(classElement, "The class %s is abstract. You can't annotate abstract classes with @%",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
            return false;
        }

        // ??????????????????: ?????????@Factory.type()?????????????????????
        TypeElement superClassElement =
                elementUtils.getTypeElement(item.getQualifiedFactoryGroupName());
        if (superClassElement.getKind() == ElementKind.INTERFACE) {
            // ???????????????????????????
            if (!classElement.getInterfaces().contains(superClassElement.asType())) {
                error(classElement, "The class %s annotated with @%s must implement the interface %s",
                        classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                        item.getQualifiedFactoryGroupName());
                return false;
            }
        } else {
            // ????????????
            TypeElement currentClass = classElement;
            while (true) {
                TypeMirror superClassType = currentClass.getSuperclass();

                if (superClassType.getKind() == TypeKind.NONE) {
                    // ?????????????????????(java.lang.Object), ????????????
                    error(classElement, "The class %s annotated with @%s must inherit from %s",
                            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                            item.getQualifiedFactoryGroupName());
                    return false;
                }

                if (superClassType.toString().equals(item.getQualifiedFactoryGroupName())) {
                    // ????????????????????????
                    break;
                }

                // ?????????????????????????????????
                currentClass = (TypeElement) typeUtils.asElement(superClassType);
            }
        }

        // ?????????????????????????????????????????????
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructorElement = (ExecutableElement) enclosed;
                if (constructorElement.getParameters().size() == 0 && constructorElement.getModifiers()
                        .contains(Modifier.PUBLIC)) {
                    // ???????????????????????????
                    return true;
                }
            }
        }

        // ??????????????????????????????
        error(classElement, "The class %s must provide an public empty default constructor",
                classElement.getQualifiedName().toString());
        return false;
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    private void error(String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args));
    }

    private void info(String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(msg, args));
    }
}
