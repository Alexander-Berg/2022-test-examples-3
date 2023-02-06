package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.Tag;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;

@DatabaseSetup({
    "/repository/transportation/all_kinds_of_transportation.xml",
    "/repository/tag/tags.xml",
})
class TagMapperTest extends AbstractContextualTest {
    private static final ZonedDateTime STUB_ZONE_DATE_TIME = ZonedDateTime.of(2021, 10, 11,
            17, 0, 0, 0, ZoneId.systemDefault());

    @Autowired
    private TagMapper mapper;

    @Test
    void getByTransportationIds() {
        List<Tag> tags = mapper.getByTransportationIds(Collections.singletonList(2L));

        softly.assertThat(tags).containsExactly(
            tag(2L, TagCode.FFWF_ROOT_REQUEST_ID, "100500", STUB_ZONE_DATE_TIME)
        );
    }

    @ParameterizedTest
    @MethodSource("getByTransportationIdsAndCodesSource")
    void getByTransportationIdsAndCodes(List<Long> transportationIds, List<TagCode> tagCodes, Tag[] expectedTags) {
        List<Tag> tags = mapper.getByTransportationIdsAndCodes(
            transportationIds,
            tagCodes
        );

        softly.assertThat(tags).containsExactly(
            expectedTags
        );
    }

    static Stream<Arguments> getByTransportationIdsAndCodesSource() {
        return Stream.of(
            Arguments.of(
                Collections.singletonList(2L),
                Collections.singletonList(TagCode.FFWF_ROOT_REQUEST_ID),
                new Tag[]{
                    tag(2L, TagCode.FFWF_ROOT_REQUEST_ID, "100500", null)
                }
            ),
            Arguments.of(
                Collections.singletonList(2L),
                Collections.singletonList(TagCode.AXAPTA_ID),
                new Tag[]{}
            ),
            Arguments.of(
                Collections.singletonList(2L),
                Collections.emptyList(),
                new Tag[]{}
            ),
            Arguments.of(
                Collections.emptyList(),
                Collections.singletonList(TagCode.FFWF_ROOT_REQUEST_ID),
                new Tag[]{}

            )
        );
    }

    @DatabaseSetup({
        "/repository/tag/tags.xml",
        "/repository/tag/tags_another_transportation.xml",
    })
    @Test
    void getByCodeAndValues() {
        assertContainsExactlyInAnyOrder(
            mapper.getByCodeAndValues(TagCode.FFWF_ROOT_REQUEST_ID, List.of("100500", "100700")),
            tag(2L, TagCode.FFWF_ROOT_REQUEST_ID, "100500", STUB_ZONE_DATE_TIME),
            tag(3L, TagCode.FFWF_ROOT_REQUEST_ID, "100500", STUB_ZONE_DATE_TIME),
            tag(3L, TagCode.FFWF_ROOT_REQUEST_ID, "100700", STUB_ZONE_DATE_TIME)
        );

        assertContainsExactlyInAnyOrder(
            mapper.getByCodeAndValues(TagCode.FFWF_ROOT_REQUEST_ID, List.of("100700")),
            tag(3L, TagCode.FFWF_ROOT_REQUEST_ID, "100700", STUB_ZONE_DATE_TIME)
        );
    }

    @ExpectedDatabase(
        value = "/repository/tag/after/inserted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void insert() {
        mapper.insert(
            List.of(
                tag(2L, TagCode.FFWF_ROOT_REQUEST_ID, "100500", null),
                tag(2L, TagCode.FFWF_ROOT_REQUEST_ID, "100700", null)
            )
        );
    }

    @DatabaseSetup({
        "/repository/tag/tags.xml",
        "/repository/tag/tags_another_transportation.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/tags.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void delete() {
        mapper.delete(3);
    }

    @DatabaseSetup({
        "/repository/tag/tags.xml",
        "/repository/tag/tags_ffwf_included_request_id_plan.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/after/ffwf_included_request_id_plan_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void deleteByTransportationIdAndCode() {
        mapper.deleteByTransportationIdAndCode(2L, TagCode.FFWF_INCLUDED_REQUEST_ID_PLAN);
    }

    private static Tag tag(Long transportationId, TagCode code, String value, ZonedDateTime zonedDateTime) {
        Tag tag = new Tag();
        tag.setTransportationId(transportationId);
        tag.setCode(code);
        tag.setValue(value);
        tag.setCreated(zonedDateTime);

        return tag;
    }
}
