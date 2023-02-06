package ru.yandex.direct.common.db

import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class PpcPropertiesMock {
    companion object {
        fun make(props: Map<String, Any>): PpcPropertiesSupport {
            fun <T> propertyMock(value:T?): PpcProperty<*> {
                val mock = Mockito.mock(PpcProperty::class.java)
                Mockito.`when`(mock.get()).thenReturn(value)
                Mockito.`when`(mock.getOrDefault(ArgumentMatchers.any())).thenAnswer { value ?: it.arguments[0] }
                return mock
            }

            val ppcPropertiesSupport = Mockito.mock(PpcPropertiesSupport::class.java)

            val nullProp = propertyMock(null)
            fun anyProp(): PpcPropertyName<*>? = ArgumentMatchers.any()
            Mockito.`when`(ppcPropertiesSupport.get(anyProp(), ArgumentMatchers.any())).thenAnswer {
                val propName = it.arguments[0] as PpcPropertyName<*>
                when {
                    props.containsKey(propName.name) -> propertyMock(props[propName.name]!!)
                    else -> nullProp
                }
            }

            return ppcPropertiesSupport
        }
    }
}
