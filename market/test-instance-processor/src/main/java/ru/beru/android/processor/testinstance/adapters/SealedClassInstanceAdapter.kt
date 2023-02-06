package ru.beru.android.processor.testinstance.adapters

import ru.beru.android.processor.commons.getTypeElement
import ru.beru.android.processor.commons.getTypeElementOrNull
import ru.beru.android.processor.testinstance.ParameterProcessingContext
import ru.beru.android.processor.testinstance.SharedProcessorLogic
import javax.inject.Inject
import javax.lang.model.type.TypeMirror

class SealedClassInstanceAdapter @Inject constructor(
    rootAdapter: InstanceAdapter,
    private val processorLogic: SharedProcessorLogic
) : AbstractInstanceAdapter(rootAdapter) {

    override fun constructInstance(
        type: TypeMirror,
        context: ParameterProcessingContext
    ) = processorLogic.generateFactoryCallForSealedClass(type.getTypeElement())

    override fun isTypeSupported(type: TypeMirror): Boolean {
        val classElement = type.getTypeElementOrNull()
        return classElement != null &&
                processorLogic.isSealedClass(classElement) &&
                processorLogic.isSealedClassProcessable(classElement)
    }
}