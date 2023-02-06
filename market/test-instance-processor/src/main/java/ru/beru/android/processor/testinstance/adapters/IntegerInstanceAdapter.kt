package ru.beru.android.processor.testinstance.adapters

import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.testinstance.toLiteral
import javax.inject.Inject
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

internal class IntegerInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext
    ) = 42.toLiteral()

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.kind == TypeKind.INT || type.canonicalNameOrNull == Int::class.javaObjectType.canonicalName
    }
}