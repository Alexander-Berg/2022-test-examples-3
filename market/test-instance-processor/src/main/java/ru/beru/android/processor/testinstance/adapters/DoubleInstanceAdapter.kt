package ru.beru.android.processor.testinstance.adapters

import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.commons.canonicalNameOrNull
import ru.beru.android.processor.testinstance.toLiteral
import javax.inject.Inject
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class DoubleInstanceAdapter @Inject constructor(rootAdapter: InstanceAdapter) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext) = 0.5.toLiteral()

    override fun isTypeSupported(type: TypeMirror): Boolean {
        return type.kind == TypeKind.DOUBLE || type.canonicalNameOrNull == Double::class.javaObjectType.canonicalName
    }
}