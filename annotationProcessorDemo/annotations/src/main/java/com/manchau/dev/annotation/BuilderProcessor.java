package com.manchau.dev.annotation;
/*
 * Created by maneesh.chauhan on 31/01/2019
 */

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;


/**
 * Our implementation of the annotation processor for the custom annotation Builder. This extends
 * the AbstractProcessor class and implements the abstract method process to add the features of the
 * builder pattern to the annotated classes
 */

public class BuilderProcessor extends AbstractProcessor {

	private Filer filer;
	private Messager messager;


	/**
	 * The annotation processor needs a public no arg constructor. We may have others as well,
	 * however this is required.
	 */
	public BuilderProcessor() {
		super();
	}

	/**
	 * The init method helps in collecting the information about the processing environment,
	 * elements that allow us to interact with the build system. E.g messager which we use to send
	 * info to the user and the Filer object which we use to write new files. This method is only
	 * called once.
	 */

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.filer = processingEnv.getFiler();
		this.messager = processingEnv.getMessager();
	}

	/**
	 * This returns the supported suource version by the annotation processor. We are targeting the
	 * latest source version
	 */
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	/**
	 * This returns the set of annotations that we want to be handed over by the annotation
	 * processing tool. This announces only the annotations in which we are interested in, so that
	 * the annotation processing tool can apply this processor to the source files annotated with
	 * those annotations.
	 */
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(Builder.class
				.getCanonicalName())));
	}


	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (Element type : roundEnv.getElementsAnnotatedWith(Builder.class)) {

			// get element metadata
			String packageName = getPackageName(type);
			String targetName = lowerCamelCase(type.getSimpleName().toString());
			Set<Element> vars = getPrivateVariables(type);

			String builderName = String.format("%sBuilder", type.getSimpleName());
			ClassName builderType = ClassName.get(packageName, builderName);

			// create private fields and public setters
			List<FieldSpec> fields = new ArrayList<>(vars.size());
			List<MethodSpec> setters = new ArrayList<>(vars.size());
			for (Element var : vars) {
				TypeName typeName = TypeName.get(var.asType());
				String name = var.getSimpleName().toString();

				// create the field
				fields.add(FieldSpec.builder(typeName, name, PRIVATE).build());

				// create the setter
				setters.add(MethodSpec.methodBuilder(name)
						.addModifiers(PUBLIC)
						.returns(builderType)
						.addParameter(typeName, name)
						.addStatement("this.$N = $N", name, name)
						.addStatement("return this")
						.build());
			}

			// create the build method
			TypeName targetType = TypeName.get(type.asType());
			MethodSpec.Builder buildMethodBuilder =
					MethodSpec.methodBuilder("build")
							.addModifiers(PUBLIC)
							.returns(targetType)
							.addStatement("$1T $2N = new $1T()", targetType, targetName);

			for (FieldSpec field : fields) {
				String name = upperCamelCase(field.name);
				buildMethodBuilder
						.addStatement("$1N.set$2N(this.$3N)", targetName, name, field);
			}

			buildMethodBuilder.addStatement("return $N", targetName);
			MethodSpec buildMethod = buildMethodBuilder.build();

			// create the builder
			TypeSpec builder = TypeSpec.classBuilder(builderType)
					.addModifiers(PUBLIC, FINAL)
					.addFields(fields)
					.addMethods(setters)
					.addMethod(buildMethod)
					.build();

			// write java source file
			JavaFile file = JavaFile
					.builder(builderType.packageName(), builder)
					.build();
			try {
				file.writeTo(filer);
			} catch (IOException e) {
				messager.printMessage(Diagnostic.Kind.ERROR,
						"Failed to write file for element", type);
			}

		}
		return true;
	}

	private Set<Element> getPrivateVariables(Element type) {

		return type.getEnclosedElements()
				.stream()
				.filter(e -> e.getModifiers().contains(PRIVATE))
				.filter(e -> e.getKind().isField())
				.collect(Collectors.toSet());
	}

	private String lowerCamelCase(String s) {
		char[] chars = s.toCharArray();
		chars[0] = Character.toLowerCase(chars[0]);
		return String.valueOf(chars);
	}

	private String upperCamelCase(String s) {
		char[] chars = s.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return String.valueOf(chars);
	}

	private String getPackageName(Element type) {
		PackageElement pkg = processingEnv.getElementUtils().getPackageOf(type);
		return pkg.getQualifiedName().toString();
	}
}
