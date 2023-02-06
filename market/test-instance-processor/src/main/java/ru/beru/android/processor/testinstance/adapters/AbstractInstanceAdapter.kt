package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import javax.lang.model.type.TypeMirror

abstract class AbstractInstanceAdapter(
    protected val rootAdapter: InstanceAdapter
) : InstanceAdapter {

    override fun getInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        require(isTypeSupported(type)) {
            "Тип $type не поддерживается данным провайдером!"
        }
        return constructInstance(type, context)
    }

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return getInstance(type, context)
    }

    protected abstract fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext
    ): CodeBlock
}