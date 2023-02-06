package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.commons.findAnnotation
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.testinstance.TestFloat
import ru.beru.android.processor.testinstance.toFloatLiteral
import javax.inject.Inject
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class FloatInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return (context.parameter.findAnnotation<TestFloat>()?.value ?: 42.0f).toFloatLiteral()
    }

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.kind == TypeKind.FLOAT || type.canonicalNameOrNull == Float::class.javaObjectType.canonicalName
    }
}