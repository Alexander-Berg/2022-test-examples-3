package ru.beru.android.processor.testinstance

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.isCompanionObject
import com.squareup.kotlinpoet.metadata.isObject
import kotlinx.metadata.KmClass
import ru.beru.android.processor.commons.constructors
import ru.beru.android.processor.commons.enclosingElements
import ru.beru.android.processor.commons.getPackage
import ru.beru.android.processor.commons.getTypeElement
import ru.beru.android.processor.commons.isAbstract
import ru.beru.android.processor.commons.isAnnotatedBy
import ru.beru.android.processor.commons.methods
import ru.beru.android.processor.commons.types
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Inject
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter

class SharedProcessorLogic @Inject constructor(
    private val configuration: ProcessorConfiguration,
    override val environment: ProcessingEnvironment,
    private val metadataCache: KotlinMetadataCache,
) : HasProcessingEnvironment {

    private val buffer = StringBuilder()

    fun defaultTestFactoryFor(className: TypeName): CodeBlock {
        return CodeBlock.of("%T.${configuration.handWrittenMethodName}()", className)
    }

    fun generatedTestFactoryFor(
        processedClassElement: Element,
        overrideParameters: Map<CharSequence, CodeBlock> = emptyMap(),
    ): CodeBlock {

        return CodeBlock.of(
            "%M(%L)",
            MemberName(
                processedClassElement.getPackage().qualifiedName.toString(),
                getFactoryMethodName(processedClassElement)
            ),
            overrideParameters.toList().joinToString(separator = ", ") { (name, value) -> "$name = $value" }
        )
    }

    fun getFactoryMethodName(processedClassElement: Element): String {
        buffer.clear()

        processedClassElement.enclosingClassNames
            .mapIndexed { index, s -> if (index == 0) s.replaceFirstChar { it.lowercase() } else s }
            .joinTo(
                buffer = buffer,
                separator = ENCLOSING_SEPARATOR,
                postfix = configuration.methodNameSuffix
            )
        return buffer.toString()
    }

    fun getFactoryFileName(processedClassElement: Element): String {
        buffer.clear()

        processedClassElement.enclosingClassNames
            .joinTo(
                buffer = buffer,
                separator = ENCLOSING_SEPARATOR,
                postfix = configuration.fileNameSuffix
            )
        return buffer.toString()
    }

    fun getFactoryFileJvmName(processedClassElement: Element): String {
        buffer.clear()

        processedClassElement.enclosingClassNames
            .joinTo(
                buffer = buffer,
                separator = ENCLOSING_SEPARATOR,
                postfix = configuration.fileNameJvmSuffix
            )
        return buffer.toString()
    }

    fun isTestFactoryGeneratedByUs(element: Element): Boolean {
        if (element.kind != ElementKind.CLASS) {
            return false
        }
        if (element.isAnnotatedBy<GenerateTestInstance>()) {
            return true
        }
        if (element.constructors.any { it.isAnnotatedBy<GenerateTestInstance>() }) {
            return true
        }
        return element.hasAnnotatedFactoryMethod
    }

    fun findCompanionObject(element: Element): Element? {
        return element.types.firstOrNull { isCompanionObject(it) }
    }

    private val Element.hasAnnotatedFactoryMethod: Boolean
        get() {
            if (methods.any { it.isAnnotatedBy<GenerateTestInstance>() }) {
                return true
            }
            return findCompanionObject(this)?.methods.orEmpty().any { it.isAnnotatedBy<GenerateTestInstance>() }
        }

    private val Element.enclosingClassNames: Sequence<String>
        get() {
            return (listOf(this) + enclosingElements)
                .asReversed()
                .asSequence()
                .map { it.asType() }
                .filter { it.kind == TypeKind.DECLARED }
                .map { it.getTypeElement().simpleName.toString() }
        }

    fun containsSuitableDefaultFactoryMethod(element: TypeElement): Boolean {
        return ElementFilter.methodsIn(element.enclosedElements)
            .asSequence()
            .filter {
                it.parameters.isEmpty() &&
                        it.modifiers.containsAll(setOf(Modifier.PUBLIC, Modifier.STATIC)) &&
                        it.returnType == element.asType() &&
                        it.simpleName.contentEquals(configuration.handWrittenMethodName)
            }
            .firstOrNull() != null
    }

    fun isSealedClass(classElement: Element): Boolean {
        if (classElement !is TypeElement || !classElement.isAbstract) {
            return false
        }
        return getKmClass(classElement)?.isSealed == true
    }

    fun isObject(classElement: Element): Boolean {
        if (classElement !is TypeElement) {
            return false
        }
        return getKmClass(classElement)?.isObject == true
    }

    fun isCompanionObject(classElement: Element): Boolean {
        if (classElement !is TypeElement) {
            return false
        }
        return getKmClass(classElement)?.isCompanionObject == true
    }

    private fun getKmClass(classElement: Element): KmClass? {
        val classMetadata = classElement.getAnnotation(Metadata::class.java) ?: return null
        return metadataCache.toImmutableKmClass(classMetadata)
    }

    fun isSealedClassProcessable(classElement: TypeElement): Boolean {
        require(isSealedClass(classElement)) {
            "Класс $classElement не является sealed классом!"
        }
        return findSuitableSealedClassImplementation(classElement) != null
    }

    private fun findSuitableSealedClassImplementation(classElement: TypeElement): TypeElement? {
        return classElement.findSealedClassImplementations()
            .firstOrNull { isTestFactoryGeneratedByUs(it) || containsSuitableDefaultFactoryMethod(it) }
    }

    fun generateFactoryCallForSealedClass(classElement: TypeElement): CodeBlock {
        require(isSealedClass(classElement)) {
            "Класс $classElement не является sealed классом!"
        }
        val suitableImplementation = findSuitableSealedClassImplementation(classElement)
        check(suitableImplementation != null) {
            "Не удалось найти подходящую реализацию класса ${classElement.simpleName}!"
        }
        return if (isTestFactoryGeneratedByUs(suitableImplementation)) {
            generatedTestFactoryFor(suitableImplementation, emptyMap())
        } else {
            defaultTestFactoryFor(suitableImplementation.asClassName())
        }
    }

    fun generateFactoryCallForObject(classElement: TypeElement): CodeBlock {
        require(isObject(classElement)) {
            "Класс $classElement не является object'ом!"
        }
        return CodeBlock.of("%T", classElement.asClassName())
    }

    companion object {
        private const val ENCLOSING_SEPARATOR = "_"
    }
}