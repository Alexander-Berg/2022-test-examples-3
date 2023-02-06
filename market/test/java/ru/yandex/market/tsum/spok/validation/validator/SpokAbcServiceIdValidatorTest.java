package ru.yandex.market.tsum.spok.validation.validator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.mockito.stubbing.Answer1;

import ru.yandex.market.request.netty.WrongStatusCodeException;
import ru.yandex.market.tsum.clients.abc.AbcApiClient;
import ru.yandex.market.tsum.clients.abc.models.AbcService;
import ru.yandex.market.tsum.clients.mdb.MdbClient;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.registry.proto.model.JavaTemplate;
import ru.yandex.market.tsum.spok.validation.model.SpokValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.stubbing.answers.AnswerFunctionalInterfaces.toAnswer;
import static ru.yandex.market.tsum.spok.validation.validator.SpokAbcServiceIdValidator.ABC_GROUP_SHOULD_BELONG_TO_SERVICE_ABC_GROUP;
import static ru.yandex.market.tsum.spok.validation.validator.SpokAbcServiceIdValidator.COULD_NOT_FIND_MDB_FOLDER;
import static ru.yandex.market.tsum.spok.validation.validator.SpokAbcServiceIdValidator.FIELD_NAME;
import static ru.yandex.market.tsum.spok.validation.validator.SpokAbcServiceIdValidator.MISSING_REQUIRED_PARAMETER;
import static ru.yandex.market.tsum.spok.validation.validator.SpokAbcServiceIdValidator.SERVICE_NOT_FOUND;
import static ru.yandex.market.tsum.spok.validation.validator.SpokAbcServiceIdValidator.SERVICE_NOT_FOUND_WITH_SUGGESTION;

@ParametersAreNonnullByDefault
public class SpokAbcServiceIdValidatorTest {
    private static final AbcService ABC_SERVICE_WITH_MDB_FOLDER_PRESENT = new AbcService()
            .withId(1)
            .withParent(3, "someparent")
            .withSlug("existingservice");

    private static final AbcService ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT = new AbcService()
            .withId(2)
            .withParent(3, "someparent")
            .withSlug("existingservice/nomdb");

    private static final List<AbcService> EXISTING_ABC_SERVICES = List.of(
            ABC_SERVICE_WITH_MDB_FOLDER_PRESENT,
            ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT);

    private static final Map<String, AbcService> ABC_SERVICES_BY_ID = EXISTING_ABC_SERVICES.stream()
            .collect(Collectors.toMap(
                    service -> Integer.toString(service.getId()),
                    Function.identity()));

    private static final Map<String, AbcService> ABC_SERVICES_BY_SLUG = EXISTING_ABC_SERVICES.stream()
            .collect(Collectors.toMap(AbcService::getSlug, Function.identity()));

    private static final Set<String> EXISTING_MDB_FOLDERS = Set.of(
            ABC_SERVICE_WITH_MDB_FOLDER_PRESENT.getSlug());

    private static final int NON_EXISTING_SERVICE_ID = 3;
    private static final String NON_EXISTING_SERVICE_SLUG = "bogusservice";

    private static final AbcApiClient ABC_API_CLIENT = mock(AbcApiClient.class);

    static {
        when(ABC_API_CLIENT.getAbcServicesBySlugExact(anyString()))
                .thenAnswer(toAnswer((String slug) ->
                        ABC_SERVICES_BY_SLUG.containsKey(slug) ?
                                Stream.of(ABC_SERVICES_BY_SLUG.get(slug)) :
                                Stream.empty()));

        when(ABC_API_CLIENT.getAbcServiceById(anyString()))
                .thenAnswer(toAnswer((String id) -> {
                    if (!ABC_SERVICES_BY_ID.containsKey(id)) {
                        throw new WrongStatusCodeException(404, "http://localhost", "payload");
                    }
                    return ABC_SERVICES_BY_ID.get(id);
                }));
    }

    private static final MdbClient MDB_CLIENT = mock(MdbClient.class);

