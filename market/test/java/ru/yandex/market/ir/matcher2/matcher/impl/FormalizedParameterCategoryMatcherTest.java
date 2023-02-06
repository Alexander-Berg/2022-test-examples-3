package ru.yandex.market.ir.matcher2.matcher.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.ir.parser.matcher.tokenizers.StringValuesTokenizer;
import ru.yandex.ir.parser.matcher.tokenizers.TokenizeHelper;
import ru.yandex.ir.parser.matcher.tokenizers.TokenizedAlias;
import ru.yandex.market.ir.matcher2.matcher.MatchType;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Category;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.CategoryKnowledge;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Model;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Modification;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.ModificationParent;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Vendor;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.dao.CategoryEntity;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.dao.FormalizedParamEntity;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.dao.FormalizedParamValue;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.dao.ModelEntity;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.dao.ModificationEntity;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.dao.ParamValuesHolder;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.dao.VendorEntity;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.AhoUtils;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.CategoryHierarchy;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.FormalizedParameterCategoryMatcher;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Level;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Match;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.StringTokenizationFactory;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.Tokenization;
import ru.yandex.market.ir.matcher2.matcher.alternate.load.ParametrizedCategoryLoader;
import ru.yandex.market.ir.matcher2.matcher.be.OfferCopy;
import ru.yandex.market.ir.matcher2.matcher.be.formalized.ParamBag;
import ru.yandex.market.ir.matcher2.matcher.util.MapToSet;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.utils.string.ahotokens2.AhoTokensMachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author a-shar.
 */
public class FormalizedParameterCategoryMatcherTest {

    private static final int CATEGORY_ID = 432460;
    private static final int MIN_PRICE = 125;
    private static final int MAX_PRICE = 15000;
    private static final int HID = 91013;
    private static final int LOCAL_VENDOR_ID = 487306;
    private static final int GLOBAL_VENDOR_ID = 152863;
    private static final int MODEL_ID = 511717;
    private static final int MODIFICATION_ID1 = 463187;
    private static final int MODIFICATION_ID2 = 46315675;
    private static final int MODIFICATION_ID3 = 46315676;
    private static final int BLOCK_LOCAL_VENDOR_ID = 487306;
    private static final int BLOCK_GLOBAL_VENDOR_ID = 152863;

    private FormalizedParameterCategoryMatcher formalizedParameterCategoryMatcher;

    private static CategoryEntity createCategoryEntity() {
        CategoryEntity categoryEntity = new CategoryEntity(CATEGORY_ID, "Ноутбуки");
        categoryEntity.setMinPrice(MIN_PRICE);
        categoryEntity.setMaxPrice(MAX_PRICE);
        categoryEntity.setHyperCategoryId(HID);
        categoryEntity.getLinkedTovarCategoryIds().addAll(Arrays.asList(1, 2));
        categoryEntity.setCutOffWords(ImmutableSet.of("Card Reader Transcend", "STOP WORD", "DVD плеер"));
        categoryEntity.setParamMatcherUsed(true);
        categoryEntity.setMatcherUsesDescription(true);
        categoryEntity.setMatcherUsesYmlParams(false);
        categoryEntity.setMatcherUsesBarcode(false);
        categoryEntity.setAliasIntersectionsAllowed(true);
        VendorEntity asusVendor = categoryEntity.addNewVendor(LOCAL_VENDOR_ID, GLOBAL_VENDOR_ID, "ASUS", true, true);
        asusVendor.setCutOffWords(ImmutableSet.of("HP", "Чижик"));
        ModelEntity modelEntity = asusVendor.addNewModel(MODEL_ID, "S300N", true, false, false, false);
        modelEntity.setAliases(Sets.newHashSet("С300Н", "S200N"));
        modelEntity.addFormalizedParamEntity(new FormalizedParamEntity(FormalizedParameterCategoryMatcher.VENDOR_PARAMETER_ID,
            MboParameters.ParamMatchingType.SPLITTER,
            MboParameters.ValueType.ENUM,
            new FormalizedParamValue(152863, 0, null)));

        ModificationEntity modificationEntity = modelEntity.addModification(MODIFICATION_ID1,
            "S300N (Pentium M 723 1000 Mhz/8.9&quot;/1024x600/512Mb/30.0Gb/DVD/CD-RW/Wi-Fi/WinXP Home)",
            -1, true, false, false, false);
        modificationEntity.setAliases(Sets.newHashSet("S300N (Pentium M 723 1000 Mhz/8.9&quot;/1024x600/512Mb/30.0Gb/DVD/CD-RW/Wi-Fi)"));

        modificationEntity.addFormalizedParamEntity(new FormalizedParamEntity(14202862,
            MboParameters.ParamMatchingType.SUPER,
            MboParameters.ValueType.STRING,
            new FormalizedParamValue(0, 0, ImmutableSet.of("123456789"))));

        modificationEntity = modelEntity.addModification(MODIFICATION_ID2,
            "S300N NEXT",
            -1, true, false, false, false);
        modificationEntity.setAliases(Sets.newHashSet("S300N NEXT"));
        modificationEntity.addFormalizedParamEntity(new FormalizedParamEntity(FormalizedParameterCategoryMatcher.VENDOR_PARAMETER_ID,
            MboParameters.ParamMatchingType.SPLITTER,
            MboParameters.ValueType.ENUM,
            new FormalizedParamValue(152863, 0, null)));
        modificationEntity.addFormalizedParamEntity(new FormalizedParamEntity(14202862,
            MboParameters.ParamMatchingType.SUPER,
            MboParameters.ValueType.STRING,
            new FormalizedParamValue(0, 0, ImmutableSet.of("123456789"))));
        modificationEntity.addFormalizedParamEntity(new FormalizedParamEntity(123,
            MboParameters.ParamMatchingType.NOT_MANDATORY,
            MboParameters.ValueType.ENUM,
            new FormalizedParamValue(321, 0, null)));


        modificationEntity = modelEntity.addModification(MODIFICATION_ID3,
            "SU-30",
            -1, true, false, false, false);
        modificationEntity.setAliases(Sets.newHashSet("SU-30"));
        modificationEntity.addFormalizedParamEntity(new FormalizedParamEntity(123,
            MboParameters.ParamMatchingType.NOT_MANDATORY,
            MboParameters.ValueType.ENUM,
            new FormalizedParamValue(321, 0, null)));
        modificationEntity.addFormalizedParamEntity(new FormalizedParamEntity(121,
            MboParameters.ParamMatchingType.NOT_MANDATORY,
            MboParameters.ValueType.ENUM,
            new FormalizedParamValue(323, 0, null)));

        return categoryEntity;
    }

