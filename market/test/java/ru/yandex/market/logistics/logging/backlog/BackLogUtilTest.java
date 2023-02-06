package ru.yandex.market.logistics.logging.backlog;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.MDC;

import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.request.trace.StringReplacer;


@ExtendWith(MockitoExtension.class)
class BackLogUtilTest {

    private static final Pattern PATTERN = Pattern.compile("payload=([^\\t]+)");

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    void createEntitiesIds() {
        Map<String, Collection<String>> entityMap = ImmutableMap.of(
            "order", ImmutableList.of("1", "2", "4"),
            "parcel", ImmutableList.of("(_)|(_)", "asddsaa")
        );

        String[] tuple = BackLogUtil.createEntitiesIds(entityMap);

        Assertions.assertThat(tuple).isEqualTo(new String[]{
            "order,parcel",
            "order:1,order:2,order:4,parcel:(_)|(_),parcel:asddsaa"
        });
    }

    @Test
    void createEntitiesIds_skipEmptyEntities() {
        Map<String, Collection<String>> entityMap = ImmutableMap.of(
            "order", ImmutableList.of("1", "2", "4"),
            "parcel", Collections.emptyList()
        );

        String[] tuple = BackLogUtil.createEntitiesIds(entityMap);
        Assertions.assertThat(tuple).isEqualTo(new String[]{"order", "order:1,order:2,order:4"});
    }

    @Test
    void createEntitiesIds_returnNullForEmptyMap() {
        Map<String, Collection<String>> entityMap = Collections.emptyMap();

        String[] tuple = BackLogUtil.createEntitiesIds(entityMap);
        Assertions.assertThat(tuple).isNull();
    }

    @Test
    void getCodeFromThrowable_searchCauseTrue() {
        Throwable throwable = new RuntimeException("Ololo", new IllegalArgumentException("pish"));

        String code = BackLogUtil.getCodeFromThrowable(throwable, true);
        Assertions.assertThat(code).isEqualTo("java.lang.IllegalArgumentException");
    }

    @Test
    void getCodeFromThrowable_searchCauseFalse() {
        Throwable throwable = new RuntimeException("Ololo", new IllegalArgumentException("pish"));

        String code = BackLogUtil.getCodeFromThrowable(throwable, false);
        Assertions.assertThat(code).isEqualTo("java.lang.RuntimeException");
    }

    @Test
    void writeException() {
        BackLogRecordBuilder.BackLogLevel level = BackLogRecordBuilder.BackLogLevel.TRACE;
        String payload = "testpayload";
        IllegalArgumentException throwable = new IllegalArgumentException("ololo");
        String code = BackLogUtil.getCodeFromThrowable(throwable, true);

        BackLogRecordBuilder builder = new BackLogRecordBuilder(level, code, payload);

        BackLogUtil.writeException(builder, throwable, "Illegal ololo");
        String result = builder.build();
        Assertions.assertThat(result).containsOnlyOnce("code=" + code);
        Assertions.assertThat(result).containsOnlyOnce("level=" + level);
        Assertions.assertThat(result).containsOnlyOnce("format=json-exception");
        Assertions.assertThat(result).containsOnlyOnce("payload={");
    }

