package client;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import api.CombinatorApi;
import dto.requests.checkouter.RearrFactor;
import dto.responses.combinator.Ycombo;
import dto.responses.combinator.YcomboParameters;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath({"delivery/combinator.properties"})
@Slf4j
public class CombinatorClient {

    private final CombinatorApi combinatorApi;

    @Property("combinator.host")
    private String host;

    public CombinatorClient() {
        PropertyLoader.newInstance().populate(this);
        combinatorApi = RETROFIT.getRetrofit(host).create(CombinatorApi.class);
    }

    @SneakyThrows
    public Ycombo ycombo(YcomboParameters params) {
        log.debug("Calling ycombo to get paths...");

        String finalExpString = params.getExperiment()
            .stream()
            .map(RearrFactor::getValue)
            .collect(Collectors.joining(";"));
        Response<Ycombo> ycomboResponse = combinatorApi.getPaths(
            params.getWarehouse(),
            params.getRegion(),
            params.getWeight(),
            serializeDimensions(params.getDimensions()),
            params.getLatitude(),
            params.getLongitude(),
            finalExpString,
            "json"
        ).execute();
        Assertions.assertTrue(ycomboResponse.isSuccessful(), "Неудачный запрос в ycombo комбинатора");
        Assertions.assertNotNull(ycomboResponse.body(), "Не удалось получить содержимое запроса ycombo комбинатора");

        return ycomboResponse.body();
    }

    @Nullable
    private String serializeDimensions(@Nullable Integer[] dimensions) {
        return Optional.ofNullable(dimensions)
            .map(
                d -> Arrays.stream(d)
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining("x"))
            )
            .orElse(null);
    }
}
