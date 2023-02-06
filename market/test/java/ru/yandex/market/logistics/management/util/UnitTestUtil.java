package ru.yandex.market.logistics.management.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.logistics.management.util.region.RegionTreeXmlBuilder;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class UnitTestUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModules(new Jdk8Module(), new JavaTimeModule());
    }

    public String readFile(String relativePath) {
        try {
            Path path = Paths.get(UnitTestUtil.class.getClassLoader().getResource(relativePath).toURI());

            try (Stream<String> lines = Files.lines(path)) {
                return lines.collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }

    public JsonNode stringToJsonNode(String s) {
        try {
            return MAPPER.reader().readTree(s);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create JsonNode from string " + s, e);
        }
    }

    public <T> String objectToPrettyString(T object) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot write to Json string object " + object, e);
        }
    }

    public Collector<JsonNode, ArrayNode, ArrayNode> toArrayNode() {
        return new Collector<>() {
            @Override
            public Supplier<ArrayNode> supplier() {
                return JsonNodeFactory.instance::arrayNode;
            }

            @Override
            public BiConsumer<ArrayNode, JsonNode> accumulator() {
                return ArrayNode::add;
            }

            @Override
            public BinaryOperator<ArrayNode> combiner() {
                return ArrayNode::addAll;
            }

            @Override
            public Function<ArrayNode, ArrayNode> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return Sets.immutableEnumSet(Collector.Characteristics.UNORDERED);
            }
        };
    }

    /**
     * Region tree used (from top)
     * 0 - 10 000 - 10 001 - 225
     * 225 - 17 - 10174 - 2
     * 225 - 3 - 1
     * 1 - 98 580
     * 1 - 213
     * 213 - 216
     * 213 - 20 279
     * 20 279 - 117 067
     * 20 279 - 117 066
     * 20 279 - 117 065
     * 117 065 - 20 481
     * 117 065 - 20 482
     * 117 066 - 20 478
     * 117 066 - 20 479
     */
    public RegionService getRegionTree() {
        RegionTreeXmlBuilder regionTreeXmlBuilder = new RegionTreeXmlBuilder();
        regionTreeXmlBuilder.setUrl(UnitTestUtil.class.getResource("/data/geobase/region-tree.xml"));
        regionTreeXmlBuilder.setSkipUnRootRegions(true);

        RegionService service = new RegionService();
        service.setRegionTreeBuilder(regionTreeXmlBuilder);
        regionTreeXmlBuilder.setSkipUnRootRegions(true);
        return service;
    }
}
