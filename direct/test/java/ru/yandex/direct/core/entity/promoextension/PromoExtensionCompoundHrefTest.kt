package ru.yandex.direct.core.entity.promoextension

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.core.entity.promoextension.model.PromoExtensionUnit
import ru.yandex.direct.core.entity.promoextension.model.PromoExtensionUnit.PCT
import ru.yandex.direct.core.entity.promoextension.model.PromoExtensionUnit.RUB
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsPrefix
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsPrefix.from
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsPrefix.to
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.cashback
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.discount
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.free
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.gift
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.installment
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.profit
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.checkEquals
import java.time.LocalDate

@RunWith(Parameterized::class)
class PromoExtensionCompoundHrefTest(
        private val type: PromoactionsType,
        private val prefix: PromoactionsPrefix?,
        private val amount: Long?,
        private val unit: PromoExtensionUnit?,
        private val description: String,
        private val finishDate: LocalDate?,
        private val compoundDescriptionWithoutDateExpected: String,
        private val compoundDescriptionExpected: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(profit, from, 15L, RUB, "На Смартфоны", LocalDate.of(2031, 12, 7),
                "Выгода от 15 ₽ на Смартфоны", "Выгода от 15 ₽ на Смартфоны до 7 декабря"),
            arrayOf(discount, null, 17L, PCT, "Для слонов", LocalDate.of(2032, 11, 18),
                "Скидка 17% для слонов", "Скидка 17% для слонов до 18 ноября"),
            arrayOf(cashback, to, 1500L, RUB, "гладиолус", LocalDate.of(2041, 8, 2),
                "Кешбэк до 1500 ₽ гладиолус", "Кешбэк до 1500 ₽ гладиолус до 2 августа"),
            arrayOf(installment, from, 112345L, RUB, "На Смартфоны", LocalDate.of(2051, 7, 6),
                "Рассрочка 0% на Смартфоны", "Рассрочка 0% на Смартфоны до 6 июля"),
            arrayOf(gift, to, 84848L, null, "При покупке смартфона второй", LocalDate.of(2063, 3, 1),
                "При покупке смартфона второй в подарок!", "При покупке смартфона второй в подарок! До 1 марта"),
            arrayOf(free, null, 123123L, PCT, "слону", LocalDate.of(2051, 4, 10),
                "Слону бесплатно!", "Слону бесплатно! До 10 апреля"),
            arrayOf(discount, null, 1234L, RUB, "на любые услуги", null,
                "Скидка 1234 ₽ на любые услуги", "Скидка 1234 ₽ на любые услуги"),
            arrayOf(profit, null, 12345L, RUB, "на любые услуги", null,
                "Выгода 12 345 ₽ на любые услуги", "Выгода 12 345 ₽ на любые услуги"),
            arrayOf(cashback, null, 12345678L, RUB, "на любые услуги", null,
                "Кешбэк 12 345 678 ₽ на любые услуги", "Кешбэк 12 345 678 ₽ на любые услуги"),
            arrayOf(installment, null, 1L, PCT, "на все квартиры!", LocalDate.of(2044, 9, 3),
                "Рассрочка 0% на все квартиры!", "Рассрочка 0% на все квартиры! До 3 сентября"),
        )
    }

    @Test
    fun testPromoactionCompoundDescription() {
        val promoToTestWithDate = PromoExtension(
            promoExtensionId = null,
            clientId = ClientId.fromLong(123L),
            type = type,
            prefix = prefix,
            amount = amount,
            unit = unit,
            description = description,
            finishDate = finishDate,
            startDate = null,
            statusModerate = PromoactionsStatusmoderate.Sending,
            href = null,
        )
        val promoToTestWithoutDate = promoToTestWithDate.copy(finishDate = null)
        softly {
            promoToTestWithDate.compoundDescription.checkEquals(compoundDescriptionExpected)
            promoToTestWithDate.compoundDescriptionWithoutDate.checkEquals(compoundDescriptionWithoutDateExpected)
            promoToTestWithoutDate.compoundDescription.checkEquals(compoundDescriptionWithoutDateExpected)
            promoToTestWithoutDate.compoundDescriptionWithoutDate.checkEquals(compoundDescriptionWithoutDateExpected)
        }
    }
}
