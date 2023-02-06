package ru.beru.android.processor.testinstance

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

data class ParameterProcessingContext(
    val processingClassType: TypeMirror,
    val constructor: ExecutableElement,
    val parameter: VariableElement,
    val recursiveParameters: List<VariableElement>,
    val typeVariablesMap: Map<String, TypeMirror>,
)

