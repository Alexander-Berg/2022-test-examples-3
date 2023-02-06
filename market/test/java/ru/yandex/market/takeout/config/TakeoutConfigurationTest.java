package ru.yandex.market.takeout.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;

public class TakeoutConfigurationTest {

    private static Takeout20Configuration takeout20Configuration;

    @BeforeClass
    public static void setup() throws Exception {
        InputStream inputStream =

                new FileInputStream(Paths.getSourcePath("market/mstat/market-takeout/src/main/resources/config/takeout-20-services.yaml"));
        takeout20Configuration = Takeout20ConfigurationLoader.loadConfiguration(inputStream);
    }

    @Test
    public void everyTakeoutHasTypeRemap() {

        Map<String, TakeoutDescription> takeouts = takeout20Configuration.getTakeouts();

        for (Map.Entry<String, TakeoutDescription> singleTakeoutDescription : takeouts.entrySet()) {
            if (singleTakeoutDescription.getValue() == null) {
                continue;
            }
            Assert.assertNotNull(String.join("/", "takeouts", singleTakeoutDescription.getKey(), "typeRemap"),
                    singleTakeoutDescription.getValue().getTypeRemap());
        }
    }

    @Test
    public void everyTakeoutHasModuleName() {

        Map<String, TakeoutDescription> takeouts = takeout20Configuration.getTakeouts();

        for (Map.Entry<String, TakeoutDescription> singleTakeoutDescription : takeouts.entrySet()) {
            if (singleTakeoutDescription.getValue() == null) {
                continue;
            }
            Assert.assertNotNull(String.join("/", "takeouts", singleTakeoutDescription.getKey(), "moduleName"),
                    singleTakeoutDescription.getValue().getModuleName());
        }
    }

    @Test
    public void thereNoNullTakeoutDescriptions() {

        Map<String, TakeoutDescription> takeouts = takeout20Configuration.getTakeouts();

        for (Map.Entry<String, TakeoutDescription> singleTakeoutDescription : takeouts.entrySet()) {
            Assert.assertNotNull(String.join("/", "takeouts", singleTakeoutDescription.getKey()),
                    singleTakeoutDescription.getValue());
        }
    }

    @Test
    public void thereIsNoTakeoutsWithRemapToNullPublicType() {

        Map<String, TakeoutDescription> takeouts = takeout20Configuration.getTakeouts();

        for (Map.Entry<String, TakeoutDescription> singleTakeoutDescription : takeouts.entrySet()) {
            if (singleTakeoutDescription.getValue() != null && singleTakeoutDescription.getValue().getTypeRemap() != null) {
                for (Map.Entry<String, String> singleRemap :
                        singleTakeoutDescription.getValue().getTypeRemap().entrySet()) {
                    Assert.assertNotNull(
                            String.join("/", "takeouts", singleTakeoutDescription.getKey(), "typeRemap",
                                    singleRemap.getKey()),
                            singleRemap.getValue());
                }
            }
        }
    }

    @Test
    public void thereIsNoNullTypes() {
        Map<String, TypeDescription> types = takeout20Configuration.getTypes();
        for (Map.Entry<String, TypeDescription> type : types.entrySet()) {
            Assert.assertNotNull(String.join("/", "types", type.getKey()), type.getValue());
        }
    }

    @Test
    public void everyTakeoutRemapTypeIsInTypes() {

        HashSet<String> publicTypes = getTakeoutPublicTypes();

        for (String publicType : publicTypes) {
            Assert.assertNotNull(String.join("/", "types", publicType),
                    takeout20Configuration.getTypes().get(publicType));
        }
    }

    @Test
    public void everyTakeoutRemapTypeHasBackPath() {

        HashSet<String> publicTypes = getTakeoutPublicTypes();

        for (String publicType : publicTypes) {
            if (takeout20Configuration.getTypes().get(publicType) != null) {
                Assert.assertNotNull(String.join("/", "types", publicType, "takeouts"),
                        takeout20Configuration.getTypes().get(publicType).getTakeouts());
            }
        }
    }

    @Test
    public void allPublicTypeTypesCanReachTakeout() {

        Map<String, TakeoutDescription> takeouts = takeout20Configuration.getTakeouts();
        Map<String, TypeDescription> types = takeout20Configuration.getTypes();
        for (String publicType : types.keySet()) {
            if (types.get(publicType) != null && types.get(publicType).getTakeouts() != null) {
                for (Map.Entry<String, Set<String>> takeoutType :
                        types.get(publicType).getTakeouts().entrySet()) {

                    Assert.assertNotNull(String.join("/", "takeouts", takeoutType.getKey()), takeouts.get(takeoutType.getKey()));
                }
            }
        }
    }

    @Test
    public void takeoutPublicTypesCanReachTakeout() {

        Map<String, TypeDescription> types = takeout20Configuration.getTypes();
        Map<String, TakeoutDescription> takeouts = takeout20Configuration.getTakeouts();
        for (String publicType : getTakeoutPublicTypes()) {
            if (types.get(publicType) != null && types.get(publicType).getTakeouts() != null) {
                for (Map.Entry<String, Set<String>> takeoutType :
                        types.get(publicType).getTakeouts().entrySet()) {

                    if (takeouts.get(takeoutType.getKey()) != null && takeouts.get(takeoutType.getKey()).getTypeRemap() != null){
                        Set<String> actual = takeouts.get(takeoutType.getKey()).getTypeRemap().keySet();
                        MatcherAssert.assertThat(String.join("/", "takeout", takeoutType.getKey(), "typeRemap"),
                                actual,
                                CoreMatchers.hasItems(takeoutType.getValue().toArray(new String[0])));
                        String[] strings = takeouts.get(takeoutType.getKey()).getTypeRemap()
                                .entrySet()
                                .stream().filter(s -> s.getValue().equals(publicType))
                                .map(stringStringEntry -> stringStringEntry.getKey())
                                .toArray(String[]::new);
                        MatcherAssert.assertThat(String.join("/", "types", "takeouts", takeoutType.getKey()),
                                takeoutType.getValue(),
                                CoreMatchers.hasItems(strings));
                    }
                }
            }
        }
    }

    @NotNull
    private HashSet<String> getTakeoutPublicTypes() {
        Map<String, TakeoutDescription> takeouts = takeout20Configuration.getTakeouts();

        HashSet<String> publicTypes = new HashSet<>();

        for (Map.Entry<String, TakeoutDescription> singleTakeoutDescription : takeouts.entrySet()) {
            if (singleTakeoutDescription.getValue() != null && singleTakeoutDescription.getValue().getTypeRemap() != null) {
                for (String publicType : singleTakeoutDescription.getValue().getTypeRemap().values()) {
                    if (publicType != null) {
                        publicTypes.add(publicType);
                    }
                }
            }
        }
        return publicTypes;
    }
}