    static {
        when(MDB_CLIENT.folderExists(anyString()))
                .then(toAnswer((Answer1<Boolean, String>) EXISTING_MDB_FOLDERS::contains));
    }

    private static final SpokValidator VALIDATOR =
            new SpokAbcServiceIdValidator(ABC_API_CLIENT, MDB_CLIENT);

    @Test
    public void idIsNull() {
        ServiceParams params = paramsWithoutMdb(null, ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT.getParent().getSlug());
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.error(FIELD_NAME,
                String.format(MISSING_REQUIRED_PARAMETER, FIELD_NAME)));
    }

    @Test
    public void bogusServiceNonNumericId() {
        String abcServiceId = NON_EXISTING_SERVICE_SLUG;
        ServiceParams params = paramsWithoutMdb(abcServiceId,
                ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT.getParent().getSlug());
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.error(FIELD_NAME,
                String.format(SERVICE_NOT_FOUND, abcServiceId)));
    }

    @Test
    public void bogusServiceValidNumericId() {
        String abcServiceId = Integer.toString(ABC_SERVICE_WITH_MDB_FOLDER_PRESENT.getId());
        ServiceParams params = paramsWithoutMdb(abcServiceId,
                ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT.getParent().getSlug());
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.error(FIELD_NAME,
                String.format(SERVICE_NOT_FOUND_WITH_SUGGESTION, abcServiceId,
                        ABC_SERVICE_WITH_MDB_FOLDER_PRESENT.getSlug())));
    }

    @Test
    public void bogusServiceInvalidNumericId() {
        String abcServiceId = Integer.toString(NON_EXISTING_SERVICE_ID);
        ServiceParams params = paramsWithoutMdb(abcServiceId,
                ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT.getParent().getSlug());
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.error(FIELD_NAME,
                String.format(SERVICE_NOT_FOUND, abcServiceId)));
    }

    @Test
    public void existingServiceNoMdbRequested() {
        String abcServiceId = ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT.getSlug();
        ServiceParams params = paramsWithoutMdb(abcServiceId,
                ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT.getParent().getSlug());
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.ok());
    }

    @Test
    public void existingServiceWithMdbFolder() {
        String abcServiceId = ABC_SERVICE_WITH_MDB_FOLDER_PRESENT.getSlug();
        ServiceParams params = paramsWithMdb(abcServiceId, ABC_SERVICE_WITH_MDB_FOLDER_PRESENT.getParent().getSlug());
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.ok());
    }

    @Test
    public void existingServiceWithoutMdbFolder() {
        String abcServiceId = ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT.getSlug();
        ServiceParams params = paramsWithMdb(abcServiceId, ABC_SERVICE_WITH_MDB_FOLDER_PRESENT.getParent().getSlug());
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.error(FIELD_NAME,
                String.format(COULD_NOT_FIND_MDB_FOLDER, abcServiceId)));
    }

    @Test
    public void existingServiceWithInvalidParent() {
        String customAbcSlug = "custom_abc_slug";
        ServiceParams params = paramsWithoutMdb(ABC_SERVICE_WITHOUT_MDB_FOLDER_PRESENT.getSlug(),
                customAbcSlug);
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.error(FIELD_NAME,
                String.format(ABC_GROUP_SHOULD_BELONG_TO_SERVICE_ABC_GROUP, customAbcSlug)));
    }

    private ServiceParams paramsWithoutMdb(@Nullable String abcServiceId, String parentServiceAbcSlug) {
        ServiceParams params = new ServiceParams();
        params.setUseExistingAbcService(true);
        params.setAbcSlug(abcServiceId);
        params.setParentAbcSlug(parentServiceAbcSlug);
        return params;
    }

    private ServiceParams paramsWithMdb(@Nullable String abcServiceId, String parentServiceAbcSlug) {
        ServiceParams params = new ServiceParams();
        params.setUseExistingAbcService(true);
        params.setJavaAppTemplate(JavaTemplate.BAZINGA_TMS_WITH_MONGO);
        params.setAbcSlug(abcServiceId);
        params.setParentAbcSlug(parentServiceAbcSlug);
        return params;
    }
}
