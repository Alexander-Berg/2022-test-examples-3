package ru.yandex.direct.intapi.entity.gorynych;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.gorynych.model.CheckRequest;
import ru.yandex.direct.intapi.entity.gorynych.model.CheckRequestGroup;
import ru.yandex.direct.intapi.entity.gorynych.model.CheckResponse;
import ru.yandex.direct.intapi.entity.gorynych.model.CheckResponseGroup;
import ru.yandex.direct.intapi.entity.gorynych.model.CheckResponseInclusion;
import ru.yandex.direct.intapi.validation.IntApiDefect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@IntApiTest
public class KeywordsInclusionControllerTest {

    @Autowired
    KeywordsInclusionController controller;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Test
    public void validationPositiveTest() {
        List<Pair<String, CheckRequest>> tests = asList(
                Pair.of("one group",
                        new CheckRequest(singletonList(
                                new CheckRequestGroup(
                                        asList("москва", "ростов-на-дону"),
                                        asList("киев", "мать городов")))
                        )),
                Pair.of("empty",
                        new CheckRequest(emptyList())),
                Pair.of("empty lists",
                        new CheckRequest(singletonList(new CheckRequestGroup(emptyList(), emptyList()))))
        );
        SoftAssertions soft = new SoftAssertions();
        for (Pair<String, CheckRequest> test : tests) {
            ValidationResult<CheckRequest, IntApiDefect> vr = controller.validateCheckRequest(test.getRight());
            soft.assertThat(vr.hasAnyErrors())
                    .as(test.getLeft())
                    .isFalse();
        }
        soft.assertAll();
    }

    @Test
    public void validateNegatives() {
        List<Pair<String, CheckRequest>> tests = asList(
                Pair.of("incorrect keywords",
                        new CheckRequest(singletonList(
                                new CheckRequestGroup(
                                        asList("мос !! ква", "ростов-на-дону"),
                                        asList("киев", "мать городов")))
                        )),
                Pair.of("null lists",
                        new CheckRequest(singletonList(
                                new CheckRequestGroup(
                                        null,
                                        null)
                        ))),
                Pair.of("null group",
                        new CheckRequest(singletonList(null))
                )
        );
        SoftAssertions soft = new SoftAssertions();
        for (Pair<String, CheckRequest> test : tests) {
            ValidationResult<CheckRequest, IntApiDefect> vr = controller.validateCheckRequest(test.getRight());
            soft.assertThat(vr.hasAnyErrors())
                    .as(test.getLeft())
                    .isTrue();
        }
        soft.assertAll();
    }

    @Test
    public void processGroupPositive() {
        CheckRequest req = new CheckRequest(singletonList(
                new CheckRequestGroup(
                        asList("туманность андромеды", "ёжик тумане"),
                        asList("ёжик фиолетовый", "в тумане", "ёжик")))
        );
        CheckResponse resp = controller.check(req);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(resp.getGroups()).hasSize(1);
        CheckResponseGroup group = resp.getGroups().get(0);

        soft.assertThat(group.getInclusions()).hasSize(1);

        CheckResponseInclusion incl = group.getInclusions().iterator().next();
        soft.assertThat(incl.getPlusKeyword()).isEqualTo("ёжик тумане");
        soft.assertThat(incl.getMinusKeywords()).containsExactlyInAnyOrder("в тумане", "ёжик");

        soft.assertAll();
    }
}
