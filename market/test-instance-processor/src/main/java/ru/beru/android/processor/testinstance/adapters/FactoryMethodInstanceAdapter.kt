package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.commons.asDeclaredType
import ru.beru.android.processor.commons.asTypeVariable
import ru.beru.android.processor.commons.getTypeElement
import ru.beru.android.processor.commons.getTypeElementOrNull
import ru.beru.android.processor.commons.isTypeVariable
import ru.beru.android.processor.commons.methods
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.HasProcessingEnvironment
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.testinstance.SharedProcessorLogic
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Inject
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

class FactoryMethodInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
    private val processorLogic: SharedProcessorLogic,
    override val environment: ProcessingEnvironment,
) : AbstractInstanceAdapter(rootAdapter), HasProcessingEnvironment {

    override fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext,
    ): CodeBlock {
        val declaredType = type.asDeclaredType()
        val factoryMethod = processorLogic.findCompanionObject(type.getTypeElement())
            ?.methods
            .orEmpty()
            .first { it.isSuitableFactoryMethod(targetType = type.asDeclaredType()) }
        val typeVars = (factoryMethod.returnType as DeclaredType).typeArguments
            .mapIndexed { index, argument ->
                val typeVarName = argument.asTypeVariable().toString()
                val realType = declaredType.typeArguments[index]
                typeVarName to realType
            }
            .associate { it }
        val parameterValues = factoryMethod.parameters
            .map { rootAdapter.getInstance(it.asType(), context.copy(typeVariablesMap = typeVars)) }
        return CodeBlocks.staticMethodInvocation(
            staticMethod = factoryMethod,
            parameters = parameterValues,
        )
    }

    override fun isTypeSupported(type: TypeMirror): Boolean {
        val typeElement = type.getTypeElementOrNull() ?: return false
        val factoryMethod = processorLogic.findCompanionObject(typeElement)
            ?.methods
            .orEmpty()
            .firstOrNull { it.isSuitableFactoryMethod(targetType = type.asDeclaredType()) }
        return factoryMethod != null
    }

    private fun ExecutableElement.isSuitableFactoryMethod(targetType: DeclaredType): Boolean {
        if (targetType.erased isNotAssignableTo returnType.erased) {
            return false
        }
        return returnType.asDeclaredType()
            .typeArguments
            .all { it.isTypeVariable }
    }
}