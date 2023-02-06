package ru.yandex.market.pers.author.mock.mvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.author.client.api.dto.UserExpertiseMailDto;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.model.AgitationEntity;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.expertise.dto.ExpertiseGainDto;
import ru.yandex.market.pers.author.expertise.dto.UserExpertiseDto;
import ru.yandex.market.pers.author.expertise.dto.UserExpertiseDtoListWithHidMapping;
import ru.yandex.market.pers.author.expertise.model.Expertise;
import ru.yandex.market.pers.author.expertise.model.ExpertiseCost;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.ADD_SHOP;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.AGIT_COMPLETED_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.AGIT_REVOKED_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.ENTITY_ID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.ENTITY_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.HID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.UID_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 14.05.2020
 */
@Service
public class ExpertiseMvcMocks extends AbstractMvcMocks {

    public List<Expertise> getDictionary() {
        DtoList<Expertise> result = parseValue(
            invokeAndRetrieveResponse(
                get("/expertise/dictionary")
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData();
    }

    public List<ExpertiseCost> getCostDictionary() {
        DtoList<ExpertiseCost> result = parseValue(
            invokeAndRetrieveResponse(
                get("/expertise/dictionary/cost")
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData();
    }


    public List<Long> getDictionaryExpertiseByHid(long hid) {
        DtoList<Long> result = parseValue(
            invokeAndRetrieveResponse(
                get("/expertise/dictionary/by/hid/" + hid)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData();
    }

    public Map<Long, Long> getDictionaryExpertiseByHids(List<Long> hids) {
        return parseValue(
                invokeAndRetrieveResponse(
                        get("/expertise/dictionary/by/hid")
                                .param(HID_KEY, toArrayStr(hids))
                                .accept(MediaType.APPLICATION_JSON),
                        status().is2xxSuccessful()
                ), new TypeReference<>() {
                });
    }

    public UserExpertiseDtoListWithHidMapping getTopUserExpertiseByHids(Long userId,
                                                                         List<Long> hids,
                                                                         boolean addShop) {
        return parseValue(
            invokeAndRetrieveResponse(
                get("/expertise/UID/" + userId + "/by/hids")
                    .param(HID_KEY, toArrayStr(hids))
                    .param(ADD_SHOP, Boolean.toString(addShop))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ),
            UserExpertiseDtoListWithHidMapping.class
        );
    }

    public List<UserExpertiseDto> getExpertiseList(long userId) {
        DtoList<UserExpertiseDto> result = parseValue(
            invokeAndRetrieveResponse(
                get("/expertise/UID/" + userId)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData();
    }

    public List<UserExpertiseMailDto> getUserExpertiseMail(long userId) {
        DtoList<UserExpertiseMailDto> result = parseValue(
            invokeAndRetrieveResponse(
                get("/expertise/UID/" + userId + "/info/mail")
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData();
    }

    public List<UserExpertiseDto> getExpertiseByHid(long hid, long... userIds) {
        DtoList<UserExpertiseDto> result = parseValue(
            invokeAndRetrieveResponse(
                get("/expertise/UID/by/hid/" + hid)
                    .param(UID_KEY, LongStream.of(userIds).mapToObj(Long::toString).toArray(String[]::new))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData();
    }

    public void updateExpertise(long userId,
                                AgitationType completed,
                                String entityId,
                                Integer hid) {
        updateExpertise(userId,
            completed.getEntity(),
            entityId,
            hid,
            Collections.singletonList(completed),
            null);
    }

    public void updateExpertise(long userId,
                                AgitationEntity entity,
                                String entityId,
                                Integer hid,
                                List<AgitationType> completed,
                                List<AgitationType> revoked) {
        completed = completed != null ? completed : Collections.emptyList();
        revoked = revoked != null ? revoked : Collections.emptyList();

        invokeAndRetrieveResponse(
            post("/expertise/UID/" + userId + "/update")
                .param(ENTITY_KEY, "" + entity.value())
                .param(ENTITY_ID_KEY, entityId)
                .param(HID_KEY, hid == null ? null : hid.toString())
                .param(AGIT_COMPLETED_KEY, toArrayStr(completed, AgitationType::value))
                .param(AGIT_REVOKED_KEY, toArrayStr(revoked, AgitationType::value))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public ExpertiseGainDto getExpertiseGain(long userId) {
        return FormatUtils.fromJson(getExpertiseGain(userId, status().is2xxSuccessful()), ExpertiseGainDto.class);
    }

    public String getExpertiseGain(long userId, ResultMatcher resultMatcher) {
        return invokeAndRetrieveResponse(
            post("/expertise/UID/" + userId + "/gain")
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

}
