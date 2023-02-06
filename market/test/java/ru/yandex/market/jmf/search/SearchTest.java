package ru.yandex.market.jmf.search;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InternalModuleSearchTestConfiguration.class)
public class SearchTest {

    private static final Fqn FQN = Fqn.of("testSearch");

    @Inject
    SearchService searchService;
    @Inject
    BcpService bcpService;
    @Inject
    EntityStorageService entityStorageService;

    @Test
    public void checkSearch_notExists() {
        createEntity();

        List<Metaclass> result = searchService.search(random());

        Assertions.assertEquals(0, result.size(), "Результат должен буть пустым т.к. нет объекта с искомым названием");
    }

    @Test
    public void checkSearch_exists() {
        String title = createEntity();

        List<Metaclass> result = searchService.search(title);

        Assertions.assertEquals(1, result.size(), "Должны найти объект т.к. создали его ранее");
        Assertions.assertEquals(FQN, result.get(0).getFqn());
    }

    /**
     * Проверяем особый кейс, когда в поисковой строке встречается спецсимвол '@', который используется в качестве
     * разделителя gid-а.
     */
    @Test
    public void check_specialCharacters() {
        String email = Randoms.email();
        String gid = Search.FQN.gidOf(email);

        Search entity = entityStorageService.get(gid);

        Assertions.assertEquals(gid, entity.getGid());
        Assertions.assertEquals(email, entity.getQuery());
    }

    /**
     * Проверяем кейс, когда в поисковой строке встречаются символы, которые обрезаются в ftsBody
     * (Например кавычки "елочки")
     */
    @ParameterizedTest
    @ValueSource(strings = {"ООО «Омега тул»", "«Омега тул»", "Омега тул", "Омега"})
    public void check_rejectedCharacters(String query) {
        createEntity("ООО «Омега тул»");
        Assertions.assertEquals(1, searchService.search(query).size());
    }

    String createEntity() {
        return createEntity(random());
    }

    private String createEntity(String title) {
        bcpService.create(FQN, Maps.of("title", title));
        return title;
    }

    private String random() {
        return Randoms.string().replace("-", "");
    }
}
