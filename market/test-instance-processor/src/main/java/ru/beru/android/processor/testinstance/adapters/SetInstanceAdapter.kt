package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.commons.asDeclaredType
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.commons.firstTypeArgument
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class SetInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        val genericArgumentType = type.asDeclaredType().firstTypeArgument
        return CodeBlocks.setOf(
            rootAdapter.getInstance(genericArgumentType, context)
        )
    }

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.emptySet
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == Set::class.java.canonicalName
}