package ru.yandex.market.rg.asyncreport.claimlostorders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.ww.WwClaimLostOrdersGenerationService;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.ClaimFromPartner;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "claimLostOrdersData.csv")
public class ClaimLostOrdersReportGeneratorTest extends FunctionalTest {
    @Autowired
    private WwClient wwClient;

    @Autowired
    private WwClaimLostOrdersGenerationService claimLostOrdersGenerationService;

    @Autowired
    private ClaimLostOrdersReportGenerator claimLostOrdersReportGenerator;

    private ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @DisplayName("Успешная генерация данных для претензии (с контактами и без, с КПП и без, с 910 ЧП и без)")
    @ParameterizedTest
    @MethodSource("paramsForSuccessGenerateReport")
    void testSuccessGenerateReport(Long partnerId, String pathToExpected) throws IOException {
        ClaimLostOrdersReportParams params = new ClaimLostOrdersReportParams(partnerId,
                getDate(2020,9,23), getDate(2020,9,25));
        ArgumentCaptor<ClaimFromPartner> captor = ArgumentCaptor.forClass(ClaimFromPartner.class);
        claimLostOrdersReportGenerator.generate("001", params);
        verify(wwClient).generateClaimFromPartner(captor.capture(), eq(DocumentFormat.PDF));
        Assertions.assertThat(captor.getValue()).isEqualTo(expectedData(pathToExpected));
    }

    private static Stream<Arguments> paramsForSuccessGenerateReport() {
        return Stream.of(
                Arguments.of(1000001L, "claimFromPartner.json"),
                Arguments.of(1000002L, "claimFromPartnerWithoutContacts.json")
        );
    }

    @Test
    @DisplayName("проверка на возникновение ошибки при генерации пустой претензии")
    void testExceptionEmptyOrders() {
        Assertions.assertThatThrownBy(() ->
            claimLostOrdersGenerationService.getClaimData(1000003L,
                    getDate(2020,9,23), getDate(2020,9,25))
        ).isInstanceOf(IllegalStateException.class)
                .hasMessage("No orders for claim between dates 2020-09-23 and 2020-09-25 found for shop 1000003");
    }
    /**
     * получаем данные для сравнения и меняем дату подачи заявления из файла на текущую
     * @param path имя файла с ожидаемыми данными по отчету
     * @return данные для сравнения
     * @throws IOException
     */
    private ClaimFromPartner expectedData(String path) throws IOException {
        String expectedJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(path)),
                StandardCharsets.UTF_8
        );
        return objectMapper.readValue(expectedJson, ClaimFromPartner.class);
    }

    private Instant getDate(int year, int month, int day) {
        return LocalDate.of(year,month,day).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