    @Before
    public void init() {

        CategoryEntity categoryEntity = createCategoryEntity();
        ParametrizedCategoryLoader.ModelCache modelCache = new ParametrizedCategoryLoader.ModelCache();

        Category category = new Category(categoryEntity);
        List<ParamValuesHolder> paramValuesHolders =
            ParametrizedCategoryLoader.getParamValuesHolders(categoryEntity);
        MapToSet<ModificationParent, Modification> modificationsHierarchy
            = ParametrizedCategoryLoader.extractModifications(categoryEntity.getVendors(), modelCache);
        MapToSet<Vendor, Model> vendorModels
            = ParametrizedCategoryLoader.extractVendorModels(categoryEntity.getVendors(), modelCache);
        Set<List<String>> blockWords = ParametrizedCategoryLoader.tokenizeBlockWords(
            category.getGuruId(), categoryEntity.getCutOffWords(), true
        );

        CategoryHierarchy categoryHierarchy = new CategoryHierarchy(vendorModels, modificationsHierarchy);

        MapToSet<ModificationParent, TokenizedAlias> modelAliases =
            extractModelAliases(categoryEntity.getVendors(), ImmutableSetMultimap.of());
        CategoryKnowledge categoryKnowledge = CategoryKnowledge.newBuilder()
            .setBlockWords(blockWords)
            .setVendorAliases(extractVendorAliases(categoryEntity.getVendors()))
            .setVendorBlockWords(createVendorBlockWords())
            .setVendorModels(extractVendorModels(categoryEntity.getVendors()))
            .setModificationsHierarchy(modificationsHierarchy)
            .setModelAliases(modelAliases)
            .setModelBlockWords(MapToSet.newInstance())
            .setCategory(category)
            .setCategoryHierarchy(categoryHierarchy)
            .setModelsIndex(buildModelsIndex(modelAliases.keySet()))
            .build();
        formalizedParameterCategoryMatcher = FormalizedParameterCategoryMatcher
            .newBuilder(categoryKnowledge, paramValuesHolders)
            .build();
    }

    private MapToSet<Vendor, TokenizedAlias> createVendorBlockWords() {
        return extractVendorBlockWords(new Vendor(BLOCK_LOCAL_VENDOR_ID, BLOCK_GLOBAL_VENDOR_ID, ""),
            ImmutableSet.of("Reader", "Lifebook", "600"));
    }

    private static MapToSet<Vendor, Model> extractVendorModels(List<VendorEntity> vendors) {
        MapToSet<Vendor, Model> result = MapToSet.newInstance();
        for (VendorEntity vendorEntity : vendors) {
            List<ModelEntity> modelEntities = vendorEntity.getModels();
            if (modelEntities == null) {
                continue;
            }
            Vendor vendor = new Vendor(vendorEntity);
            for (ModelEntity modelEntity : modelEntities) {
                result.put(vendor, new Model(modelEntity));
            }
        }
        return result;
    }

