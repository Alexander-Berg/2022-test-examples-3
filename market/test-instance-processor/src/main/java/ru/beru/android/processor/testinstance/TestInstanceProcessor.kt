package ru.beru.android.processor.testinstance

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.jvm.jvmName
import com.squareup.kotlinpoet.jvm.jvmOverloads
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import ru.beru.android.processor.commons.asDeclaredType
import ru.beru.android.processor.commons.asTypeElement
import ru.beru.android.processor.commons.containsType
import ru.beru.android.processor.commons.isAbstract
import ru.beru.android.processor.commons.isGeneric
import ru.beru.android.processor.commons.isNullable
import ru.beru.android.processor.commons.isPrivate
import ru.beru.android.processor.commons.rawType
import ru.beru.android.processor.testinstance.adapters.InstanceAdapter
import ru.beru.android.processor.testinstance.adapters.InstanceAdaptersInitializer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
@AutoService(Processor::class)
class TestInstanceProcessor : AbstractProcessor(), HasProcessingEnvironment {

    private val dateTimeFormatter = SimpleDateFormat(DATE_TIME_FORMAT_ISO_8601, Locale.ENGLISH)

    @Inject
    internal lateinit var sharedLogic: SharedProcessorLogic

    @Inject
    internal lateinit var configuration: ProcessorConfiguration

    @Inject
    internal lateinit var adaptersInitializer: InstanceAdaptersInitializer

    @Inject
    internal lateinit var instanceAdapter: InstanceAdapter

    @Inject
    internal lateinit var processingEnvironment: ProcessingEnvironment

    override fun getSupportedOptions(): MutableSet<String> {
        return mutableSetOf(IncrementalAnnotationProcessorType.ISOLATING.processorOption)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes() = mutableSetOf(ANNOTATION_GENERATE.java.name)

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        /*
         * DaggerProcessorComponent красный из-за бага в IDE - https://youtrack.jetbrains.com/issue/IDEA-160956.
         * Это не мешает процессору нормально компилироваться и работать.
         */
        DaggerProcessorComponent.builder()
            .processingEnvironment(processingEnv)
            .build()
            .injectMembers(this)

        adaptersInitializer.setupProviders()
    }

