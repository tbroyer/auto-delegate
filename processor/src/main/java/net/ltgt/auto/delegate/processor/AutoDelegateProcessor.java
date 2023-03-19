/*
 * Copyright © 2023 Thomas Broyer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ltgt.auto.delegate.processor;

import static com.squareup.javapoet.CodeBlock.joining;
import static java.util.stream.Collectors.toList;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Javac annotation processor (compiler plugin) for the delegation pattern; user code never
 * references this class.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes(AutoDelegateProcessor.AUTO_DELEGATE_TYPE_NAME)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
public class AutoDelegateProcessor extends AbstractProcessor {
  static final String AUTO_DELEGATE_TYPE_NAME = "net.ltgt.auto.delegate.AutoDelegate";

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  private final List<String> deferredTypeNames = new ArrayList<>();

  private TypeElement annotationType;
  private TypeElement javaLangObject;
  private AnnotationSpec generatedAnnotation;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    annotationType = processingEnv.getElementUtils().getTypeElement(AUTO_DELEGATE_TYPE_NAME);
    javaLangObject = processingEnv.getElementUtils().getTypeElement(Object.class.getName());
    generatedAnnotation =
        AnnotationSpec.builder(
                processingEnv.getSourceVersion().compareTo(SourceVersion.RELEASE_8) > 0
                    ? ClassName.get("javax.annotation.processing", "Generated")
                    : ClassName.get("javax.annotation", "Generated"))
            .addMember("value", "$S", getClass().getName())
            .build();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (annotationType == null) {
      // This should not happen. If the annotation type is not found, how did the processor get
      // triggered?
      processingEnv
          .getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              "Did not process @"
                  + AUTO_DELEGATE_TYPE_NAME
                  + " because the annotation class was not found");
      return false;
    }
    List<TypeElement> deferredTypes =
        deferredTypeNames.stream()
            .map(name -> processingEnv.getElementUtils().getTypeElement(name))
            .collect(toList());
    if (roundEnv.processingOver()) {
      // This means that the previous round didn't generate any new sources, so we can't have found
      // any new instances of @AutoDelegate; and we can't have any new types that are the reason a
      // type was in deferredTypes.
      for (TypeElement type : deferredTypes) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "[AutoDelegateUndefined] Did not generate @AutoDelegate class for "
                    + type.getQualifiedName()
                    + " because it references undefined types",
                type);
      }
      return false;
    }
    Collection<? extends Element> annotatedElements =
        roundEnv.getElementsAnnotatedWith(annotationType);
    List<TypeElement> types = new ArrayList<>();
    types.addAll(deferredTypes);
    types.addAll(ElementFilter.typesIn(annotatedElements));
    deferredTypeNames.clear();
    for (TypeElement type : types) {
      AutoDelegateInfo info = validateType(type);
      if (info == null) {
        continue;
      }
      try {
        if (!processType(info)) {
          addDeferredType(type);
        }
      } catch (RuntimeException e) {
        String trace;
        try (StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw)) {
          e.printStackTrace(pw);
          pw.flush();
          trace = sw.toString();
        } catch (IOException ex) {
          // should never happen
          trace = e.toString();
        }
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                String.format(
                    "[AutoDelegateException] @AutoDelegate processor threw an exception: %s",
                    trace),
                type);
        throw e;
      }
    }
    return false; // never claim annotation, because who knows what other processors want?
  }

  private void addDeferredType(TypeElement type) {
    // We save the name of the type rather
    // than its TypeElement because it is not guaranteed that it will be represented by
    // the same TypeElement on the next round.
    deferredTypeNames.add(type.getQualifiedName().toString());
  }

  private @Nullable AutoDelegateInfo validateType(TypeElement type) {
    if (type.getKind() != ElementKind.CLASS) {
      processingEnv
          .getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              "[AutoDelegateWrongType] @AutoDelegate only applies to classes",
              type);
      return null;
    }
    if (!checkModifiersIfNested(type)) {
      return null;
    }
    ClassName targetName = autoDelegateName(type);
    if (type.getSuperclass().getKind() != TypeKind.ERROR
        && !((ErrorType) type.getSuperclass())
            .asElement()
            .getSimpleName()
            .contentEquals(targetName.simpleName())) {
      processingEnv
          .getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              "[AutoDelegateSuperClass] @AutoDelegate super class must be the to-be-generated class",
              type);
      return null;
    }

    List<TypeElement> interfaces = new ArrayList<>();
    TypeElement extend = javaLangObject;
    AnnotationMirror annotation =
        type.getAnnotationMirrors().stream()
            .filter(a -> a.getAnnotationType().asElement().equals(annotationType))
            .findFirst()
            .orElseThrow(AssertionError::new);
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        annotation.getElementValues().entrySet()) {
      switch (entry.getKey().getSimpleName().toString()) {
        case "value":
          @SuppressWarnings("unchecked")
          List<? extends AnnotationValue> values =
              (List<? extends AnnotationValue>) entry.getValue().getValue();
          for (AnnotationValue value : values) {
            if (!(value.getValue() instanceof DeclaredType)
                || ((DeclaredType) value.getValue()).getKind() != TypeKind.DECLARED) {
              addDeferredType(type);
              return null;
            }
            TypeElement element = (TypeElement) ((DeclaredType) value.getValue()).asElement();
            if (element.getKind() != ElementKind.INTERFACE) {
              processingEnv
                  .getMessager()
                  .printMessage(
                      Diagnostic.Kind.ERROR,
                      "[AutoDelegateInterface] @AutoDelegate class can only delegate to interfaces",
                      type,
                      annotation,
                      value);
              return null;
            }
            // TODO: check modifiers if nested
            interfaces.add(element);
          }
          break;
        case "extend":
          if (!(entry.getValue().getValue() instanceof DeclaredType)
              || ((DeclaredType) entry.getValue().getValue()).getKind() != TypeKind.DECLARED) {
            addDeferredType(type);
            return null;
          }
          extend = (TypeElement) ((DeclaredType) entry.getValue().getValue()).asElement();
          if (extend.getKind() != ElementKind.CLASS) {
            processingEnv
                .getMessager()
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    "[AutoDelegateExtend] @AutoDelegate super class must be a class",
                    type,
                    annotation,
                    entry.getValue());
            return null;
          }
          // TODO: check modifiers if nested
          break;
      }
    }

    return new AutoDelegateInfo(targetName, interfaces, extend);
  }

  private static ClassName autoDelegateName(TypeElement type) {
    ClassName name = ClassName.get(type);
    return name.topLevelClassName()
        .peerClass("AutoDelegate_" + String.join("_", name.simpleNames()));
  }

  private boolean checkModifiersIfNested(TypeElement type) {
    @SuppressWarnings("NullAway") // false positive
    ElementKind enclosingKind = type.getEnclosingElement().getKind();
    if (enclosingKind.isClass() || enclosingKind.isInterface()) {
      if (type.getModifiers().contains(Modifier.PRIVATE)) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "[AutoDelegatePrivate] @AutoDelegate class must not be private",
                type);
        return false;
      }
      // TODO: handle some enclosing element being private
      //      else {
      //        // The previous case, where the class itself is private, is much commoner so it
      // deserves
      //        // its own error message, even though it would be caught by the test here too.
      //        processingEnv
      //            .getMessager()
      //            .printMessage(
      //                Diagnostic.Kind.ERROR,
      //                "[AutoDelegateInPrivate] @AutoDelegate class must not be nested in a private
      // class",
      //                type);
      //        return false;
      //      }
      if (!type.getModifiers().contains(Modifier.STATIC)) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "[AutoDelegateInner] Nested @AutoDelegate class must be static",
                type);
        return false;
      }
    }
    // In principle type.getEnclosingElement() could be an ExecutableElement (for a class
    // declared inside a method), but since RoundEnvironment.getElementsAnnotatedWith doesn't
    // return such classes we won't see them here.
    return true;
  }

  private boolean processType(AutoDelegateInfo info) {
    NameAllocator nameAllocator = new NameAllocator();
    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(info.targetName)
            .addAnnotation(generatedAnnotation)
            .addModifiers(Modifier.ABSTRACT)
            // TODO: handle generics
            .superclass(info.extend.asType());
    List<ParameterSpec> ctorParameters = new ArrayList<>();
    CodeBlock.Builder ctorFieldInitBuilder = CodeBlock.builder();
    for (TypeElement i : info.interfaces) {
      // TODO: handle generics
      TypeName ti = ClassName.get(i);
      classBuilder.addSuperinterface(ti);
      String name =
          nameAllocator.newName(
              "__" + i.getSimpleName() + "_unlikelyToConflictWithExistingMember", i);
      classBuilder.addField(ti, name, Modifier.PRIVATE, Modifier.FINAL);
      ctorParameters.add(ParameterSpec.builder(ti, name).build());
      ctorFieldInitBuilder.addStatement("this.$1N = $1N", name);
    }
    CodeBlock ctorFieldInit = ctorFieldInitBuilder.build();
    for (ExecutableElement ctor : ElementFilter.constructorsIn(info.extend.getEnclosedElements())) {
      // TODO: filter out non-visible constructors
      classBuilder.addMethod(
          MethodSpec.constructorBuilder()
              // TODO: handle generics
              .addParameters(ctorParameters)
              .addParameters(
                  ctor.getParameters().stream().map(ParameterSpec::get).collect(toList()))
              .varargs(ctor.isVarArgs())
              .addStatement(
                  "super($L)",
                  ctor.getParameters().stream()
                      .map(p -> CodeBlock.of("$N", p.getSimpleName()))
                      .collect(joining(",")))
              .addCode(ctorFieldInit)
              .build());
    }
    for (TypeElement i : info.interfaces) {
      for (ExecutableElement m :
          ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(i))) {
        if (m.getModifiers().contains(Modifier.STATIC)
            || m.getModifiers().contains(Modifier.PRIVATE)
            || javaLangObject.equals(m.getEnclosingElement())) {
          continue;
        }
        // TODO: handle methods contributed from more than one interface
        classBuilder.addMethod(
            MethodSpec.overriding(m)
                .addStatement(
                    "$Lthis.$N.$N($L)",
                    m.getReturnType().getKind() == TypeKind.VOID ? "" : "return ",
                    nameAllocator.get(i),
                    m.getSimpleName(),
                    m.getParameters().stream()
                        .map(p -> CodeBlock.of("$N", p.getSimpleName()))
                        .collect(joining(",")))
                .build());
      }
    }
    try {
      JavaFile.builder(info.targetName.packageName(), classBuilder.build())
          .build()
          .writeTo(processingEnv.getFiler());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return true;
  }

  static class AutoDelegateInfo {
    final ClassName targetName;
    final List<? extends TypeElement> interfaces;
    final TypeElement extend;

    AutoDelegateInfo(
        ClassName targetName, List<? extends TypeElement> interfaces, TypeElement extend) {
      this.targetName = targetName;
      this.interfaces = interfaces;
      this.extend = extend;
    }
  }
}
