package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import ru.beru.android.processor.commons.asDeclaredTypeOrNull
import ru.beru.android.processor.commons.asTypeVariableOrNull
import ru.beru.android.processor.commons.isAnnotatedBy
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.testinstance.SharedProcessorLogic
import ru.beru.android.processor.testinstance.TestRecursiveWarning
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

internal class RootInstanceAdapter @Inject constructor(
    private val adaptersRegistry: InstanceAdaptersRegistry,
    private val processorLogic: SharedProcessorLogic,
) : InstanceAdapter {

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return adaptersRegistry.getProvider(type)?.getRecursiveInstance(type, context) ?: getForUnknown(type)
    }

    override fun getInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        val typeVariable = type.asTypeVariableOrNull()
        val targetType = if (typeVariable != null) {
            context.typeVariablesMap[typeVariable.toString()] ?: type
        } else {
            type
        }
        return adaptersRegistry.getProvider(targetType)?.let {
            if (context.parameter.isAnnotatedBy<TestRecursiveWarning>()) {
                it.getRecursiveInstance(targetType, context)
            } else {
                it.getInstance(targetType, context)
            }
        } ?: getForUnknown(targetType)
    }

    private fun getForUnknown(type: TypeMirror): CodeBlock {
        val declaredType = type.asDeclaredTypeOrNull()
        return if (declaredType != null) {
            processorLogic.defaultTestFactoryFor(declaredType.asTypeName())
        } else {
            throw IllegalStateException("Не удалось обработать тип \"$type\"!")
        }
    }

    override fun isTypeSupported(type: TypeMirror) = true
}