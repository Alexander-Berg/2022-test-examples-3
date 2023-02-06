package ru.yandex.travel.api.services.avia.country_restrictions;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.travel.api.models.avia.country_restrictions.v1.Metric;
import ru.yandex.travel.api.models.avia.country_restrictions.v1.RichString;
import ru.yandex.travel.api.models.avia.country_restrictions.v1.RichStringBlock;
import ru.yandex.travel.proto.avia.TCountryRestrictionsV1;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "avia-country-restrictions-v1.enabled=true"
})
@ActiveProfiles("test")
public class CountryRestrictionsServiceV1Test {
    @Autowired
    Environment environment;

    @Autowired
    CountryRestrictionsServiceV1 countryRestrictionsService;

    private Metric buildMetric(Integer value, String textValue) {
        Metric metric = new Metric();
        metric.setValue(JsonNodeFactory.instance.numberNode(value));

        RichStringBlock rsb = new RichStringBlock();
        rsb.setBlockType("text");
        ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        rsb.setData(mapper.valueToTree(new Object() {
            final public String text = textValue;
        }));

        RichString rs = new RichString();
        rs.setData(List.of(rsb));
        metric.setText(rs);

        metric.setAdditions(java.util.Collections.emptyList());
        metric.setExclusions(java.util.Collections.emptyList());
        return metric;
    }

    @Test
    public void testCorrectJsonParser() {
        String json = "{\"m1\": {\"value\": 10, \"text\": {\"data\": [{\"block_type\": \"text\", \"data\": {\"text\": \"a\"}}]}, \"exclusions\": [], \"additions\": [], \"aa\": true}}\"";
        TCountryRestrictionsV1 countryRestrictions = TCountryRestrictionsV1.newBuilder().setKey(0).setValue(json).build();
        Map<String, Metric> actual = countryRestrictionsService.valueGetter(countryRestrictions);

        Metric metric = buildMetric(10, "a");
        Map<String, Metric> expected = Map.of("m1", metric);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testJsonParserWithNoRequiredField() {
        CountryRestrictionsMeters meters = mock(CountryRestrictionsMeters.class, Mockito.RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(countryRestrictionsService, "meters", meters);

        String json = "{\"m1\": {\"value\": 10, \"exclusions\": [], \"additions\": [], \"aa\": true}}";
        TCountryRestrictionsV1 countryRestrictions = TCountryRestrictionsV1.newBuilder().setKey(0).setValue(json).build();
        countryRestrictionsService.valueGetter(countryRestrictions);

        verify(meters, times(1)).getV1JsonParseErrorsNoRequiredFields();
    }

    @Test
    public void testJsonBadStructure() {
        CountryRestrictionsMeters meters = mock(CountryRestrictionsMeters.class, Mockito.RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(countryRestrictionsService, "meters", meters);

        String json = "this is too bad json";
        TCountryRestrictionsV1 countryRestrictions = TCountryRestrictionsV1.newBuilder().setKey(0).setValue(json).build();
        countryRestrictionsService.valueGetter(countryRestrictions);

        verify(meters, times(1)).getV1JsonParseErrorsOther();
    }
}
