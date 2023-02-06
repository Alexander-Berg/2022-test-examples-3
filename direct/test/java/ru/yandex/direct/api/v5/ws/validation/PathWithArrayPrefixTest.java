package ru.yandex.direct.api.v5.ws.validation;

import java.util.List;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static java.util.Arrays.asList;

public class PathWithArrayPrefixTest {

    @Test
    public void fromPath_success() {
        List<JsonMappingException.Reference> path = asList(
                field("top"),
                field("array"),
                index(0),
                field("objInArray"));
        PathWithArrayPrefix actual = PathWithArrayPrefix.fromPath(path);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.getPrefixArrayPath()).hasSize(3);
            softAssertions.assertThat(actual.getInArrayPath()).hasSize(1);
            softAssertions.assertThat(actual.getPrefixArrayPath()).isEqualTo(path.subList(0, 3));
            softAssertions.assertThat(actual.getInArrayPath()).isEqualTo(path.subList(3, 4));
        });
    }

    private static JsonMappingException.Reference field(String field) {
        return new JsonMappingException.Reference(Object.class, field);
    }

    @SuppressWarnings("SameParameterValue")
    private static JsonMappingException.Reference index(int idx) {
        return new JsonMappingException.Reference(Object.class, idx);
    }

}
