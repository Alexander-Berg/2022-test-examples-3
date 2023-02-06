package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.commons.asDeclaredType
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.commons.firstTypeArgument
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

internal class ListInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter
) : AbstractInstanceAdapter(rootAdapter) {

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.canonicalNameOrNull == List::class.java.canonicalName || type.canonicalNameOrNull == Collection::class.java.canonicalName &&
                type.asDeclaredType().typeArguments.size == 1
    }

    override fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext
    ): CodeBlock {
        val genericArgumentType = type.asDeclaredType().firstTypeArgument
        return CodeBlocks.listOf(rootAdapter.getInstance(genericArgumentType, context))
    }

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.emptyList
    }
}