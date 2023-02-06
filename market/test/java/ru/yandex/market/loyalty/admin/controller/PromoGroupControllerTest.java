package ru.yandex.market.loyalty.admin.controller;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.controller.dto.AvatarImageWithLink;
import ru.yandex.market.loyalty.admin.controller.dto.HistoryStatus;
import ru.yandex.market.loyalty.admin.controller.dto.promogroup.PromoGroupConverter;
import ru.yandex.market.loyalty.admin.controller.dto.promogroup.PromoGroupDto;
import ru.yandex.market.loyalty.admin.controller.dto.promogroup.PromoGroupHistoryDto;
import ru.yandex.market.loyalty.admin.controller.dto.promogroup.PromoGroupHistoryVersionDto;
import ru.yandex.market.loyalty.admin.controller.dto.promogroup.PromoGroupPromoDto;
import ru.yandex.market.loyalty.admin.controller.dto.promogroup.PromoGroupWithPromoDto;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PagedResponse;
import ru.yandex.market.loyalty.api.model.coin.SmartShoppingThumbs;
import ru.yandex.market.loyalty.core.dao.PromoGroupDao;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroup;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupImpl;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupPromo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.avatar.AvatarImageId;
import ru.yandex.market.loyalty.core.service.promogroup.PromoGroupUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.core.service.promogroup.PromoGroupUtils.DEFAULT_SORT_ORDER;
import static ru.yandex.market.loyalty.core.service.promogroup.PromoGroupUtils.createDefaultPromoGroupPromo;
import static ru.yandex.market.loyalty.core.service.promogroup.PromoGroupUtils.createPromoGroup;
import static ru.yandex.market.loyalty.lightweight.CommonUtils.getNonNullValue;

