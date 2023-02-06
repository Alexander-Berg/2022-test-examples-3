package ru.yandex.market.logistics.management.controller.admin.korobyteRestriction;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.domain.dto.front.korobyteRestriction.KorobyteRestrictionNewDto;
import ru.yandex.market.logistics.management.domain.dto.front.korobyteRestriction.KorobyteRestrictionNewDto.KorobyteRestrictionNewDtoBuilder;
import ru.yandex.market.logistics.management.domain.dto.front.korobyteRestriction.KorobyteRestrictionUpdateDto;
import ru.yandex.market.logistics.management.domain.dto.front.korobyteRestriction.KorobyteRestrictionUpdateDto.KorobyteRestrictionUpdateDtoBuilder;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.management.util.TestUtil.pojoToString;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
class AdminKorobyteRestrictionControllerTestHelper {

    static final String METHOD_URL = "/admin/lms/korobyte-restrictions";
    static final String READ_ONLY = LMSPlugin.AUTHORITY_ROLE_KOROBYTE_RESTRICTIONS;
    static final String READ_WRITE = LMSPlugin.AUTHORITY_ROLE_KOROBYTE_RESTRICTIONS_EDIT;

    static final Long NON_EXISTS_KOROBYTE_RESTRICTION_ID = 9999L;
    static final Long KOROBYTE_RESTRICTION_ID_1 = 101L;
    static final Long KOROBYTE_RESTRICTION_ID_2 = 102L;
    static final Long KOROBYTE_RESTRICTION_ID_3 = 103L;

    @Nonnull
    static MockHttpServletRequestBuilder getGrid() {
        return get(METHOD_URL);
    }

    @Nonnull
    static MockHttpServletRequestBuilder getDetail(long korobyteRestrictionId) {
        return get(METHOD_URL + "/{korobyteRestrictionId}", korobyteRestrictionId);
    }

    @Nonnull
    static MockHttpServletRequestBuilder getNew() {
        return get(METHOD_URL + "/new");
    }

    @Nonnull
    static MockHttpServletRequestBuilder create(KorobyteRestrictionNewDto newDto) {
        return post(METHOD_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pojoToString(newDto));
    }

    @Nonnull
    static MockHttpServletRequestBuilder update(long korobyteRestrictionId, KorobyteRestrictionUpdateDto updateDto) {
        return put(METHOD_URL + "/{korobyteRestrictionId}", korobyteRestrictionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pojoToString(updateDto));
    }

    @Nonnull
    static MockHttpServletRequestBuilder delete(long korobyteRestrictionId) {
        return MockMvcRequestBuilders.delete(METHOD_URL + "/{korobyteRestrictionId}", korobyteRestrictionId);
    }

    @Nonnull
    static KorobyteRestrictionNewDto newDto() {
        return defaultNewDto().build();
    }

    @Nonnull
    static KorobyteRestrictionNewDtoBuilder defaultNewDto() {
        return emptyNewDto()
            .minWeightG(0)
            .maxWeightG(1)
            .minLengthCm(2)
            .maxLengthCm(3)
            .minWidthCm(4)
            .maxWidthCm(5)
            .minHeightCm(6)
            .maxHeightCm(7)
            .minSidesSumCm(8)
            .maxSidesSumCm(9);
    }

    @Nonnull
    static KorobyteRestrictionNewDtoBuilder emptyNewDto() {
        return KorobyteRestrictionNewDto.builder()
            .key("CREATE_TEST")
            .description("testing create");
    }

    @Nonnull
    static KorobyteRestrictionUpdateDto updateDto() {
        return emptyUpdateDto().build();
    }

    @Nonnull
    static KorobyteRestrictionUpdateDtoBuilder defaultUpdateDto() {
        return emptyUpdateDto()
            .minWeightG(0)
            .maxWeightG(1)
            .minLengthCm(2)
            .maxLengthCm(3)
            .minWidthCm(4)
            .maxWidthCm(5)
            .minHeightCm(6)
            .maxHeightCm(7)
            .minSidesSumCm(8)
            .maxSidesSumCm(9);
    }

    @Nonnull
    static KorobyteRestrictionUpdateDtoBuilder emptyUpdateDto() {
        return KorobyteRestrictionUpdateDto.builder().key("UPDATE_TEST");
    }
}
