package ru.yandex.direct.core.entity.internalads.repository;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.internalads.model.TemplatePlace;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DirectTemplatePlaceRepositoryTest {
    private static final Long PLACE_ID1 = 22L;
    private static final Long PLACE_ID2 = 1357L;
    private static final Long TEMPLATE_ID1 = 2354L;
    private static final Long TEMPLATE_ID2 = 678L;
    private static final TemplatePlace TEMPLATE_PLACE1 = new TemplatePlace()
            .withPlaceId(PLACE_ID1)
            .withTemplateId(TEMPLATE_ID1);
    private static final TemplatePlace TEMPLATE_PLACE2 = new TemplatePlace()
            .withPlaceId(PLACE_ID2)
            .withTemplateId(TEMPLATE_ID2);

    @Autowired
    DirectTemplatePlaceRepository directTemplatePlaceRepository;

    @Before
    public void before() {
        directTemplatePlaceRepository.delete(directTemplatePlaceRepository.getAll());
        assertThat(directTemplatePlaceRepository.getAll(), hasSize(0));
    }

    @Test
    public void insertAndGet_compareData() {
        directTemplatePlaceRepository.add(List.of(TEMPLATE_PLACE1));
        var dbData = directTemplatePlaceRepository.getByTemplateId(TEMPLATE_ID1);
        assertThat(dbData, hasSize(1));
        var dbTemplatePlace = dbData.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(dbTemplatePlace.getTemplateId()).isEqualTo(TEMPLATE_ID1);
            softly.assertThat(dbTemplatePlace.getPlaceId()).isEqualTo(PLACE_ID1);
        });
    }

    @Test
    public void testDelete() {
        directTemplatePlaceRepository.add(List.of(TEMPLATE_PLACE1, TEMPLATE_PLACE2));
        var allTemplatePlaces = directTemplatePlaceRepository.getAll();
        assertThat(allTemplatePlaces, hasSize(2));
        directTemplatePlaceRepository.delete(List.of(TEMPLATE_PLACE1));
        assertThat(directTemplatePlaceRepository.getAll(), equalTo(List.of(TEMPLATE_PLACE2)));
    }

    @Test
    public void testGetMethods() {
        var mixedTemplatePlace = new TemplatePlace()
                .withTemplateId(TEMPLATE_ID1)
                .withPlaceId(PLACE_ID2);
        directTemplatePlaceRepository.add(
                List.of(TEMPLATE_PLACE1, TEMPLATE_PLACE2, TEMPLATE_PLACE1, mixedTemplatePlace));

        assertThat(directTemplatePlaceRepository.getAll(), hasSize(3));

        assertThat(directTemplatePlaceRepository.getByPlaceIds(List.of(PLACE_ID2)), hasSize(2));
        assertThat(directTemplatePlaceRepository.getByPlaceIds(List.of(PLACE_ID1, PLACE_ID2)), hasSize(3));

        assertThat(directTemplatePlaceRepository.getByTemplateId(TEMPLATE_ID1), hasSize(2));
        assertThat(directTemplatePlaceRepository.getByTemplateId(TEMPLATE_ID2), hasSize(1));

        assertThat(directTemplatePlaceRepository.getByTemplateIds(List.of(TEMPLATE_ID1, TEMPLATE_ID2)),
                equalTo(Map.of(
                        TEMPLATE_ID1, List.of(TEMPLATE_PLACE1, mixedTemplatePlace),
                        TEMPLATE_ID2, List.of(TEMPLATE_PLACE2))));
    }
}
