package ru.yandex.market.crm.core.test.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.platform.api.GetFactsRequest;
import ru.yandex.market.crm.platform.api.GetFactsRequest.Fact;
import ru.yandex.market.crm.platform.api.User;
import ru.yandex.market.crm.platform.api.UsersResponse;
import ru.yandex.market.crm.platform.commons.Response;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.profiles.Facts;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.ResponseBuilder;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

import static ru.yandex.market.mcrm.http.HttpRequest.get;
import static ru.yandex.market.mcrm.http.HttpRequest.post;

/**
 * @author apershukov
 */
@Component
public class PlatformHelper implements StatefulHelper {

    private final Map<String, Multimap<Uid, Object>> facts;

    private final HttpEnvironment httpEnvironment;
    private final String platformUrl;

    public PlatformHelper(HttpEnvironment httpEnvironment,
                          @Value("${external.crmPlatform.url}") String platformUrl) {
        this.httpEnvironment = httpEnvironment;
        this.platformUrl = platformUrl;
        this.facts = new HashMap<>();
    }

    public PlatformHelper putFact(String factId, Uid uid, Object fact) {
        facts.computeIfAbsent(
                factId,
                k -> Multimaps.newMultimap(new HashMap<>(), ArrayList::new)
        ).put(uid, fact);

        return this;
    }

    @Override
    public void setUp() {
        httpEnvironment.when(post(platformUrl + "/facts"))
                .then(request -> {
                    GetFactsRequest body = GetFactsRequest.parseFrom(request.getBody());

                    Facts.Builder factsBuilder = Facts.newBuilder();

                    Set<Uid> uids = Sets.newHashSet(body.getUidList());

                    for (Fact fact : body.getFactsList()) {
                        factsBuilder.getDescriptorForType().getFields().stream()
                                .filter(descriptor -> descriptor.getName().equalsIgnoreCase(fact.getConfig()))
                                .findFirst()
                                .ifPresent(descriptor -> {
                                    Multimap<Uid, Object> ofType = facts.get(fact.getConfig());

                                    if (ofType != null) {
                                        List<?> toSet = ofType.entries().stream()
                                                .filter(e -> uids.contains(e.getKey()))
                                                .map(Map.Entry::getValue)
                                                .collect(Collectors.toList());

                                        factsBuilder.setField(descriptor, toSet);
                                    }
                                });
                    }

                    Response response = Response.newBuilder()
                            .setFacts(factsBuilder)
                            .build();

                    return ResponseBuilder.newBuilder()
                            .body(response.toByteArray())
                            .build();
                });
    }

    public void prepareUser(UidType idType, String idValue, @Nullable User user) {
        UsersResponse response = user == null
                ? UsersResponse.getDefaultInstance()
                : UsersResponse.newBuilder()
                        .addUser(user)
                        .build();

        httpEnvironment.when(
                get(platformUrl + "/users/" + idType.name().toLowerCase() + "/" + idValue)
        ).then(
                ResponseBuilder.newBuilder()
                    .body(response.toByteArray())
                    .build()
        );
    }

    @Override
    public void tearDown() {
        facts.clear();
    }
}
