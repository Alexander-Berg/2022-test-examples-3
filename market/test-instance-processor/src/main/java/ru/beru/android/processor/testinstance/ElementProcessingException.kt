package ru.beru.android.processor.testinstance

import javax.lang.model.element.Element

class ElementProcessingException(
    val element: Element,
    message: String? = null,
    cause: Exception? = null
) : Exception(message, cause)