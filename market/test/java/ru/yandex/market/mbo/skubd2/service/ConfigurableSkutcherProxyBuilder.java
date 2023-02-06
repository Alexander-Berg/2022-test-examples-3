package ru.yandex.market.mbo.skubd2.service;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.mockito.Mockito;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.skubd2.knowledge.SkuKnowledge;
import ru.yandex.market.mbo.skubd2.load.dao.ParameterValue;
import ru.yandex.market.mbo.skubd2.service.dao.OfferInfo;
import ru.yandex.market.mbo.skubd2.service.dao.OfferResult;
import ru.yandex.market.mbo.skubd2.service.dao.Sku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author jkt on 27.07.18.
 */
public class ConfigurableSkutcherProxyBuilder {

    private CategorySkutcher categorySkutcherSpy;

    private List<ParameterValue> additionalSkuParameterValues = new ArrayList<>();
    private List<ParameterValue> extractedParameterValues = new ArrayList<>();

    private List<Long> clearIfAbsentInSkuParameters = new ArrayList<>();
    private List<Long> allSkuParameters = new ArrayList<>();

    private SkuBDApi.Status skutchingStatus = SkuBDApi.Status.OK;

    public ConfigurableSkutcherProxyBuilder(CategorySkutcher categorySkutcher) {
        this.categorySkutcherSpy = Mockito.spy(categorySkutcher);
    }

    public Skutcher buildDefaultSkutcher() {
        configureCategorySkutcher();
        return new DefaultSkutcher(configureSkuKnowledge());

    }

    public ConfigurableSkutcherProxyBuilder addParameterValuesToSkutchedSku(ParameterValue... parameterValues) {
        additionalSkuParameterValues.addAll(Stream.of(parameterValues).collect(Collectors.toList()));
        return this;
    }

    public ConfigurableSkutcherProxyBuilder addExtractedFromOfferParameterValuesToSkutchingResult(
        ParameterValue... parameterValues) {
        extractedParameterValues.addAll(Stream.of(parameterValues).collect(Collectors.toList()));
        return this;
    }

    public ConfigurableSkutcherProxyBuilder addClearIfAbsentInSkuParametersToConfiguration(Long... paramIds) {
        clearIfAbsentInSkuParameters.addAll(Stream.of(paramIds).collect(Collectors.toList()));
        return this;
    }

    public ConfigurableSkutcherProxyBuilder addSkuParametersToConfiguration(Long... paramIds) {
        allSkuParameters.addAll(Stream.of(paramIds).collect(Collectors.toList()));
        return this;
    }

    public ConfigurableSkutcherProxyBuilder makeCategorySkutcherNoSkutch() {
        skutchingStatus = SkuBDApi.Status.NO_SKU;
        return this;
    }

    public ConfigurableSkutcherProxyBuilder noneParameterClearsIfAbsentInSku() {
        clearIfAbsentInSkuParameters.clear();
        return this;
    }

    private void configureCategorySkutcher() {
        if (skutchingStatus != SkuBDApi.Status.OK) {
            configureCategorySkutcherThenNoSkutch();
            return;
        }

        Mockito.doReturn(new LongOpenHashSet(clearIfAbsentInSkuParameters))
            .when(categorySkutcherSpy).getClearIfAbsentInSkuParamIds();
        Mockito.doReturn(new LongOpenHashSet(allSkuParameters))
            .when(categorySkutcherSpy).getAllParamIds();

        Mockito.doAnswer(invocation -> {
            OfferResult realResult = (OfferResult) invocation.callRealMethod();
            return modifyOfferResult(realResult);
        }).when(categorySkutcherSpy).skutch(any(OfferInfo.class));
    }

    private void configureCategorySkutcherThenNoSkutch() {
        Mockito.doAnswer(invocation -> {
            return OfferResult.createCantSkutch(Collections.emptyList(),false);
        }).when(categorySkutcherSpy).skutch(any(OfferInfo.class));
    }

    private SkuKnowledge configureSkuKnowledge() {
        SkuKnowledge skuKnowledgeMock = Mockito.mock(SkuKnowledge.class);
        Mockito.when(skuKnowledgeMock.getSkutcherBySkuId(anyLong())).thenReturn(categorySkutcherSpy);
        Mockito.when(skuKnowledgeMock.getSkutcher(anyLong())).thenReturn(categorySkutcherSpy);
        return skuKnowledgeMock;
    }

    private Object modifyOfferResult(OfferResult realResult) {
        OfferResult modifiedResult = Mockito.spy(realResult);
        if (!extractedParameterValues.isEmpty()) {
            modifiedResult.getExtractedParamValues().addAll(extractedParameterValues);
        }
        Mockito.doReturn(modifySku(realResult.getSku())).when(modifiedResult).getSku();
        return modifiedResult;
    }

    private Sku modifySku(Sku sku) {
        Sku skuSpy = Mockito.spy(sku);
        Mockito.when(skuSpy.getParameterValues()).thenAnswer(
            invocation -> {
                if (additionalSkuParameterValues.isEmpty()) {
                    return invocation.callRealMethod();
                }

                Collection<ParameterValue> realParameters = (Collection<ParameterValue>) invocation.callRealMethod();
                ArrayList<ParameterValue> parameterValues = new ArrayList<>(realParameters);
                parameterValues.addAll(additionalSkuParameterValues);
                return parameterValues;
            }
        );
        return skuSpy;
    }
}
