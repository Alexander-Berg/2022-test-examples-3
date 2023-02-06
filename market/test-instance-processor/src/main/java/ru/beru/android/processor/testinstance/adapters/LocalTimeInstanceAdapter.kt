package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import java.time.LocalTime
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class LocalTimeInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.staticMethodInvocation(
            typeName = LocalTime::class.asTypeName(),
            "of",
            listOf(
                13,   // hour
                43,   // minute
                50,   // second
                0,    // nanoOfSecond
            )
                .map { CodeBlock.of("%L", it) }
        )
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == LocalTime::class.java.canonicalName
}