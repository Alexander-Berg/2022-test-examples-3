package ru.beru.android.processor.testinstance.adapters

import ru.beru.android.processor.testinstance.CodeBlocks
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.commons.canonicalNameOrNull
import java.math.BigDecimal
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class BigDecimalInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(type: TypeMirror, context: ParameterProcessingContext) = CodeBlocks.bigDecimalTen

    override fun isTypeSupported(type: TypeMirror) = type.canonicalNameOrNull == BigDecimal::class.java.canonicalName
}