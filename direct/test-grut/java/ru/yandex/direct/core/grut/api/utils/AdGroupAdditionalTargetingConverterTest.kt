package ru.yandex.direct.core.grut.api.utils

import org.assertj.core.api.Assertions
import org.junit.Test
import org.reflections.Reflections
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.grut.objects.proto.AdGroupAdditionalTargeting.TAdGroupAdditionalTargeting

class AdGroupAdditionalTargetingConverterTest {

    @Test
    fun checkAllAdditionalTargetingSubtypesAreHandled() {
        val reflections = Reflections("ru.yandex.direct.core.entity")
        val subtypes = reflections.getSubTypesOf(AdGroupAdditionalTargeting::class.java)

        for (clazz in subtypes) {
            val instance = clazz.declaredConstructors.first().newInstance()
            Assertions.assertThatCode {
                try {
                    AdGroupAdditionalTargetingConverter.fillValueField(TAdGroupAdditionalTargeting.newBuilder(), instance as AdGroupAdditionalTargeting, emptyMap())
                } catch (ex: NullPointerException) {
                    //NPE is fine
                }
            }.doesNotThrowAnyException() //no "Unknown additional targeting class" exception
        }
    }

}
