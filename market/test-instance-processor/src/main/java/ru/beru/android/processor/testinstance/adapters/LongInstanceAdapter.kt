package ru.beru.android.processor.testinstance.adapters

import com.squareup.kotlinpoet.CodeBlock
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.testinstance.TestLong
import ru.beru.android.processor.testinstance.toLongLiteral
import javax.inject.Inject
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

internal class LongInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext): CodeBlock {
        return (context.parameter.getAnnotation(TestLong::class.java)?.value ?: 42L).toLongLiteral()
    }

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.kind == TypeKind.LONG || type.canonicalNameOrNull == Long::class.javaObjectType.canonicalName
    }
}