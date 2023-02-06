package ru.yandex.direct.model.generator.old.javafile;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.model.generator.old.conf.AttrConf;
import ru.yandex.direct.model.generator.old.conf.ModelConf;
import ru.yandex.direct.model.generator.old.conf.ModelConfFactory;
import ru.yandex.direct.model.generator.old.conf.UpperLevelModelConf;
import ru.yandex.direct.model.generator.old.registry.ModelConfRegistry;
import ru.yandex.direct.model.generator.old.spec.JavaFileSpec;
import ru.yandex.direct.model.generator.old.spec.factory.JavaFileSpecFactory;

import static com.google.common.base.Functions.identity;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class GenerateAttrsTest {

    private static final String SIMPLE_BANNER_NAME = "SimpleBanner";
    private static final String BANNER_WITH_STATUS_NAME = "BannerWithStatus";
    private static final String NOT_SO_SIMPLE_BANNER_NAME = "NotSoSimpleBanner";
    private static final String BASE_BANNER_INTERFACE_NAME = "BaseBannerInterface";
    private static final String TEXT_BANNER_INTERFACE_NAME = "TextBannerInterface";

    private static final String ABSTRACT_BANNER_RESOURCE_NAME = "examples/attrconf/abstract_banner.conf";
    private static final String BASE_BANNER_RESOURCE_NAME = "examples/attrconf/base_banner.conf";
    private static final String TEXT_BANNER_RESOURCE_NAME = "examples/attrconf/text_banner.conf";
    private static final String STANDALONE_BANNER_INTERFACE_RESOURCE_NAME = "examples/attrconf/i_banner_simple.conf";

    private static UpperLevelModelConf abstractBannerConf;
    private static UpperLevelModelConf baseBannerConf;
    private static UpperLevelModelConf textBannerConf;
    private static UpperLevelModelConf standaloneInterfaceConf;

    private static ModelConf simpleBannerConf;
    private static ModelConf bannerWithStatus;
    private static ModelConf notSoSimpleBannerConf;
    private static ModelConf baseBannerInterfaceConf;
    private static ModelConf textBannerInterfaceConf;

    private static ModelConfFactory modelConfFactory;

    private static Map<String, JavaFileSpec> specMap;

    @BeforeClass
    public static void beforeClass() throws Exception {
        modelConfFactory = new ModelConfFactory();

        abstractBannerConf = createConf(ABSTRACT_BANNER_RESOURCE_NAME);
        baseBannerConf = createConf(BASE_BANNER_RESOURCE_NAME);
        textBannerConf = createConf(TEXT_BANNER_RESOURCE_NAME);
        standaloneInterfaceConf = createConf(STANDALONE_BANNER_INTERFACE_RESOURCE_NAME);

        ModelConfRegistry modelConfRegistry = new ModelConfRegistry(
                asList(abstractBannerConf, baseBannerConf, textBannerConf, standaloneInterfaceConf));
        Map<String, ModelConf> modelConfMap = listToMap(modelConfRegistry.getAllModelConfigs(), ModelConf::getName);
        specMap = buildTypeSpecs(modelConfRegistry);

        simpleBannerConf = modelConfMap.get(SIMPLE_BANNER_NAME);
        bannerWithStatus = modelConfMap.get(BANNER_WITH_STATUS_NAME);
        notSoSimpleBannerConf = modelConfMap.get(NOT_SO_SIMPLE_BANNER_NAME);
        baseBannerInterfaceConf = modelConfMap.get(BASE_BANNER_INTERFACE_NAME);
        textBannerInterfaceConf = modelConfMap.get(TEXT_BANNER_INTERFACE_NAME);
    }

    private static UpperLevelModelConf createConf(String resourceName) {
        URL resource = ClassLoader.getSystemResource(resourceName);
        return modelConfFactory.createModelConf(resource);
    }

    @Test
    public void abstractBannerClassHasAttrs() {
        JavaFileSpec spec = specMap.get(abstractBannerConf.getName());
        checkClassSpecAttrs(spec, abstractBannerConf.getAttrs(), emptyList());
    }

    @Test
    public void baseBannerClassHasAllInheritedAttrs() {
        JavaFileSpec spec = specMap.get(baseBannerConf.getName());
        checkClassSpecAttrs(spec, baseBannerConf.getAttrs(), abstractBannerConf.getAttrs());
    }

    @Test
    public void textBannerClassHasAllInheritedAttrs() {
        JavaFileSpec spec = specMap.get(textBannerConf.getName());

        Set<AttrConf> expectedInheritedAttrs = new HashSet<>(abstractBannerConf.getAttrs());
        expectedInheritedAttrs.addAll(baseBannerConf.getAttrs());
        checkClassSpecAttrs(spec, textBannerConf.getAttrs(), expectedInheritedAttrs);
    }

    @Test
    public void simpleBannerInterfaceHasAttrs() {
        JavaFileSpec spec = specMap.get(SIMPLE_BANNER_NAME);

        checkInterfaceSpecAttrs(spec, simpleBannerConf.getAttrNames(), emptyList());
    }

    @Test
    public void notSoSimpleBannerInterfaceHasAttrs() {
        JavaFileSpec spec = specMap.get(NOT_SO_SIMPLE_BANNER_NAME);

        checkInterfaceSpecAttrs(spec, notSoSimpleBannerConf.getAttrNames(), simpleBannerConf.getAttrNames());
    }

    @Test
    public void baseBannerInterfaceHasAttrs() {
        JavaFileSpec spec = specMap.get(BASE_BANNER_INTERFACE_NAME);

        Set<String> expectedInheritedAttrs = new HashSet<>(simpleBannerConf.getAttrNames());
        expectedInheritedAttrs.addAll(notSoSimpleBannerConf.getAttrNames());
        checkInterfaceSpecAttrs(spec, baseBannerInterfaceConf.getAttrNames(), expectedInheritedAttrs);
    }

    @Test
    public void textBannerInterfaceHasAttrs() {
        JavaFileSpec spec = specMap.get(TEXT_BANNER_INTERFACE_NAME);

        Set<String> expectedInheritedAttrs = new HashSet<>(simpleBannerConf.getAttrNames());
        expectedInheritedAttrs.addAll(notSoSimpleBannerConf.getAttrNames());
        expectedInheritedAttrs.addAll(baseBannerInterfaceConf.getAttrNames());
        //удаляем существующие аттрибуты
        expectedInheritedAttrs.removeAll(textBannerInterfaceConf.getAttrNames());

        checkInterfaceSpecAttrs(spec, textBannerInterfaceConf.getAttrNames(), expectedInheritedAttrs);
    }

    @Test
    public void standAloneInterfaceHasAllInheritedAttrs() {
        JavaFileSpec spec = specMap.get(standaloneInterfaceConf.getName());

        Set<String> expectedInheritedAttrs = new HashSet<>(simpleBannerConf.getAttrNames());
        expectedInheritedAttrs.addAll(notSoSimpleBannerConf.getAttrNames());
        expectedInheritedAttrs.addAll(baseBannerInterfaceConf.getAttrNames());
        expectedInheritedAttrs.addAll(bannerWithStatus.getAttrNames());

        checkInterfaceSpecAttrs(spec, standaloneInterfaceConf.getAttrNames(), expectedInheritedAttrs);
    }

    private void checkClassSpecAttrs(JavaFileSpec spec, Collection<AttrConf> expectedAttrs,
                                     Collection<AttrConf> expectedInheritedAttrs) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(spec.getAttrs()).containsOnlyElementsOf(expectedAttrs);
        soft.assertThat(spec.getInheritedAttributes()).containsOnlyElementsOf(expectedInheritedAttrs);
        soft.assertAll();
    }

    private void checkInterfaceSpecAttrs(JavaFileSpec spec, Collection<String> expectedAttrNames,
                                         Collection<String> expectedInheritedAttrNames) {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(mapList(spec.getAttrs(), AttrConf::getName)).containsOnlyElementsOf(expectedAttrNames);
        soft.assertThat(mapList(spec.getInheritedAttributes(), AttrConf::getName))
                .containsOnlyElementsOf(expectedInheritedAttrNames);
        soft.assertAll();
    }

    private static Map<String, JavaFileSpec> buildTypeSpecs(ModelConfRegistry modelConfRegistry) {
        JavaFileSpecFactory javaFileSpecFactory = new JavaFileSpecFactory(modelConfRegistry);
        return StreamEx.of(javaFileSpecFactory.convertAllConfigsToSpecs())
                .toMap(JavaFileSpec::getName, identity());
    }
}
