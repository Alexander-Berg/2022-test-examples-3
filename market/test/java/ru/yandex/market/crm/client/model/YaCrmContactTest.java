package ru.yandex.market.crm.client.model;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Тесты для {@link YaCrmContact}.
 */
class YaCrmContactTest {

    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();


    @MethodSource("testFindToSyncData")
    @ParameterizedTest(name = "{0} -> {1} -> {2}")
    void testFindToSync(
            final String crmJsonFile, final String dbJsonFile, final String expectedJsonFile
    ) throws Exception {
        final Collection<YaCrmContact> crm = readContacts(crmJsonFile);
        final Collection<YaCrmContact> db = readContacts(dbJsonFile);
        final Collection<YaCrmContact> expected = readContacts(expectedJsonFile);
        final Collection<YaCrmContact> actual = YaCrmContact.findToSync(crm, db);

        ReflectionAssert.assertReflectionEquals(expected, actual, ReflectionComparatorMode.LENIENT_ORDER);
    }

    private static Stream<Arguments> testFindToSyncData() {
        return Stream.of(
                of("crm-contacts-api.json", "crm-contacts-api.json", "crm-contacts-empty.json"),
                of("crm-contacts-api.json", "crm-contacts-same.json", "crm-contacts-empty.json"),
                of("crm-contacts-api.json", "crm-contacts-another.json", "crm-contacts-another.json"),
                of("crm-contacts-single-kik.json", "crm-contacts-another.json", "crm-contacts-empty.json"),
                of("crm-contacts-another.json", "crm-contacts-single-kik.json", "crm-contacts-empty.json"),
                of("crm-contacts-api.json", "crm-contacts-single-kik.json", "crm-contacts-single-kik.json")
        );
    }

    private Collection<YaCrmContact> readContacts(final String jsonFile) throws Exception {
        try (InputStream resourceAsStream = getClass().getResourceAsStream(jsonFile)) {
            final CollectionType collectionType = JSON_OBJECT_MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, YaCrmContactResponse.class);
            return JSON_OBJECT_MAPPER.readValue(resourceAsStream, collectionType);
        }
    }

}
