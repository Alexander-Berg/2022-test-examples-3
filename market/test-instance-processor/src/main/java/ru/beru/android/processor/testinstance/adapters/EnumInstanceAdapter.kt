package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.HasProcessingEnvironment
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.testinstance.ProcessorConfiguration
import ru.beru.android.processor.testinstance.SharedProcessorLogic
import ru.beru.android.processor.testinstance.asClassName
import ru.beru.android.processor.commons.asDeclaredTypeOrNull
import ru.beru.android.processor.commons.getTypeElement
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Inject
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

class EnumInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
    override val environment: ProcessingEnvironment,
    private val processorLogic: SharedProcessorLogic,
    private val configuration: ProcessorConfiguration
) : AbstractInstanceAdapter(rootAdapter), HasProcessingEnvironment {

    override fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext
    ): CodeBlock {
        val enumElement = type.getTypeElement()
        if (enumElement.containsSuitableFactoryMethod) {
            return processorLogic.defaultTestFactoryFor(enumElement.asType().asClassName())
        }
        val enumConstants = enumElement.enumConstants
        return if (enumConstants.isNotEmpty()) {
            CodeBlocks.enumLiteral(enumElement.asType().asTypeName(), enumConstants.first().simpleName)
        } else {
            throw IllegalArgumentException("Данный enum пустой, непонятно какое значение для него следует взять!")
        }
    }

    override fun isTypeSupported(type: TypeMirror) = type.asDeclaredTypeOrNull()?.isEnum == true

    private val TypeElement.containsSuitableFactoryMethod: Boolean
        get() {
            return enclosedElements.asSequence()
                .filter { it.kind == ElementKind.METHOD }
                .map { it as ExecutableElement }
                .filter {
                    it.parameters.isEmpty() &&
                            it.modifiers.containsAll(setOf(Modifier.PUBLIC, Modifier.STATIC)) &&
                            it.returnType == this.asType() &&
                            it.simpleName.contentEquals(configuration.handWrittenMethodName)
                }
                .firstOrNull() != null
        }
}