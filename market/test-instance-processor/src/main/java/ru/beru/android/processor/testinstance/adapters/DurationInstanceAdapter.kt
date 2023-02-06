package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import java.time.Duration
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class DurationInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.staticMethodInvocation(
            typeName = Duration::class.asTypeName(),
            "parse",
            listOf(CodeBlock.of("%S", "PT15M")),
        )
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == Duration::class.java.canonicalName
}