package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import java.util.Date
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class DateInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return CodeBlock.of(
            "%L.time",
            CodeBlocks.newGregorianCalendar(2019, 10, 18, 16, 38, 0)
        )
    }

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == Date::class.java.canonicalName
}