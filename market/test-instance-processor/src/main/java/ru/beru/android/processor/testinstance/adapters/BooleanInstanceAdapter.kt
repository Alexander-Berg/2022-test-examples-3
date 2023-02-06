package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.commons.findAnnotation
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.testinstance.TestBoolean
import ru.beru.android.processor.testinstance.toLiteral
import javax.inject.Inject
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class BooleanInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return (context.parameter.findAnnotation<TestBoolean>()?.value ?: true).toLiteral()
    }

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.kind == TypeKind.BOOLEAN || type.canonicalNameOrNull == Boolean::class.javaObjectType.canonicalName
    }
}