package ru.yandex.market.ocrm.module.order;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.external.report.ReportClient;
import ru.yandex.market.crm.external.report.SkuSpecification.SpecificationFeature;
import ru.yandex.market.crm.external.report.SkuSpecification.SpecificationGroup;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.ocrm.module.order.domain.SkuSpecification;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class SkuSpecificationTest {

    @Inject
    private EntityStorageService entityStorageService;

    @Inject
    private ReportClient reportClient;

    @Test
    public void testList() {
        String sku = Randoms.string();
        List<SpecificationGroup> groups = generateReportResponse();
        Mockito.when(reportClient.getSkuSpecification(sku)).thenReturn(groups);
        Query query = Query.of(SkuSpecification.FQN)
                .withFilters(Filters.eq(SkuSpecification.SKU, sku));

        List<SkuSpecification> specs = entityStorageService.list(query);

        Assertions.assertNotNull(specs);
        Assertions.assertEquals(4, specs.size());

        assertSkuSpecificationImpl(specs.get(0), groups.get(0), groups.get(0).getFeatures().get(0), sku);
        assertSkuSpecificationImpl(specs.get(1), groups.get(0), groups.get(0).getFeatures().get(1), sku);
        assertSkuSpecificationImpl(specs.get(2), groups.get(1), groups.get(1).getFeatures().get(0), sku);
        assertSkuSpecificationImpl(specs.get(3), groups.get(1), groups.get(1).getFeatures().get(1), sku);
    }

    private List<SpecificationGroup> generateReportResponse() {
        return List.of(generateSpecificationGroup(), generateSpecificationGroup());
    }

    private SpecificationGroup generateSpecificationGroup() {
        SpecificationGroup group = new SpecificationGroup();
        group.setName(Randoms.string());
        group.addFeature(generateSpecificationFeature());
        group.addFeature(generateSpecificationFeature());
        return group;
    }

    private SpecificationFeature generateSpecificationFeature() {
        SpecificationFeature feature = new SpecificationFeature();
        feature.setName(Randoms.string());
        feature.setValue(Randoms.string());
        return feature;
    }

    private void assertSkuSpecificationImpl(SkuSpecification spec,
                                            SpecificationGroup group,
                                            SpecificationFeature feature,
                                            String sku) {
        Assertions.assertEquals(group.getName(), spec.getGroup());
        Assertions.assertEquals(feature.getName(), spec.getName());
        Assertions.assertEquals(feature.getValue(), spec.getValue());
        Assertions.assertEquals(sku, spec.getSku());
    }

}