@TestFor(PromoGroupController.class)
public class PromoGroupControllerTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String PROMO_GROUP_BASE_URL = "/api/promoGroup";
    private static final TypeReference<PagedResponse<PromoGroupDto>> PAGED_RESPONSE_TYPE_REFERENCE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<PromoGroupPromoDto>> LIST_TYPE_REFERENCE =
            new TypeReference<>() {
            };
    public static final AvatarImageWithLink AVATAR_IMAGE_WITH_LINK = new AvatarImageWithLink(new AvatarImageId(0,
            null), "AvatarImageId{groupId=0, imageName='null'}" + SmartShoppingThumbs.ORIGINAL);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Clock clock;
    @Autowired
    private PromoGroupDao promoGroupDao;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;
    @Autowired
    private PromoGroupConverter promoGroupConverter;

    private Promo testPromo;
    private PromoGroup testSavedPromoGroup;
    private Long testPromoGroupId;

    @Test
    public void should404IfPromoGroupNotFound() throws Exception {
        getPromoGroupById(-1L, status().isNotFound());
    }

    @Test
    public void shouldReturnPromoGroupById() throws Exception {
        initTestPromoGroup();
        String response = getPromoGroupById(testSavedPromoGroup.getId()).getContentAsString();

        assertEquals(
                response,
                objectMapper.writeValueAsString(promoGroupConverter.promoGroupToDto(testSavedPromoGroup))
        );
    }

    @Test
    public void shouldReturnPagedPromoGroupResponse() throws Exception {
        initTestPromoGroup();
        savePromoGroups();
        int pageSize = 5;
        int from = 1;

        String response = mockMvc.perform(
                get(PROMO_GROUP_BASE_URL + "/paged")
                        .param("from", String.valueOf(from))
                        .param("size", String.valueOf(pageSize))
                        .with(csrf())
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PagedResponse<PromoGroupDto> pagedResponse = readResponseAsPagedResponse(response);
        assertEquals(from, pagedResponse.getPager().getCurrentPage());
        assertEquals(pageSize, pagedResponse.getPager().getPageSize());
        assertEquals(pageSize, pagedResponse.getData().size());
    }

    @Test
    public void shouldUpdatePromoGroup() throws Exception {
        initTestPromoGroup();
        PromoGroupDto promoGroupDto = new PromoGroupDto(
                testSavedPromoGroup.getId(),
                "someUpdatedName",
                testSavedPromoGroup.getPromoGroupType(),
                "someUpdatedToken",
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(10),
                // because using wrong AvatarClient Mock
                AVATAR_IMAGE_WITH_LINK
        );
        String content = objectMapper.writeValueAsString(new PromoGroupWithPromoDto(promoGroupDto));
        mockMvc.perform(
                post(PROMO_GROUP_BASE_URL + "/update").with(csrf())
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .content(content)
        ).andExpect(status().isOk());

        assertEquals(
                objectMapper.writeValueAsString(promoGroupDto),
                getPromoGroupById(testSavedPromoGroup.getId()).getContentAsString()
        );
    }

    @Test
    public void shouldUpdatePromoGroupChangingLinkedPromos() throws Exception {
        initTestPromoGroup();
        Promo promoToDelete = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFreeDelivery());
        Promo promoToAdd = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery());
        int newSortOrder = 111;

        PromoGroupDto promoGroupDto = promoGroupConverter.promoGroupToDto(testSavedPromoGroup);

        promoGroupDao.insertPromoGroupPromo(
                Arrays.asList(
                        createDefaultPromoGroupPromo(testPromoGroupId, testPromo),
                        createDefaultPromoGroupPromo(testPromoGroupId, promoToDelete)
                )
        );

        PromoGroupPromo preservedPromoGroupPromo =
                promoGroupDao.getPromoGroupPromosByPromoGroupId(testPromoGroupId).stream()
                .filter(promoGroupPromo -> testPromo.getId() == promoGroupPromo.getPromoId())
                .findFirst()
                .orElseThrow(AssertionError::new);


        List<PromoGroupPromoDto> promoDtos = Arrays.asList(
                new PromoGroupPromoDto(
                        preservedPromoGroupPromo.getId(),
                        preservedPromoGroupPromo.getPromoId(),
                        testPromoGroupId,
                        newSortOrder,
                        null
                ),
                new PromoGroupPromoDto(
                        null,
                        promoToAdd.getId(),
                        testPromoGroupId,
                        newSortOrder,
                        null
                )
        );

        mockMvc.perform(
                post(PROMO_GROUP_BASE_URL + "/update").with(csrf())
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(new PromoGroupWithPromoDto(
                                promoGroupDto,
                                promoDtos
                        )))
        ).andExpect(status().isOk());


        List<PromoGroupPromo> promos = promoGroupDao.getPromoGroupPromosByPromoGroupId(testPromoGroupId);


        assertTrue(promos.stream().noneMatch(promoGroupPromo -> promoGroupPromo.getPromoId() == promoToDelete.getId()));
        assertThat(
                promos,
                Matchers.<List<PromoGroupPromo>>allOf(
                        hasSize(2),
                        everyItem(hasProperty("sortOrder", equalTo(newSortOrder))),
                        not(hasItem(hasProperty("promoId", equalTo(promoToDelete.getId()))))
                )
        );
    }

    @Test
    public void shouldSavePromoGroupWithLinkedPromo() throws Exception {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFreeDelivery());

        mockMvc.perform(
                post(PROMO_GROUP_BASE_URL + "/update")
                        .with(csrf())
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(new PromoGroupWithPromoDto(
                                promoGroupConverter.promoGroupToDto(createPromoGroup(clock, "someToken")),
                                Collections.singletonList(
                                        new PromoGroupPromoDto(
                                                null,
                                                promo.getId(),
                                                null,
                                                DEFAULT_SORT_ORDER,
                                                null
                                        )
                                )
                        )))
        ).andExpect(status().isOk());

        long savedPromoGroupId = promoGroupDao.getAllPromoGroups()
                .stream()
                .findFirst()
                .map(PromoGroup::getId)
                .orElseThrow(AssertionError::new);

        List<PromoGroupPromo> savedPromoGroupsPromos = promoGroupDao.getPromoGroupPromosByPromoGroupId(
                savedPromoGroupId
        );
        assertThat(
                savedPromoGroupsPromos,
                hasItem(hasProperty("promoId", equalTo(promo.getId())))
        );
    }

    @Test
    public void shouldReturnPromoForPromoGroup() throws Exception {
        initTestPromoGroup();
        promoGroupDao.insertPromoGroupPromo(
                Collections.singletonList(PromoGroupUtils.createDefaultPromoGroupPromo(testPromoGroupId, testPromo))
        );

        String response = mockMvc.perform(
                get(PROMO_GROUP_BASE_URL + "/{id}/promos", testPromoGroupId)
                        .with(csrf())
        ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(1, readResponseAsPromoGroupPromoList(response).size());
    }

    @Test
    public void checkPromoGroupHistoryApi() throws Exception {
        initTestPromoGroup();
        promoGroupDao.insertPromoGroupPromo(
                Collections.singletonList(PromoGroupUtils.createDefaultPromoGroupPromo(testPromoGroupId, testPromo))
        );

        checkPromoGroupHistory(2, 1, 0, HistoryStatus.ADDED, testPromo.getId());

        PromoGroupDto promoGroupDto = new PromoGroupDto(
                testPromoGroupId,
                "someUpdatedName",
                testSavedPromoGroup.getPromoGroupType(),
                "someUpdatedToken",
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(10),
                AVATAR_IMAGE_WITH_LINK
        );
        mockMvc.perform(
                post(PROMO_GROUP_BASE_URL + "/update").with(csrf())
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(new PromoGroupWithPromoDto(promoGroupDto)))
        ).andExpect(status().isOk());

        checkPromoGroupHistory(4, 1, 0, HistoryStatus.DELETED, testPromo.getId());

        Promo newPromo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFreeDelivery());
        promoGroupDao.insertPromoGroupPromo(List.of(createDefaultPromoGroupPromo(testPromoGroupId, newPromo)));

        checkPromoGroupHistory(5, 1, 0, HistoryStatus.ADDED, newPromo.getId());
    }

    private void checkPromoGroupHistory(int versionSize,
                                        int promoCount,
                                        int promoNumber,
                                        HistoryStatus historyStatus,
                                        Long promoId) throws Exception {
        PromoGroupHistoryDto promoGroupHistoryDto = getPromoGroupHistoryDto();

        assertTrue(promoCount >= 0);
        assertTrue(promoNumber >= 0);
        assertTrue(promoNumber <= promoCount);
        assertNotNull(promoGroupHistoryDto);

        assertEquals(testPromoGroupId, promoGroupHistoryDto.getId());
        assertEquals(testSavedPromoGroup.getPromoGroupType().getCode(), promoGroupHistoryDto.getPromoGroupType());

        List<PromoGroupHistoryVersionDto> versions = promoGroupHistoryDto.getVersions();
        assertEquals(versionSize, versions.size());

        List<PromoGroupPromoDto> lastVersionPromos = versions.get(versionSize - 1).getPromos();
        assertEquals(promoCount, lastVersionPromos.size());

        PromoGroupPromoDto lastVersionFirstPromo = lastVersionPromos.get(promoNumber);
        assertEquals(testPromoGroupId, lastVersionFirstPromo.getPromoGroupId());
        assertEquals(historyStatus, lastVersionFirstPromo.getHistoryStatus());
        assertEquals(promoId, lastVersionFirstPromo.getPromoId());
    }

    private PromoGroupHistoryDto getPromoGroupHistoryDto() throws Exception {
        return objectMapper.readValue(mockMvc
                .perform(get(PROMO_GROUP_BASE_URL + "/" + testPromoGroupId + "/history"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PromoGroupHistoryDto.class);
    }

    private List<PromoGroupPromoDto> readResponseAsPromoGroupPromoList(String response) throws IOException {
        return objectMapper.readValue(response, LIST_TYPE_REFERENCE);
    }

    @NotNull
    private MockHttpServletResponse getPromoGroupById(Long id) throws Exception {
        return getPromoGroupById(id, status().isOk());
    }

    @NotNull
    private MockHttpServletResponse getPromoGroupById(Long id, ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(
                get(PROMO_GROUP_BASE_URL + "/{promoId}", id).with(csrf()))
                .andExpect(resultMatcher)
                .andReturn()
                .getResponse();
    }

    private PromoGroup insertAndReturnPromoGroup() {
        long promoGroupId =
                promoGroupDao.insertPromoGroup(PromoGroupUtils.createDefaultPromoGroup(clock));
        Optional<PromoGroupImpl> promoGroup = promoGroupDao.getPromoGroupById(promoGroupId);
        assertTrue(promoGroup.isPresent());
        assertNotNull(promoGroup.get().getId());
        return promoGroup.get();
    }

    private void savePromoGroups() {
        IntStream.range(0, 20)
                .mapToObj(i -> {
                    PromoGroup promoGroup = PromoGroupUtils.createPromoGroup(clock, "token" + i);
                    return promoGroupDao.insertPromoGroup(promoGroup);
                })
                .collect(Collectors.toList());
    }

    private PagedResponse<PromoGroupDto> readResponseAsPagedResponse(String response) throws IOException {
        return objectMapper.readValue(
                response,
                PAGED_RESPONSE_TYPE_REFERENCE
        );
    }

    private void initTestPromoGroup() {
        testSavedPromoGroup = insertAndReturnPromoGroup();
        testPromoGroupId = getNonNullValue(testSavedPromoGroup.getId());
        testPromo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFreeDelivery());
    }
}
