package ru.yandex.market.pers.author.mock.mvc;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.author.agitation.model.Agitation;
import ru.yandex.market.pers.author.client.api.dto.AgitationAddRequest;
import ru.yandex.market.pers.author.agitation.model.AgitationPreviewDto;
import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoListWithPriority;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.client.api.model.AgitationCancelReason;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.DELAY_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.DURATION_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.ENTITY_ID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.FORCE_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.PAGE_NUM_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.PAGE_SIZE_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.REASON_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.TYPE_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 23.06.2020
 */
@Primary
@Service
public class AgitationMvcMocks extends AbstractMvcMocks {
    private final String prefix;

    public AgitationMvcMocks() {
        this("");
    }

    public AgitationMvcMocks(String prefix) {
        this.prefix = prefix;
    }

    public List<Agitation> getPopup(AgitationUser user, AgitationType... types) {
        return getPopupDto(user, types).getData();
    }

    public DtoListWithPriority<Agitation> getPopupDto(AgitationUser user, AgitationType... types) {
        DtoListWithPriority<Agitation> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "/popup")
                    .param(TYPE_KEY, toArrayStr(List.of(types), AgitationType::value))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result;
    }

    public List<Agitation> getTasks(AgitationUser user, AgitationType... types) {
        DtoListWithPriority<Agitation> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "/tasks")
                    .param(TYPE_KEY, toArrayStr(List.of(types), AgitationType::value))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });
        return result.getData();
    }

    public List<Agitation> getExisted(AgitationUser user, AgitationType type, String... entityId) {
        DtoListWithPriority<Agitation> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "/existed")
                    .param(TYPE_KEY, String.valueOf(type.value()))
                    .param(ENTITY_ID_KEY, entityId)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });
        return result.getData();
    }

    public void limitPopup(AgitationUser user, String duration) {
        invokeAndRetrieveResponse(
            post("/agitation/" + getUserPath(user) + "/popup/limit")
                .param(DURATION_KEY, duration)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void enablePopup(AgitationUser user) {
        invokeAndRetrieveResponse(
            delete("/agitation/" + getUserPath(user) + "/popup/limit")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public List<AgitationPreviewDto> getPreview(AgitationUser user, int pageSize, AgitationType... types) {
        DtoList<AgitationPreviewDto> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "/preview")
                    .param(TYPE_KEY, toArrayStr(List.of(types), AgitationType::value))
                    .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData();
    }

    public DtoPager.Pager getPagePager(AgitationUser user, AgitationType type, int pageNum, int pageSize) {
        return getPageDto(user, type, pageNum, pageSize).getPager();
    }

    public List<Agitation> getPage(AgitationUser user, AgitationType type, int pageNum, int pageSize) {
        return getPageDto(user, type, pageNum, pageSize).getData();
    }

    public DtoPager<Agitation> getPageDto(AgitationUser user, AgitationType type, int pageNum, int pageSize) {
        DtoPager<Agitation> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "")
                    .param(TYPE_KEY, String.valueOf(type.value()))
                    .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                    .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result;
    }

    public List<Agitation> getPageUgcTasks(AgitationUser user, AgitationType type, int pageNum, int pageSize) {
        return getPageUgcTasksDto(user, type, pageNum, pageSize).getData();
    }

    public DtoPager<Agitation> getPageUgcTasksDto(AgitationUser user, AgitationType type, int pageNum, int pageSize) {
        DtoPager<Agitation> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "/ugc/tasks")
                    .param(TYPE_KEY, String.valueOf(type.value()))
                    .param(PAGE_NUM_KEY, String.valueOf(pageNum))
                    .param(PAGE_SIZE_KEY, String.valueOf(pageSize))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result;
    }

    public Agitation getStatusFast(AgitationUser user, AgitationType type, String entityId) {
        DtoList<Agitation> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "/status/fast")
                    .param(TYPE_KEY, String.valueOf(type.value()))
                    .param(ENTITY_ID_KEY, entityId)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData().isEmpty() ? null : result.getData().get(0);
    }

    public Agitation getStatus(AgitationUser user, AgitationType type, String entityId) {
        DtoList<Agitation> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "/status")
                    .param(TYPE_KEY, String.valueOf(type.value()))
                    .param(ENTITY_ID_KEY, entityId)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData().isEmpty() ? null : result.getData().get(0);
    }

    public void complete(AgitationUser user, String agitationId) {
        invokeAndRetrieveResponse(
            post("/agitation/" + getUserPath(user) + "/agitation/" + agitationId + "/complete")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void reset(AgitationUser user, String agitationId) {
        invokeAndRetrieveResponse(
            post("/agitation/" + getUserPath(user) + "/agitation/" + agitationId + "/reset")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void cancel(AgitationUser user, String agitationId, AgitationCancelReason reason) {
        cancel(user, agitationId, reason, status().is2xxSuccessful());
    }

    public void cancel(AgitationUser user,
                       String agitationId,
                       AgitationCancelReason reason,
                       ResultMatcher resultMatcher) {
        invokeAndRetrieveResponse(
            post("/agitation/" + getUserPath(user) + "/agitation/" + agitationId + "/cancel")
                .param(REASON_KEY, reason == null ? null : String.valueOf(reason.getValue()))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public void delay(AgitationUser user, String agitationId, Duration duration, ResultMatcher resultMatcher) {
        invokeAndRetrieveResponse(
            post("/agitation/" + getUserPath(user) + "/agitation/" + agitationId + "/delay")
                .param(DURATION_KEY, duration != null ? duration.toString() : null)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public void delay(AgitationUser user, String agitationId, Duration duration) {
        delay(user, agitationId, duration, status().is2xxSuccessful());
    }

    public void addAgitation(AgitationUser user, AgitationType type, String entityId, boolean force) {
        addAgitation(user, type, entityId, force, null, null);
    }

    public void addAgitation(AgitationUser user,
                             AgitationType type,
                             String entityId,
                             boolean force,
                             Duration delay,
                             Duration duration) {
        addAgitation(user, type, entityId, force, delay, duration, null, status().is2xxSuccessful());
    }

    public void addAgitation(AgitationUser user,
                             AgitationType type,
                             String entityId,
                             boolean force,
                             Duration delay,
                             Duration duration,
                             ResultMatcher resultMatcher) {
        addAgitation(user, type, entityId, force, delay, duration, null, resultMatcher);
    }

    public void addAgitation(AgitationUser user,
                             AgitationType type,
                             String entityId,
                             boolean force,
                             Duration delay,
                             Duration duration,
                             Map<String, String> data,
                             ResultMatcher resultMatcher) {
        MockHttpServletRequestBuilder request = post("/agitation/" + getUserPath(user))
            .param(TYPE_KEY, String.valueOf(type.value()))
            .param(ENTITY_ID_KEY, entityId)
            .param(FORCE_KEY, String.valueOf(force))
            .param(DELAY_KEY, delay != null ? delay.toString() : null)
            .param(DURATION_KEY, duration != null ? duration.toString() : null)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
        if (data != null) {
            request.content(FormatUtils.toJson(new AgitationAddRequest(data)));
        }
        invokeAndRetrieveResponse(request, resultMatcher);
    }

    public String getUserPath(AgitationUser user) {
        switch (user.getType()) {
            case UID:
                return prefix + "UID/" + user.getUserId();
            case YANDEXUID:
                return prefix + "YANDEXUID/" + user.getUserId();
            default:
                throw new IllegalArgumentException("invalid user type" + user.getType());
        }
    }
}