    @Test
    void writeException_payloadValidJson() {
        IllegalArgumentException throwable = new IllegalArgumentException("ololo");
        BackLogRecordBuilder builder = new BackLogRecordBuilder(
            BackLogRecordBuilder.BackLogLevel.TRACE,
            BackLogUtil.getCodeFromThrowable(throwable, true),
            "testpayload"
        );

        BackLogUtil.writeException(builder, throwable, "ololo");
        String result = builder.build();

        Matcher matcher = PATTERN.matcher(result);
        Assertions.assertThat(matcher.find()).withFailMessage("Could not extract payload").isTrue();

        String payloadExtracted = matcher.group(1);
        payloadExtracted = StringReplacer.TSKV.revert(payloadExtracted);
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<LinkedHashMap<String, String>> typeReference =
            new TypeReference<LinkedHashMap<String, String>>() {
            };

        try {
            mapper.readValue(payloadExtracted, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void writeException_jsonNoSpaces() {
        IllegalArgumentException throwable = new IllegalArgumentException("ololo");
        BackLogRecordBuilder builder = new BackLogRecordBuilder(
            BackLogRecordBuilder.BackLogLevel.TRACE,
            BackLogUtil.getCodeFromThrowable(throwable, true),
            "testpayload"
        );

        BackLogUtil.writeException(builder, throwable, "Illegal ololo");
        String result = builder.build();

        Matcher matcher = PATTERN.matcher(result);
        Assertions.assertThat(matcher.find()).withFailMessage("Could not extract payload").isTrue();

        String payloadExtracted = matcher.group(1);
        payloadExtracted = StringReplacer.TSKV.revert(payloadExtracted);
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<LinkedHashMap<String, String>> typeReference =
            new TypeReference<LinkedHashMap<String, String>>() {
            };

        Map<String, String> payloadMap;
        try {
            payloadMap = mapper.readValue(payloadExtracted, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            // при сериализации не должно быть пробелов вне строковых литералов иначе visitParam в КХ не распарсит
            // https://clickhouse.yandex/docs/en/query_language/functions/json_functions/
            String json = mapper.writer().writeValueAsString(payloadMap);
            Assertions.assertThat(json).isEqualTo(payloadExtracted);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void logError_requestIdFromContextButMDC(@Mock Logger loggerMock) throws Exception {
        RequestContextHolder.createContext("testid");
        setFinalStatic(BackLogUtil.class.getDeclaredField("logger"), loggerMock);

        BackLogUtil.logError(
            new IllegalArgumentException("phone"),
            "Illegal phone",
            null);

        RequestContextHolder.clearContext();
        Mockito.verify(loggerMock).error(messageCaptor.capture());
        String message = messageCaptor.getValue();
        Assertions.assertThat(message).containsOnlyOnce("request_id=testid");
    }

    @Test
    void logError_getEntitiesFromMDC(@Mock org.slf4j.Logger loggerMock) throws Exception {
        String[] entitiesIds = BackLogUtil.createEntitiesIds(ImmutableMap.of(
            "order",
            ImmutableList.of("1", "3", "4"))
        );
        Assertions.assertThat(entitiesIds).isNotNull();

        MDC.put("back_log:entities_types", entitiesIds[0]);
        MDC.put("back_log:entities_ids", entitiesIds[1]);

        setFinalStatic(BackLogUtil.class.getDeclaredField("logger"), loggerMock);

        BackLogUtil.logError(
            new IllegalArgumentException("phone"),
            "Illegal phone",
            null
        );

        Mockito.verify(loggerMock).error(messageCaptor.capture());
        String message = messageCaptor.getValue();
        Assertions.assertThat(message).containsOnlyOnce("entity_types=" + entitiesIds[0]);
        Assertions.assertThat(message).containsOnlyOnce("entity_values=" + entitiesIds[1]);
    }

    @Test
    void logError_getEntitiesFromArgument(@Mock Logger loggerMock) throws Exception {
        Map<String, Collection<String>> map = ImmutableMap.of("order", ImmutableList.of("1", "3", "4"));
        String[] entitiesIds = BackLogUtil.createEntitiesIds(map);
        Assertions.assertThat(entitiesIds).isNotNull();

        setFinalStatic(BackLogUtil.class.getDeclaredField("logger"), loggerMock);

        BackLogUtil.logError(
            new IllegalArgumentException("phone"),
            "Illegal phone",
            map
        );

        Mockito.verify(loggerMock).error(messageCaptor.capture());
        String message = messageCaptor.getValue();
        Assertions.assertThat(message).containsOnlyOnce("entity_types=" + entitiesIds[0]);
        Assertions.assertThat(message).containsOnlyOnce("entity_values=" + entitiesIds[1]);
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}
