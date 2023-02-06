package ru.yandex.market.logistics.nesu.jobs;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import lombok.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.UpdatePartnersHolidaysProperties;
import ru.yandex.market.logistics.nesu.exception.NesuException;
import ru.yandex.market.logistics.nesu.jobs.executor.UpdatePartnersHolidaysExecutor;
import ru.yandex.market.logistics.nesu.jobs.model.SetPartnerHolidaysData;
import ru.yandex.market.logistics.nesu.jobs.producer.SetPartnerHolidaysBatchProducer;
import ru.yandex.market.logistics.nesu.service.mds.MdsS3Service;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Обработка выгрузки выходных партнёров из mbi")
class UpdatePartnersHolidaysExecutorTest extends AbstractContextualTest {

    @Autowired
    private SetPartnerHolidaysBatchProducer setPartnerHolidaysBatchProducer;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private MdsS3Service mdsS3Service;

    @Autowired
    private UpdatePartnersHolidaysProperties updatePartnersHolidaysProperties;

    private UpdatePartnersHolidaysExecutor updatePartnersHolidaysExecutor;

    private SAXParser saxParser;

    private static final LocalDate DATE_FROM = LocalDate.of(2020, 10, 20);
    private static final LocalDate DATE_TO = LocalDate.of(2021, 1, 11);

    @BeforeEach
    void setup() throws Exception {
        saxParser = SAXParserFactory.newInstance().newSAXParser();
        updatePartnersHolidaysExecutor = new UpdatePartnersHolidaysExecutor(
            mdsS3Service,
            updatePartnersHolidaysProperties,
            setPartnerHolidaysBatchProducer,
            saxParser,
            clock
        );
        doNothing().when(setPartnerHolidaysBatchProducer).produceTask(any());
        doReturn(4)
            .when(updatePartnersHolidaysProperties).getTasksBatchSize();
    }

    @AfterEach
    void checkMocks() {
        Mockito.verifyNoMoreInteractions(mdsS3Client, setPartnerHolidaysBatchProducer);
    }

    @Test
    @DisplayName("Джоба выключается по настройке")
    void jobCanBeDisabled() {
        updatePartnersHolidaysExecutor = new UpdatePartnersHolidaysExecutor(
            mdsS3Service,
            new UpdatePartnersHolidaysProperties(),
            setPartnerHolidaysBatchProducer,
            saxParser,
            clock
        );

        updatePartnersHolidaysExecutor.doJob(null);
    }

    @Test
    @DisplayName("Есть подходящие календари")
    void hasSuitableCalendars() {
        mockHasSuitableCalendars();

        verifySetPartnerHolidaysTaskCreation(
            new PartnerHolidaysData(148L, 1L, List.of()),
            new PartnerHolidaysData(155L, 2L, List.of()),
            new PartnerHolidaysData(162L, 3L, List.of(LocalDate.of(2020, 10, 24), LocalDate.of(2020, 10, 31))),
            new PartnerHolidaysData(211L, 4L, List.of(LocalDate.of(2020, 10, 21), LocalDate.of(2020, 10, 25)))
        );
    }

    @Test
    @DisplayName("Запуск тасок батчами: чётное число батчей, после парсинга в буфере не остаются таски")
    void batchTasksCreationWithEvenBatch() {
        doReturn(2)
            .when(updatePartnersHolidaysProperties).getTasksBatchSize();

        mockHasSuitableCalendars();

        verify(setPartnerHolidaysBatchProducer, times(2)).produceTask(any());
    }

    @Test
    @DisplayName("Запуск тасок батчами: нечётное число батчей, после парсинга в буфере остаются таски")
    void batchTasksCreationWithOddBatch() {
        doReturn(3)
            .when(updatePartnersHolidaysProperties).getTasksBatchSize();

        mockHasSuitableCalendars();

        verifySetPartnerHolidaysTaskCreation(
            new PartnerHolidaysData(148L, 1L, List.of()),
            new PartnerHolidaysData(155L, 2L, List.of()),
            new PartnerHolidaysData(162L, 3L, List.of(LocalDate.of(2020, 10, 24), LocalDate.of(2020, 10, 31)))
        );
        verifySetPartnerHolidaysTaskCreation(
            new PartnerHolidaysData(211L, 4L, List.of(LocalDate.of(2020, 10, 21), LocalDate.of(2020, 10, 25)))
        );
    }

    @Test
    @DisplayName("Неудача при скачивании файла из MDS")
    void downloadFailed() {
        when(mdsS3Client.download(eq(createResourceLocation()), any(StreamCopyContentConsumer.class)))
            .thenThrow(new RuntimeException("error"));

        softly.assertThatThrownBy(() -> updatePartnersHolidaysExecutor.doJob(null))
            .isInstanceOf(NesuException.class)
            .hasMessage("Could not process holidays file")
            .hasCauseInstanceOf(RuntimeException.class)
            .extracting(t -> t.getCause().getMessage())
            .isEqualTo("error");

        verifyMdsDownload();
    }

