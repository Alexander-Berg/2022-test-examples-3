package ru.yandex.market.core.program.partner.calculator.marketplace;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.program.partner.model.ProgramSubStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.program.partner.model.Substatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест проверяет работу {@link NoLoadedOffersResolver}.
 */
class NoLoadedOffersResolverTest extends FunctionalTest {

    @Autowired
    private NoLoadedOffersResolver noLoadedOffersResolver;

    /**
     * @return Стрим аргументов. Первый аргумент id поставщика,
     *         второй аргумент true если у поставщика нет загруженных товаров.
     */
    public static Stream<Arguments> suppliers() {
        return Stream.of(
            Arguments.of(548940L, true),
            Arguments.of(546979L, false)
        );
    }

    @ParameterizedTest
    @MethodSource("suppliers")
    @DbUnitDataSet(before = "NoLoadedOffersResolverTest.checkPartnerWithoutOffers.csv")
    public void checkPartnerWithoutOffers(long partnerId, boolean isEmptyLoadedTotalOffers) {
        var answer = noLoadedOffersResolver.resolve(partnerId, null);

        if (isEmptyLoadedTotalOffers) {
            assertThat(answer).isPresent();
            var actualProgramStatusBuilder = answer.get();
            assertThat(actualProgramStatusBuilder.getStatus()).isEqualTo(Status.FAILED);
            assertThat(actualProgramStatusBuilder.getEnabled()).isFalse();
            assertThat(actualProgramStatusBuilder.getSubStatuses())
                    .containsEntry(
                            Substatus.NO_LOADED_OFFERS.getId(),
                            ProgramSubStatus.builder().code(Substatus.NO_LOADED_OFFERS).build()
                    );
        } else {
            assertThat(answer).isEmpty();
        }

    }
}