    override val environment: ProcessingEnvironment get() = processingEnvironment

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment?,
    ): Boolean {
        val isClaimed = annotations?.containsType(ANNOTATION_GENERATE) != null
        return if (isClaimed) {
            roundEnv?.getElementsAnnotatedWith(ANNOTATION_GENERATE.java)
                .orEmpty()
                .onEach { checkIsApplicable(it) }
                .also { annotatedElements ->
                    val processingClasses = annotatedElements.filter { it.kind == ElementKind.CLASS }
                        .associateWith { it.properties }
                    val processingFactories =
                        ElementFilter.constructorsIn(annotatedElements).associateWith { it.properties } +
                                processingClasses.mapNotNull { (element, properties) ->
                                    try {
                                        findAppropriateConstructor(element) to properties
                                    } catch (e: Exception) {
                                        reportError(e, element)
                                        null
                                    }
                                }.toMap() +
                                ElementFilter.methodsIn(annotatedElements).associateWith { it.properties }
                    processingFactories.forEach { (factory, properties) ->
                        try {
                            val targetElement = factory.targetElement
                            val processingClassPackage =
                                processingEnv.elementUtils.getPackageOf(targetElement).toString()
                            val classType = targetElement.asType()
                            val context = ClassProcessingContext(
                                factory = factory,
                                properties = properties,
                                processingClassElement = targetElement,
                                processingClassType = classType,
                                processingClassPackage = processingClassPackage,
                                processingClassTypeName = classType.asTypeName()
                            )
                            when {
                                sharedLogic.isSealedClass(targetElement) -> processSealedClass(context)
                                sharedLogic.isObject(targetElement) -> processObject(context)
                                else -> process(context)
                            }
                        } catch (e: Exception) {
                            reportError(e, factory)
                        }
                    }
                }
            true
        } else {
            false
        }
    }

    private val ExecutableElement.targetElement: Element
        get() = if (kind == ElementKind.CONSTRUCTOR) enclosingElement else returnType.asDeclaredType().asElement()

    private val Element.properties: AnnotationProperties
        get() {
            val annotation = getAnnotation(GenerateTestInstance::class.java)
            return if (annotation != null) {
                AnnotationProperties(annotation.jvmOverloads)
            } else {
                AnnotationProperties()
            }
        }

    private fun checkIsApplicable(element: Element) {
        when (element.kind) {
            ElementKind.CLASS -> checkIsApplicableToClass(element)
            ElementKind.CONSTRUCTOR -> checkIsApplicableToConstructor(element)
            ElementKind.METHOD -> checkIsApplicableToMethod(element)
            else -> reportError(
                "Аннотацию $ANNOTATION_GENERATE можно применять только к конструкторам или классам!",
                element
            )
        }
    }

    private fun checkIsApplicableToConstructor(constructor: Element) {
        if (constructor.isPrivate) {
            reportError("Не получится создать тестовую фабрику для приватного конструктора.", constructor)
        }
        checkIsApplicableToClass(constructor.enclosingElement)
    }

    private fun checkIsApplicableToMethod(method: Element) {
        if (method.isPrivate) {
            reportError("Не получится создать тестовую фабрику для приватного метода.", method)
        }
    }

    private fun checkIsApplicableToClass(clazz: Element) {
        if (clazz.isPrivate) {
            reportError("Не получится создать тестовую фабрику для приватного класса.", clazz)
        }
    }

    private fun findAppropriateConstructor(clazz: Element): ExecutableElement {
        val constructors = clazz.enclosedElements
            .filter { it.kind == ElementKind.CONSTRUCTOR }
            .map { it as ExecutableElement }
        return when (constructors.size) {
            0 -> throw IllegalArgumentException("У класса нет конструкторов.")
            1 -> constructors.first()
            else -> {
                constructors.maxByOrNull { it.parameters.size }!!
            }
        }
    }

    private fun process(context: ClassProcessingContext) {
        writeFactoryFile(context) {
            val factory = context.factory
            val processingClassType = context.processingClassType
            val properties = context.properties

            val recursiveConstructorParameters = factory.parameters
                .filter { it.asType().containsType(processingClassType) }
            val parameterSpecs = factory.parameters
                .map {
                    val defaultValueBlock = instanceAdapter.getInstance(
                        it.asType(),
                        ParameterProcessingContext(
                            processingClassType = processingClassType,
                            constructor = factory,
                            parameter = it,
                            recursiveParameters = recursiveConstructorParameters,
                            typeVariablesMap = emptyMap(),
                        )
                    )
                    val name = it.simpleName.toString()
                    val type = it.asType().asTypeName().toKotlinType().copy(nullable = it.isNullable)
                    ParameterSpec.builder(name, type)
                        .defaultValue(defaultValueBlock)
                        .build()
                }

            addFunction(generateFactoryMethod(context, parameterSpecs))
                .apply {
                    if (properties.jvmOverloadsMode == JvmOverloadsMode.NoArgOnly) {
                        addFunction(generateNoArgFactoryMethod(context, parameterSpecs))
                    }
                }
        }
    }

    private fun generateFactoryMethod(
        context: ClassProcessingContext,
        parameterSpecs: List<ParameterSpec>,
    ): FunSpec {
        val classTypeName = context.processingClassTypeName
        val properties = context.properties

        val parameters = parameterSpecs.map { it.name.toLiteral() }
        return getFactoryMethodBuilder(context)
            .addParameters(parameterSpecs)
            .apply {
                if (properties.jvmOverloadsMode == JvmOverloadsMode.Full) {
                    jvmOverloads()
                }
            }
            .returns(classTypeName)
            .addCode(
                CodeBlocks.returnStatement(
                    if (context.factory.kind == ElementKind.CONSTRUCTOR) {
                        CodeBlocks.constructorInvocation(
                            typeName = classTypeName,
                            parameters = parameters,
                        )
                    } else {
                        CodeBlocks.staticMethodInvocation(
                            staticMethod = context.factory,
                            parameters = parameters,
                        )
                    }
                )
            )
            .build()
    }

    private fun getFactoryMethodBuilder(context: ClassProcessingContext): FunSpec.Builder {
        val classElement = context.processingClassElement
        val classTypeName = context.processingClassTypeName

        return FunSpec.builder(sharedLogic.getFactoryMethodName(classElement))
            .addOriginatingElement(classElement)
            .addAnnotation(CodeBlocks.restrictToTestsAnnotation)
            .addAnnotation(CodeBlocks.jvmNameAnnotation(configuration.jvmFactoryMethodName))
            .returns(classTypeName)
    }

    private fun generateNoArgFactoryMethod(
        context: ClassProcessingContext,
        parameterSpecs: List<ParameterSpec>,
    ): FunSpec {
        val classTypeName = context.processingClassTypeName

        return getFactoryMethodBuilder(context)
            .addCode(
                CodeBlocks.returnStatement(
                    CodeBlocks.constructorInvocation(
                        classTypeName,
                        parameterSpecs.mapNotNull { it.defaultValue?.toLiteral() }
                    )
                )
            )
            .build()
    }

    private fun processSealedClass(context: ClassProcessingContext) {
        writeFactoryFile(context) {
            addFunction(generateSealedClassFactoryMethod(context))
        }
    }

    private fun processObject(context: ClassProcessingContext) {
        writeFactoryFile(context) {
            addFunction(generateObjectFactoryMethod(context))
        }
    }

    private fun generateSealedClassFactoryMethod(context: ClassProcessingContext): FunSpec {
        return getFactoryMethodBuilder(context)
            .addCode(
                CodeBlocks.returnStatement(
                    sharedLogic.generateFactoryCallForSealedClass(context.processingClassElement.asTypeElement())
                )
            )
            .build()
    }

    private fun generateObjectFactoryMethod(context: ClassProcessingContext): FunSpec {
        return getFactoryMethodBuilder(context)
            .addCode(
                CodeBlocks.returnStatement(
                    sharedLogic.generateFactoryCallForObject(context.processingClassElement.asTypeElement())
                )
            )
            .build()
    }

    private fun writeFactoryFile(
        context: ClassProcessingContext,
        generateMethod: FileSpec.Builder.() -> FileSpec.Builder,
    ) {
        val processingClassElement = context.processingClassElement
        val processingClassPackage = context.processingClassPackage

        val outputFileName = sharedLogic.getFactoryFileName(processingClassElement)
        val outputFileJvmName = sharedLogic.getFactoryFileJvmName(processingClassElement)
        val factoryFile = FileSpec.builder(processingClassPackage, outputFileName)
            .jvmName(outputFileJvmName)
            .apply {
                val generatedAnnotation = generatedAnnotation(useSiteTarget = AnnotationSpec.UseSiteTarget.FILE)
                if (generatedAnnotation != null) {
                    addAnnotation(generatedAnnotation)
                } else {
                    addComment("Generated by ${TestInstanceProcessor::class.qualifiedName}")
                }
            }
            .run(generateMethod)
            .build()
        factoryFile.writeTo(processingEnv.filer)
    }

    private fun TypeMirror.containsType(type: TypeMirror): Boolean {
        return when (this.kind) {
            TypeKind.WILDCARD -> (this as WildcardType).rawType.containsType(type)
            TypeKind.DECLARED -> {
                val declaredType = asDeclaredType()
                if (declaredType.isGeneric) {
                    declaredType.typeArguments.any { it.containsType(type) }
                } else {
                    isSameTypeAs(type)
                }
            }
            else -> isSameTypeAs(type)
        }
    }

    private fun generatedAnnotation(
        generationDateTime: Date = Date(),
        useSiteTarget: AnnotationSpec.UseSiteTarget? = null,
    ): AnnotationSpec? {

        return elements.getTypeElement(
            if (sourceVersion > SourceVersion.RELEASE_8) {
                GENERATED_ANNOTATION_NAME_JVM8
            } else {
                GENERATED_ANNOTATION_NAME
            }
        )
            ?.let {
                AnnotationSpec.builder(it.asTypeElement().asClassName())
                    .addMember("value = [%S]", TestInstanceProcessor::class.qualifiedName ?: "")
                    .addMember("date = %S", dateTimeFormatter.format(generationDateTime))
                    .useSiteTarget(useSiteTarget)
                    .build()
            }
    }

    private fun reportError(
        errorMessage: CharSequence,
        element: Element?,
    ) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element)
    }

    private fun reportError(
        exception: Throwable,
        element: Element?,
    ) {
        val el = if (exception is ElementProcessingException) exception.element else element
        processingEnv.messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Во время обработки элемента произошла ошибка ${exception.stackTraceToString()}",
            el
        )
    }

    data class AnnotationProperties(val jvmOverloadsMode: JvmOverloadsMode = JvmOverloadsMode.None)

    private data class ClassProcessingContext(
        val processingClassElement: Element,
        val processingClassType: TypeMirror,
        val processingClassPackage: String,
        val processingClassTypeName: TypeName,
        val factory: ExecutableElement,
        val properties: AnnotationProperties,
    )

    companion object {
        private val ANNOTATION_GENERATE = GenerateTestInstance::class
        private const val GENERATED_ANNOTATION_NAME = "javax.annotation.Generated"
        private const val GENERATED_ANNOTATION_NAME_JVM8 = "javax.annotation.processing.Generated"
        private const val DATE_TIME_FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssZ"
    }
}