    private static Int2ObjectOpenHashMap<ModificationParent> buildModelsIndex(Set<ModificationParent> models) {
        final Int2ObjectOpenHashMap<ModificationParent> index = new Int2ObjectOpenHashMap<>();
        for (ModificationParent model : models) {
            index.put(model.getId(), model);
        }
        index.trim();

        return index;
    }

    @Test
    public void test_categoryBlockWord() {
        OfferCopy offer = OfferCopy.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setTitle("LIFEBOOK S7110 (Core 2 Duo 1660Mhz/14.0&amp;quot;/1024Mb/60.0Gb/DVD-RW) Stop Word")
            .build();
        List<Match> matches = formalizedParameterCategoryMatcher.multiMatch(offer, null);
        assertEquals(1, matches.size());
        Match match = formalizedParameterCategoryMatcher.match(offer, null);
        assertEquals(MatchType.BLOCK_WORD_MATCH, match.getMatchedType());
    }

    @Test
    public void test_superParameterSingleModel() {
        OfferCopy offer = OfferCopy.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setTitle("123456789") // barcode
            .build();
        List<Match> matches = formalizedParameterCategoryMatcher.multiMatch(offer, null);
        assertEquals(2, matches.size());
        Match match = formalizedParameterCategoryMatcher.match(offer, null);
        assertEquals(MatchType.SUPER_PARAM_MATCH, match.getMatchedType());
    }

    @Test
    public void test_splitterParamMatching() {
        ParamBag paramBag = createVendorParamBag();
        paramBag.addOptionValue(123, 321);
        OfferCopy.Builder builder = OfferCopy.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setTitle("trash 123456789 text")
            .setParamBag(paramBag);
        OfferCopy offer = builder.build();
        List<Match> matches = formalizedParameterCategoryMatcher.multiMatch(offer, null);
        assertEquals(2, matches.size());
        Match match = formalizedParameterCategoryMatcher.match(offer, null);
        assertEquals(3, match.getHierarchy().length);
        assertEquals(MatchType.FORMALIZED_PARAM_MATCH, match.getMatchedType());

        builder = OfferCopy.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setTitle("trash text")
            .setParamBag(createVendorParamBag());

        OfferCopy offerWithoutSuperParam = builder.build();
        matches = formalizedParameterCategoryMatcher.multiMatch(offerWithoutSuperParam, null);
        assertEquals(0, matches.size());
        match = formalizedParameterCategoryMatcher.match(offerWithoutSuperParam, null);
        assertNull(match);
    }

    @Test
    public void test_AliasMatching() {
        ParamBag paramBag = createVendorParamBag();
        OfferCopy offer = OfferCopy.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setTitle("S200N S300N NEXT")
            .setParamBag(paramBag)
            .build();
        List<Match> matches = formalizedParameterCategoryMatcher.multiMatch(offer, null);
        assertEquals(2, matches.size());
        Match match = formalizedParameterCategoryMatcher.match(offer, null);
        assertEquals(MatchType.MODIFICATION_MATCH, match.getMatchedType());
    }

    @Test
    public void test_OptionalMatching() {
        ParamBag paramBag = createVendorParamBag();

        paramBag.addOptionValue(123, 321);

        OfferCopy offer = OfferCopy.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setTitle("S0N NEXT")
            .setParamBag(paramBag)
            .build();
        List<Match> matches = formalizedParameterCategoryMatcher.multiMatch(offer, null);
        assertEquals(2, matches.size());
        Match match = formalizedParameterCategoryMatcher.match(offer, null);
        assertEquals(MatchType.FORMALIZED_PARAM_MATCH, match.getMatchedType());
        assertEquals(2, getLowestLevel(match.getHierarchy()).getParamEntries().length);
    }

    private Level getLowestLevel(Level[] hierarchy) {
        Level result = Level.EMPTY_VENDOR_LEVEL;
        if (hierarchy != null && hierarchy.length > 0) {
            result = hierarchy[hierarchy.length - 1];
        }
        return result;
    }

