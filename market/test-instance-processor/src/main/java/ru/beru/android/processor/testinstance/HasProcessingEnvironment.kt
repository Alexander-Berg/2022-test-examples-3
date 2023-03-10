package ru.beru.android.processor.testinstance

import ru.beru.android.processor.commons.getEnumConstants
import ru.beru.android.processor.commons.isEnum
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

interface HasProcessingEnvironment {

    val environment: ProcessingEnvironment

    val TypeMirror.isEnum get() = environment.isEnum(this)

    val TypeElement.enumConstants get() = environment.getEnumConstants(this)

    val sourceVersion: SourceVersion get() = environment.sourceVersion

    val elements: Elements get() = environment.elementUtils

    val types: Types get() = environment.typeUtils

    infix fun TypeMirror.isSameTypeAs(another: TypeMirror) = types.isSameType(this, another)

    infix fun TypeMirror.isAssignableTo(another: TypeMirror) = types.isAssignable(this, another)

    infix fun TypeMirror.isNotAssignableTo(another: TypeMirror) = !types.isAssignable(this, another)

    fun TypeElement.findSealedClassImplementations(): List<TypeElement> {
        val thisTypeMirror = asType()
        val innerImplementations = ElementFilter.typesIn(this.enclosedElements)
            .filter { it.superclass.isSameTypeAs(thisTypeMirror) }
        val outerImplementation = this.enclosingElement.enclosedElements
            .filter { it != this }
            .let { ElementFilter.typesIn(it) }
            .filter { it.superclass.isSameTypeAs(thisTypeMirror) }
        return innerImplementations + outerImplementation
    }

    val TypeMirror.erased: TypeMirror get() = types.erasure(this)
}