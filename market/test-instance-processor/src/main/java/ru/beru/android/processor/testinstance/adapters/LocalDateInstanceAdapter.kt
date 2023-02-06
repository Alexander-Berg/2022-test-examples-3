package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import java.time.LocalDate
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class LocalDateInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlocks.staticMethodInvocation(
            typeName = LocalDate::class.asTypeName(),
            "of",
            listOf(
                2021, // year
                4,    // month
                6,    // dayOfMonth
            )
                .map { CodeBlock.of("%L", it) }
        )
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == LocalDate::class.java.canonicalName
}