    @Test
    public void test2() {

        OfferCopy.Builder builder = OfferCopy.newBuilder();
        builder.setCategoryId(CATEGORY_ID)
            .setTitle("11B-02JE600");

        StringTokenizationFactory stringTokenizationFactory = new StringTokenizationFactory(true, true, true, true);
        Tokenization tokenization = stringTokenizationFactory.getTokenization(builder.build());
        MapToSet<Vendor, TokenizedAlias> vendorAliasMap = createVendorBlockWords();


        AhoTokensMachine<String, Vendor> vendorAntiMachine
            = AhoUtils.buildMachineByTokenizedAliases(vendorAliasMap.keySet(), vendorAliasMap);

        IntSet vendorBlockingIds = new IntOpenHashSet();
        tokenization.apply((baseInOfferEntry, tokens) ->
            AhoUtils.searchAliasedPositions(tokens.getTokens(), vendorAntiMachine,
                (endTokenIndex, tokensCount, value) -> {
                    System.out.println(value.getId());
                    System.out.println(tokensCount);
                    System.out.println(endTokenIndex);
                    vendorBlockingIds.add(value.getId());
                }));
        vendorBlockingIds.forEach(System.out::println);
    }

    private static MapToSet<Vendor, TokenizedAlias> extractVendorBlockWords(Vendor vendor,
                                                                                     Set<String> cutOffWords) {
        MapToSet<Vendor, TokenizedAlias> result = MapToSet.newInstance();
        putAliasesSet(cutOffWords, vendor, result);
        return result;
    }

    private static void putAliasesSet(Set<String> aliases, Vendor vendor,
                                      MapToSet<Vendor, TokenizedAlias> result) {
        if (null != aliases) {
            for (String alias : aliases) {
                List<String> tokenizedAlias = StringValuesTokenizer.tokenizeAndSingletonize(alias);
                if (tokenizedAlias.size() > 0) {
                    final List<List<String>> variations = StringValuesTokenizer.applySpaceRemoving(tokenizedAlias);
                    result.putCollection(vendor, TokenizeHelper.convertToTokenizedAliases(variations));
                }
            }
        }
    }

    private static MapToSet<Vendor, TokenizedAlias> extractVendorAliases(List<VendorEntity> vendors) {
        MapToSet<Vendor, TokenizedAlias> result = MapToSet.newInstance();
        for (VendorEntity vendorEntity : vendors) {
            final Vendor vendor = new Vendor(vendorEntity);
            ParametrizedCategoryLoader.addRawAlias(result, vendor, vendorEntity.getName(), null);
            putAliasesSetVA(vendorEntity.getVendorAliases(), vendor, result);
            putAliasesSetVA(vendorEntity.getGlobalVendorAliases(), vendor, result);
        }
        result.trim();
        return result;
    }

    private static void putAliasesSetVA(Set<String> aliases, Vendor vendor,
                                        MapToSet<Vendor, TokenizedAlias> result) {
        if (null != aliases) {
            for (String alias : aliases) {
                ParametrizedCategoryLoader.addRawAlias(result, vendor, alias, null);
            }
        }
    }

    private static MapToSet<ModificationParent, TokenizedAlias> extractModelAliases(
        List<VendorEntity> vendors, SetMultimap<Integer, String> modelToBarcode) {
        MapToSet<ModificationParent, TokenizedAlias> result = MapToSet.newInstance();
        for (VendorEntity vendor : vendors) {
            List<ModelEntity> modelEntities = vendor.getModels();
            if (null != modelEntities) {
                for (ModelEntity modelEntity : modelEntities) {
                    final Model model = new Model(modelEntity);
                    //addOptionValue barcodes to aliases
                    Set<String> barcodes = modelToBarcode.get(model.getModelId());
                    if (modelEntity.getAliases() != null) {
                        modelEntity.getAliases().addAll(barcodes);
                    }

                    putAliasesSet(modelEntity.getAliases(), model, result);
                    List<ModificationEntity> modificationEntities = modelEntity.getModifications();
                    if (null != modificationEntities) {
                        for (ModificationEntity modificationEntity : modificationEntities) {
                            final Modification modification = new Modification(modificationEntity);

                            Set<String> modificationBarcodes = modelToBarcode.get(modification.getModificationId());
                            if (modificationEntity.getAliases() != null) {
                                modificationEntity.getAliases().addAll(modificationBarcodes);
                            }
                            putAliasesSet(modificationEntity.getAliases(), modification, result);
                        }
                    }
                }
            }
        }
        result.trim();
        return result;
    }

    private static void putAliasesSet(Set<String> aliases, ModificationParent model,
                                      MapToSet<ModificationParent, TokenizedAlias> result) {
        result.putCollection(model, Collections.emptyList());
        if (null != aliases) {
            for (String alias : aliases) {
                ParametrizedCategoryLoader.addRawAlias(result, model, alias, null);
            }
        }
    }

    private static ParamBag createVendorParamBag() {
        ParamBag paramBag = new ParamBag(1);
        paramBag.addOptionValue(FormalizedParameterCategoryMatcher.VENDOR_PARAMETER_ID, 152863);
        return paramBag;
    }
}
