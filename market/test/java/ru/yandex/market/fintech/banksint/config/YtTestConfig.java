package ru.yandex.market.fintech.banksint.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.stubs.YtTablesStub;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.fintech.banksint.util.JsonUtils;
import ru.yandex.market.fintech.banksint.yt.CategoryTreeCache;
import ru.yandex.market.fintech.banksint.yt.CategoryTreeYtRepository;
import ru.yandex.market.fintech.banksint.yt.ScoringDataYtRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Configuration
public class YtTestConfig {

    @Value("${yt.path.categories}")
    private String categoriesPath;

    @Bean
    public CategoryTreeYtRepository categoryTreeYtRepository() {

        Yt ytMock = createYtMock("yt/category_tree.json");

        return new CategoryTreeYtRepository(
                ytMock,
                categoriesPath
        );
    }

    @Bean
    public CategoryTreeCache categoryTreeCache() {
        return new CategoryTreeCache(categoryTreeYtRepository());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private Yt createYtMock(final String mockFilePath) {
        Yt ytMock = Mockito.mock(Yt.class);
        Cypress cypressMock = Mockito.mock(Cypress.class);
        YtTables ytTablesMock = new YtTablesStub() {
            @Override
            public <T> void read(YPath path, YTableEntryType<T> entryType, Consumer<T> consumer) {
                assertThat(path).isEqualTo(YPath.simple(categoriesPath));
                assertThat(entryType).isEqualTo(YTableEntryTypes.YSON);
                Consumer<YTreeMapNode> jsonNodeConsumer = (Consumer<YTreeMapNode>) consumer;
                try {
                    List<JsonNode> nodeList = JsonUtils.getJsonMapper()
                            .readValue(
                                    ScoringDataYtRepository.class.getResourceAsStream("category_tree.json"),
                                    new TypeReference<List<JsonNode>>() {
                                    });
                    nodeList
                            .stream()
                            .map(node -> {
                                var ytNode = new YTreeMapNodeImpl(new EmptyMap());
                                ytNode.put("hid", new YTreeIntegerNodeImpl(false, node.get("hid").asLong(),
                                        Collections.emptyMap()));
                                ytNode.put("parent_hid", new YTreeIntegerNodeImpl(false,
                                        node.get("parent_hid").asLong(), Collections.emptyMap()));
                                ytNode.put("leaf", new YTreeBooleanNodeImpl(node.get("leaf").asBoolean(),
                                        new EmptyMap()));
                                ytNode.put("published",
                                        new YTreeBooleanNodeImpl(node.get("published").asBoolean(),
                                                new EmptyMap()));
                                ytNode.put("name", new YTreeStringNodeImpl(node.get("name").asText(),
                                        Collections.emptyMap()));
                                ytNode.put("unique_name",
                                        new YTreeStringNodeImpl(node.get("unique_name").asText(),
                                                Collections.emptyMap()));
                                return ytNode;
                            })
                            .forEach(jsonNodeConsumer);
                } catch (IOException e) {
                    fail("Couldn't read mock data", e);
                }
            }
        };


        when(ytMock.tables()).thenReturn(ytTablesMock);
        when(ytMock.cypress()).thenReturn(cypressMock);

        when(cypressMock.exists(eq(YPath.simple(categoriesPath)))).thenReturn(true);
        return ytMock;
    }
}
