package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.commons.asDeclaredType
import ru.beru.android.processor.commons.canonicalNameOrNull
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class MapInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        val (firstArgumentType, secondArgumentType) = type.asDeclaredType().typeArguments
        return CodeBlocks.mapOf(
            rootAdapter.getInstance(firstArgumentType, context),
            rootAdapter.getInstance(secondArgumentType, context)
        )
    }

    override fun getRecursiveInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.emptyMap
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == Map::class.java.canonicalName
}