package ru.yandex.market.mbo.skubd2.onetime;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.ir.io.proto.ProtoCategoryReader;
import ru.yandex.ir.io.proto.ProtoModelReader;
import ru.yandex.market.ir.FormalizerContractor;
import ru.yandex.market.ir.dao.CategoryCreator;
import ru.yandex.market.ir.decision.RuleFactory;
import ru.yandex.market.ir.parser.view.automat.StateFactory;
import ru.yandex.market.ir.processor.FormalizationStringIndexer;
import ru.yandex.market.ir.processor.PatternEntriesExtractor;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.skubd2.knowledge.SkuRuntimeKnowledge;
import ru.yandex.market.mbo.skubd2.service.CategorySkutcher;
import ru.yandex.utils.string.DeNullingProcessor;
import ru.yandex.utils.string.indexed.IndexedStringFactory;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class Loader {
    private static final String categoryFileName = "/home/a-shar/work/skutcher/parameters_1003092.pb.gz";
    private static final String modelFileName = "/home/a-shar/work/skutcher/sku_1003092.pb.gz";

    @Test
    @Ignore()
    public void testLoadCategory() throws IOException {
        MboParameters.Category categoryEntity = ProtoCategoryReader.loadProtoCategoryDump(categoryFileName);
        Stream<ModelStorage.Model> modelStream = ProtoModelReader.createModelStream(modelFileName);

        long hid = categoryEntity.getHid();

        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());
        indexedStringFactory.setSteam(true);
        FormalizationStringIndexer stringIndexer = new FormalizationStringIndexer();
        stringIndexer.setIndexedStringFactory(indexedStringFactory);
        CategoryCreator categoryCreator = new CategoryCreator();
        categoryCreator.setIndexedStringFactory(indexedStringFactory);
        categoryCreator.setStateFactory(new StateFactory());
        categoryCreator.setRuleFactory(new RuleFactory());

        PatternEntriesExtractor patternEntriesExtractor = new PatternEntriesExtractor();
        patternEntriesExtractor.setIndexedStringFactory(indexedStringFactory);

        FormalizerContractor formalizerContractor = new FormalizerContractor();
        formalizerContractor.setPatternEntriesExtractor(patternEntriesExtractor);

        SkuRuntimeKnowledge runtimeKnowledge = new SkuRuntimeKnowledge(formalizerContractor,
            indexedStringFactory,
            stringIndexer, categoryCreator
        );
        runtimeKnowledge.fullyReloadCategory(hid, categoryEntity, modelStream);
        CategorySkutcher categorySkutcher = runtimeKnowledge.getSkutcherByHid(hid);
        System.out.println(categorySkutcher.getHid());
    }

}
