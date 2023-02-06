package ru.yandex.market.pvz.core.domain.vaccination;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

class VaccinationAnswerYtDeserializerTest {

    @Test
    void parse() {
        long id = 130174498;
        String created = "2021-08-01T00:36:41Z";
        String answer = getFileContent("json/vaccination_answer.json");

        VaccinationYtData vaccinationYtData = new VaccinationYtData(id, created, answer);

        VaccinationAnswerYtDeserializer deserializer = new VaccinationAnswerYtDeserializer();
        var actual = deserializer.deserialize(vaccinationYtData);

        var expected = VaccinationPickupPointParams.builder()
                .id(vaccinationYtData.getId())
                .partnerName("ИП Мосьпан Вадим Романович")
                .region("Краснодарский край")
                .city("Краснодар")
                .address("Кожевенная 30")
                .amount((short) 1)
                .vaccinatedAmount((short) 1)
                .created(OffsetDateTime.parse(vaccinationYtData.getCreated()))
                .employees(List.of(
                        VaccinationEmployeeParams.builder()
                                .name("Мосьпан Вадим Романович")
                                .passport("312338594")
                                .nationality("Рф")
                                .birthday(LocalDate.of(1995, 10, 22))
                                .firstVaccinationDate(LocalDate.of(2021, 7, 16))
                                .secondVaccinationDate(LocalDate.of(2021, 8, 6))
                                .certificate("9230 0000 3258 2056")
                                .signed(true)
                                .build(),
                        VaccinationEmployeeParams.builder()
                                .name("Панарина Ольга Сергеевна")
                                .passport("312338595")
                                .nationality("Рф")
                                .birthday(LocalDate.of(1994, 10, 28))
                                .firstVaccinationDate(LocalDate.of(2021, 7, 14))
                                .secondVaccinationDate(LocalDate.of(2021, 8, 4))
                                .certificate("9230 0000 3258 2057")
                                .signed(true)
                                .build()))
                .build();

        assertThat(actual).isEqualTo(expected);
    }
}
