package ru.yandex.market.pers.author.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.expertise.AuthorSaasQueryService;
import ru.yandex.market.pers.author.expertise.ExpertiseControllerTest;
import ru.yandex.market.saas.search.SaasKvSearchRequest;
import ru.yandex.market.saas.search.SaasKvSearchService;
import ru.yandex.market.saas.search.SaasSearchException;
import ru.yandex.market.saas.search.response.SaasSearchDocument;
import ru.yandex.market.saas.search.response.SaasSearchResponse;
import ru.yandex.market.util.ExecUtils;
import ru.yandex.market.util.FormatUtils;
import ru.yandex.market.util.ListUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.author.expertise.AuthorSaasDataParser.ENTITY_ID_SAAS_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.06.2020
 */
@Service
public class PersAuthorSaasMocks {
    public static final int INDEX_AGE_DAYS = 5;

    @Autowired
    private SaasKvSearchService saasKvSearchService;

    public void mockExpertise(String expertise) {
        long timestampMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(INDEX_AGE_DAYS);
        String indexTime = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(timestampMs));
        mockExpertise(expertise, indexTime);
    }

    public void mockExpertise(String expertise, String indexTime) {
        this.mockExpertise(ExpertiseControllerTest.USER_ID, expertise, indexTime);
    }

    public void mockExpertise(long userId, String expertise, String indexTime) {
        Map<Long, String> resultMap = expertise == null ? Collections.emptyMap() : Map.of(userId, expertise);
        mockExpertise(resultMap, indexTime);
    }

    public void mockExpertise(Map<Long, String> expertiseMap, String indexTime) {
        mockSaasResponse(expertiseMap, Collections.emptyMap(), indexTime);
    }

    public void mockAgitation(AgitationUser user, Map<AgitationType, List<String>> agitations) {
        mockAgitation(user, agitations, null);
    }

    public void mockAgitationPlain(AgitationUser user, Map<String, String> agitations) {
        mockSaasResponse(Map.of(), Map.of(user, agitations), null);
    }

    public void mockAgitation(AgitationUser user, Map<AgitationType, List<String>> agitations, String indexTime) {
        mockSaasResponse(Collections.emptyMap(), Map.of(user, buildSimpleAgitations(agitations)), indexTime);
    }

    public void mockUserRoles(AgitationUser user, List<Map<String, String>> roles) {
        mockSaasResponse(Collections.emptyMap(), Map.of(user, Map.of()), roles, null);
    }

    private void mockSaasResponse(Map<Long, String> expertiseMap,
                                  Map<AgitationUser, Map<String, String>> agitations,
                                  String indexTime) {
        mockSaasResponse(expertiseMap, agitations, List.of(), indexTime);
    }

    private void mockSaasResponse(Map<Long, String> expertiseMap,
                                  Map<AgitationUser, Map<String, String>> agitations,
                                  List<Map<String,String>> roles,
                                  String indexTime) {
        String expIndexTimeMs = indexTime == null ? null : indexTime + "000";
        Map<AgitationUser, String> expertiseMapFull = ListUtils.toMap(expertiseMap.entrySet(),
            x -> AgitationUser.uid(x.getKey()),
            Map.Entry::getValue);

        List<AgitationUser> users = Stream.concat(
            expertiseMapFull.keySet().stream(),
            agitations.keySet().stream())
            .distinct()
            .collect(Collectors.toList());

        try {
            when(saasKvSearchService.search(argThat(argument -> {
                if (argument == null) {
                    return false;
                }
                // all requested keys are called
                Set<String> callSet = argument.getKeys();
                Set<String> expectedSet = ListUtils.toSet(users, AuthorSaasQueryService::saasKey);
                return callSet.size() == expectedSet.size() && callSet.containsAll(expectedSet);
            })))
                .then(invocation -> {
                    SaasSearchResponse response = mock(SaasSearchResponse.class);
                    List<SaasSearchDocument> documents = new ArrayList<>();
                    when(response.getDocuments()).thenReturn(documents);

                    users.forEach(user -> {
                        SaasSearchDocument document = mock(SaasSearchDocument.class);
                        when(document.getProperty("expertise")).thenReturn(expertiseMapFull.get(user));
                        when(document.getProperty(SaasKvSearchRequest.FIELD_IDX_TIMESTAMP)).thenReturn(indexTime);
                        when(document.getProperty("exp_index_time")).thenReturn(expIndexTimeMs);
                        when(document.getProperty("author_id")).thenReturn(user.getUserId());
                        when(document.getProperty("author_type")).thenReturn(String.valueOf(user.getType().getValue()));
                        when(document.getProperty("roles_data")).thenReturn(buildUserRoles(roles));
                        agitations.getOrDefault(user, Collections.emptyMap()).forEach((type, value) ->
                            when(document.getProperty(eq(type))).thenReturn(value));
                        documents.add(document);
                    });
                    return response;
                });
        } catch (SaasSearchException e) {
            throw ExecUtils.silentError(e);
        }
    }

    public static Map<String, String> buildSimpleAgitations(Map<AgitationType, List<String>> agitations) {
        if (agitations == null) {
            return Map.of();
        }
        HashMap<String, String> result = new HashMap<>();
        agitations.forEach((type, entityList) -> {
            result.put("agt" + type.value(),
                "[" + entityList.stream()
                    .map(entityId-> Map.of(ENTITY_ID_SAAS_KEY, entityId))
                    .map(FormatUtils::toJson)
                    .map(StringEscapeUtils::escapeJson)
                    .collect(Collectors.joining(",")) + "]"
            );
        });
        return result;
    }

    public static Map<String, String> buildAgitations(Map<AgitationType, List<Map<String, String>>> agitations) {
        if (agitations == null) {
            return Map.of();
        }
        HashMap<String, String> result = new HashMap<>();
        agitations.forEach((type, data) ->
            result.put("agt" + type.value(), StringEscapeUtils.escapeJson(FormatUtils.toJson(data))
            ));
        return result;
    }

    public static String buildUserRoles(List<Map<String,String>> roles) {
        return StringEscapeUtils.escapeJson(FormatUtils.toJson(roles));
    }

    public void mockSaasKvResponseEmpty() {
        try {
            when(saasKvSearchService.search(any(SaasKvSearchRequest.class)))
                .then(invocation -> {
                    SaasSearchResponse response = mock(SaasSearchResponse.class);
                    when(response.getDocuments()).thenReturn(Collections.emptyList());
                    return response;
                });
        } catch (SaasSearchException e) {
            throw ExecUtils.silentError(e);
        }
    }

    public String buildSaasExp(long value, long[] expIds) {
        return Arrays.stream(expIds)
            .mapToObj(x -> String.format("%d-%d", x, value))
            .collect(Collectors.joining("|"));
    }
}
