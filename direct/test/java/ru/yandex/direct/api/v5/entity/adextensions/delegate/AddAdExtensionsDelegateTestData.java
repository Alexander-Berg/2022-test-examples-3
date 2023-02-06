package ru.yandex.direct.api.v5.entity.adextensions.delegate;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adextensions.AdExtensionAddItem;
import com.yandex.direct.api.v5.adextensions.AddRequest;
import com.yandex.direct.api.v5.general.ActionResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
class AddAdExtensionsDelegateTestData {

    static List<ActionResult> createActionResults() {
        int size = RandomUtils.nextInt(3, 10);
        return Stream.generate(ActionResult::new).limit(size).collect(toList());
    }

    static AddRequest createAddRequest() {
        final int size = RandomUtils.nextInt(4, 20);
        return createAddRequest(size);
    }

    static AddRequest createAddRequest(int size) {
        return new AddRequest().withAdExtensions(
                Stream.generate(() -> new AdExtensionAddItem().withCallout(callout()))
                        .limit(size)
                        .collect(toList()));
    }

    static List<Callout> createListOfCallouts(ClientId clientId) {
        final int size = RandomUtils.nextInt(4, 20);
        return createListOfCallouts(clientId, size);
    }

    static List<Callout> createListOfCallouts(ClientId clientId, int size) {
        return Stream.generate(() ->
                new Callout()
                        .withClientId(clientId.asLong())
                        .withId(randomLong()))
                .limit(size)
                .collect(toList());
    }

    @SuppressWarnings("unchecked")
    static MassResult<Long> successfulMassResult() {
        MassResult<Long> result = mock(MassResult.class);
        when(result.getErrorCount()).thenReturn(0);
        when(result.isSuccessful()).thenReturn(true);
        return result;
    }

    @SuppressWarnings("unchecked")
    static ApiMassResult<Long> successfulApiMassResult() {
        ApiMassResult<Long> result = mock(ApiMassResult.class);
        when(result.getErrorCount()).thenReturn(0);
        when(result.isSuccessful()).thenReturn(true);
        return result;
    }

    @SuppressWarnings("unchecked")
    static ApiMassResult<Long> brokenMassResult() {
        ApiMassResult<Long> result = mock(ApiMassResult.class);
        when(result.getErrorCount()).thenReturn(1);
        when(result.isSuccessful()).thenReturn(false);
        return result;
    }

    @SuppressWarnings("unchecked")
    static ApiMassResult<Long> anyMassResultOfLongs() {
        return any(ApiMassResult.class);
    }

    private static com.yandex.direct.api.v5.adextensiontypes.Callout callout() {
        int textLength = RandomUtils.nextInt(1, 15);
        return new com.yandex.direct.api.v5.adextensiontypes.Callout()
                .withCalloutText(RandomStringUtils.randomAlphabetic(textLength));
    }

    private static long randomLong() {
        return RandomUtils.nextLong(0, Long.MAX_VALUE);
    }

}
