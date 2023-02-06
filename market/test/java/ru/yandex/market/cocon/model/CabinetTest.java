package ru.yandex.market.cocon.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.cocon.ArcanumService;
import ru.yandex.market.cocon.CabinetServiceImpl;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.cocon.config.CabinetLoaderConfig.ARC_RESOURCE_PATTERN;
import static ru.yandex.market.cocon.config.CabinetLoaderConfig.RESOURCE_PATTERN;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class CabinetTest {

    private ObjectMapper mapper = new ObjectMapper(); //factory.createJsonMapper();
    private ResourceLoader resourceLoader = new DefaultResourceLoader();
    private CabinetServiceImpl cabinetService;
    private RestTemplate arcanumRestTemplate = Mockito.mock(RestTemplate.class);
    private String arcanumUrl = "http://arc";
    private String arcanumToken = "token";
    private ArcanumService arcanumService = new ArcanumService(arcanumRestTemplate, arcanumUrl, arcanumToken);

    static Stream<Arguments> args() {
        return Stream.of(CabinetType.values()).map(Arguments::of);
    }

    static Stream<Arguments> argsWithCommit() {
        return Stream.of(
                Arguments.of(CabinetType.SHOP, "6a6e96e0f8ba0fbf6e5e6c3b77efef87b73ccb6a"),
                Arguments.of(CabinetType.SUPPLIER, "6a6e96e0f8ba0fbf6e5e6c3b77efef87b73ccb6a")
        );
    }

    @BeforeEach
    void setUp() {
        cabinetService =
                new CabinetServiceImpl(mapper, resourceLoader, RESOURCE_PATTERN, ARC_RESOURCE_PATTERN, arcanumService);
    }

    @DisplayName("Проверяем сериализацию/десериализацию params из страницы конфига")
    @Test
    void checkPageParams() throws IOException {
        try (InputStream inputStream =
                     this.getClass().getResourceAsStream("../security/testcab.json")) {
            String json = StringTestUtil.getString(Objects.requireNonNull(inputStream));
            Cabinet cabinet = mapper.readerFor(Cabinet.class).readValue(json);
            Optional<Page> page =
                    cabinet.getPages().stream()
                            .filter(p -> Objects.equals(p.getName(), "testPage1"))
                            .findFirst();
            Map<String, String> expected = Map.of("testPageParam1", "param-pam",
                    "testPageParam2", "param-pam-pam");
            assertEquals(expected, page.get().getParams());

            String actual = mapper.writeValueAsString(cabinet);
            JsonTestUtil.assertEquals(json, actual);
        }
    }

    @DisplayName("Сериализация туда-сюда даёт стабильный результат")
    @Test
    void test() throws IOException {
        try (InputStream inputStream = CabinetTest.class.getClassLoader()
                .getResourceAsStream("cabinets/delivery.json")) {
            String json = StringTestUtil.getString(Objects.requireNonNull(inputStream));
            Cabinet cabinet = mapper.readerFor(Cabinet.class).readValue(json);
            // заодно проверим конструктор копирования
            cabinet = new Cabinet(cabinet);
            String role = cabinet.getPages().stream()
                    .filter(p -> Objects.equals(p.getName(), "market-partner:html:platform-register:get"))
                    .map(Page::getFeatures)
                    .flatMap(Collection::stream)
                    .filter(f -> Objects.equals(f.getName(), "canViewRegisterLink"))
                    .map(Securable::getRoles)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(SecurityRule::getItems)
                    .flatMap(Collection::stream)
                    .findFirst()
                    .orElse(null);

            assertEquals("NOT_AUTHENTICATED", role);

            String actual = mapper.writeValueAsString(cabinet);
            JsonTestUtil.assertEquals(json, actual);
        }
    }

    @ParameterizedTest
    @MethodSource("args")
    void test1(CabinetType type) throws IOException {
        Cabinet cabinet = cabinetService.getCabinet(type);
        String actual = mapper.writeValueAsString(cabinet);
        String json = readJson(type);
        JsonTestUtil.assertEquals(json, actual);
    }

    @ParameterizedTest
    @MethodSource("argsWithCommit")
    void testByCommit(CabinetType type, String revision) throws IOException {

        String json = readJson(type);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Authorization", "OAuth " + arcanumToken);
        Mockito.when(arcanumRestTemplate.exchange(
                        arcanumUrl + "/v2/repos/arc/blobs?commit_id={commit_id}&at={at}&path={path}&fields=content",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class,
                        revision, revision, String.format(ARC_RESOURCE_PATTERN, type.getId())))
                .thenReturn(new ResponseEntity<>(
                        "{ \"data\": {\"content\": \"" + StringEscapeUtils.escapeJava(json) + "\"}}", HttpStatus.OK));

        Cabinet cabinet = cabinetService.getCabinet(type, revision);
        String actual = mapper.writeValueAsString(cabinet);
        JsonTestUtil.assertEquals(json, actual);
    }

    private String readJson(CabinetType type) {
        try (InputStream is = resourceLoader.getResource(
                String.format(RESOURCE_PATTERN, type.getId() + ".json")).getInputStream()) {
            return StringTestUtil.getString(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
