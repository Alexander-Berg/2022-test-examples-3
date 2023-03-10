package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.commons.asDeclaredType
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.commons.firstTypeArgument
import ru.beru.android.processor.testinstance.toLiteral
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class Function0InstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        val genericArgumentType = type.asDeclaredType().firstTypeArgument
        return if (genericArgumentType.canonicalNameOrNull == Unit::class.java.canonicalName) {
            "{}"
        } else {
            "{ ${rootAdapter.getInstance(genericArgumentType, context)} }"
        }.toLiteral()
    }

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.canonicalNameOrNull == kotlin.jvm.functions.Function0::class.java.canonicalName
    }
}