    @Test
    @DisplayName("Ошибка при парсинге файла, пришел вообще не xml")
    void parseFailed() {
        mockMdsDownloadWithFile("jobs/executors/update_partner_holidays/bad_file.xml");

        softly.assertThatThrownBy(() -> updatePartnersHolidaysExecutor.doJob(null))
            .isInstanceOf(NesuException.class)
            .hasMessage("Could not process holidays file")
            .hasCauseInstanceOf(SAXException.class);

        verifyMdsDownload();
    }

    @Test
    @DisplayName("Дата в неожиданном формате")
    void incorrectDateFormat() {
        mockMdsDownloadWithFile("jobs/executors/update_partner_holidays/calendars_start-date_bad_format.xml");

        softly.assertThatThrownBy(() -> updatePartnersHolidaysExecutor.doJob(null))
            .isInstanceOf(NesuException.class)
            .hasMessage("Could not process holidays file")
            .hasCauseInstanceOf(DateTimeParseException.class)
            .extracting(t -> t.getCause().getMessage())
            .isEqualTo("Text '2020-10-20' could not be parsed at index 2");

        verifyMdsDownload();
    }

    @ParameterizedTest
    @MethodSource("unsupportedSource")
    @DisplayName("Ошибки в формате выгрузки")
    void unsupportedXmlStructure(
        String fileLocation,
        String causeErrorMessage
    ) {
        mockMdsDownloadWithFile(fileLocation);

        softly.assertThatThrownBy(() -> updatePartnersHolidaysExecutor.doJob(null))
            .isInstanceOf(NesuException.class)
            .hasMessage("Could not process holidays file")
            .hasCauseInstanceOf(NesuException.class)
            .extracting(t -> t.getCause().getMessage())
            .isEqualTo(causeErrorMessage);

        verifyMdsDownload();
    }

    private static Stream<Arguments> unsupportedSource() {
        return Stream.of(
            Arguments.of(
                "jobs/executors/update_partner_holidays/unsupported_format_1.xml",
                "Xml structure incorrect: got 'shop' tag to enter while having [calendars] in stack"
            ),
            Arguments.of(
                "jobs/executors/update_partner_holidays/unsupported_format_2.xml",
                "Xml structure incorrect: got 'partners' tag to enter while having [calendars] in stack"
            ),
            Arguments.of(
                "jobs/executors/update_partner_holidays/calendars_start-date_bad_input_1.xml",
                "Required attribute 'start-date' is empty"
            ),
            Arguments.of(
                "jobs/executors/update_partner_holidays/calendars_start-date_bad_input_2.xml",
                "Required attribute 'start-date' is empty"
            ),
            Arguments.of(
                "jobs/executors/update_partner_holidays/calendars_depth-days_bad_input_1.xml",
                "Required attribute 'depth-days' is empty"
            ),
            Arguments.of(
                "jobs/executors/update_partner_holidays/calendars_depth-days_bad_input_2.xml",
                "Required attribute 'depth-days' is empty"
            )
        );
    }

    private void mockHasSuitableCalendars() {
        mockMdsDownloadWithFile("jobs/executors/update_partner_holidays/has_suitable_calendars.xml");
        when(updatePartnersHolidaysProperties.getUpdateDbsGraphShopIds()).thenReturn(Set.of(148L, 162L));

        updatePartnersHolidaysExecutor.doJob(null);

        verifyMdsDownload();
    }

    @Value
    private static class PartnerHolidaysData {
        Long shopId;
        Long partnerId;
        List<LocalDate> dates;
    }

    private void verifySetPartnerHolidaysTaskCreation(PartnerHolidaysData... shopPartnerIdsToDates) {
        List<SetPartnerHolidaysData> payloadsData = Stream.of(shopPartnerIdsToDates)
            .map(data -> new SetPartnerHolidaysData(
                data.partnerId,
                DATE_FROM,
                DATE_TO,
                data.dates,
                data.shopId
            ))
            .toList();

        verify(setPartnerHolidaysBatchProducer)
            .produceTask(payloadsData);
    }

    private void mockMdsDownloadWithFile(String filePath) {
        when(mdsS3Client.download(eq(createResourceLocation()), any(StreamCopyContentConsumer.class)))
            .thenAnswer(invocation -> {
                try (InputStream systemResourceAsStream = getSystemResourceAsStream(filePath)) {
                    invocation.<StreamCopyContentConsumer<?>>getArgument(1)
                        .consume(Objects.requireNonNull(systemResourceAsStream));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
    }

    private void verifyMdsDownload() {
        verify(mdsS3Client).download(
            eq(createResourceLocation()),
            any(StreamCopyContentConsumer.class)
        );
    }

    private ResourceLocation createResourceLocation() {
        var fileLocation = updatePartnersHolidaysProperties.getFileLocation();
        return ResourceLocation.create(fileLocation.getBucket(), fileLocation.getKey());
    }
}
