package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class ZonedDateTimeInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        val timeZoneId = CodeBlocks.staticMethodInvocation(
            typeName = ZoneId::class.asTypeName(),
            methodName = "of",
            parameters = listOf(CodeBlock.of("%S", "Europe/Moscow")),
        )
        return CodeBlocks.staticMethodInvocation(
            typeName = ZonedDateTime::class.asTypeName(),
            "of",
            listOf(
                2021, // year
                2,    // month
                3,    // dayOfMonth
                18,   // hour
                50,   // minute
                21,   // second
                0,    // nanoOfSecond
            )
                .map { CodeBlock.of("%L", it) } + timeZoneId
        )
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == ZonedDateTime::class.java.canonicalName
}