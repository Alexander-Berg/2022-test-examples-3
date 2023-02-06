package ru.yandex.market.replenishment.autoorder.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationCountryInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationRegionInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationRegionSupplierInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationWarehouseInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.StocksWithLifetimes;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.yt.YtTableService;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

    private TestUtils() {
    }

    public static String readResource(String name) {
        InputStream inputStream = TestUtils.class.getResourceAsStream(name);
        return inputStream == null ? null
            : new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }

    public static LocalDate parseISOLocalDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String dtoToString(Object dto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(NON_ABSENT);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(dto);
    }


    public static <T, K> Map<K, T> collectionToMap(Collection<T> collection, Function<T, K> keyMapper) {
        return collection.stream().collect(Collectors.toMap(keyMapper, Function.identity()));
    }

    public static void mockTimeService(TimeService timeService, LocalDateTime nowDateTime) {
        mockTimeService(timeService, nowDateTime, nowDateTime);
    }

    public static void mockTimeService(TimeService timeService,
                                       LocalDateTime nowDateTime,
                                       LocalDateTime nowDateTimeUTC) {
        doReturn(nowDateTime.toLocalDate()).when(timeService).getNowDate();
        doReturn(nowDateTime).when(timeService).getNowDateTime();
        doReturn(nowDateTimeUTC).when(timeService).getNowDateTimeUTC();
    }

    public static void setMockedYtTableExistsCheckService(
        @NotNull Object object,
        @NotNull Map<String, Boolean> tableExistsFlags
    ) {
        final YtTableService ytTableService = mock(YtTableService.class);
        tableExistsFlags.forEach((path, exists) -> {
            when(ytTableService.checkYtTableExists(path)).thenReturn(exists);
            when(ytTableService.checkYtTableNotExists(path)).thenReturn(!exists);
        });

        ReflectionTestUtils.setField(object, "ytTableService", ytTableService);
    }

    public static void setMockedYtTableExistsCheckServiceAlwaysTrue(@NotNull Object object) {
        final YtTableService ytTableService = mock(YtTableService.class);
        when(ytTableService.checkYtTableExists(anyString())).thenReturn(true);
        when(ytTableService.checkYtTableNotExists(anyString())).thenReturn(false);
        ReflectionTestUtils.setField(object, "ytTableService", ytTableService);
    }

    public static RecommendationNew new1pRecommendation() {
        return newRecommendation(DemandType.TYPE_1P);
    }

    public static RecommendationNew newRecommendation(DemandType demandType) {
        RecommendationNew recommendation = new RecommendationNew();
        recommendation.setWarehouseInfo(new RecommendationWarehouseInfo());
        recommendation.setRegionInfo(new RecommendationRegionInfo());
        recommendation.setCountryInfo(new RecommendationCountryInfo());
        recommendation.setRegionSupplierInfo(new RecommendationRegionSupplierInfo());
        recommendation.setStocksWithLifetimes(new StocksWithLifetimes());
        recommendation.setDemandType(demandType);
        return recommendation;
    }

    public static Workbook mockWorkbook(Workbook workbook) {
        if (OsChecker.getOsType().equals("linux")) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                workbook.write(bos);
                byte[] barray = bos.toByteArray();
                InputStream is = new ByteArrayInputStream(barray);
                return new XSSFWorkbook(is);
            } catch (IOException e) {
                return null;
            }
        } else {
            return new SXSSFWorkbook((XSSFWorkbook) workbook);
        }
    }
}
