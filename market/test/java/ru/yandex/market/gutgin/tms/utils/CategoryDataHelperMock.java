package ru.yandex.market.gutgin.tms.utils;

import Market.DataCamp.DataCampContentMarketParameterValue;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;

import static ru.yandex.market.partner.content.common.BaseDBStateGenerator.CATEGORY;
import static ru.yandex.market.partner.content.common.BaseDBStateGenerator.CATEGORY_ID;


public class CategoryDataHelperMock extends CategoryDataHelper {

    public CategoryDataHelperMock() {
        super(null, null);
    }

    @Override
    public String getParameterNameForPartner(
            Long categoryId, DataCampContentMarketParameterValue.MarketParameterValue parameterValue
    ) {
        return parameterValue.getParamName();
    }

    @Override
    public CategoryData getCategoryData(long categoryId) {
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.addCategoryData(CATEGORY_ID, CategoryData.build(CATEGORY));
        return categoryDataKnowledge.getCategoryData(CATEGORY_ID);
    }

}
