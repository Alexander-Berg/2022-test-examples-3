package ru.yandex.market.mboc.common.services.category_info.knowledges;

import ru.yandex.market.mbo.lightmapper.test.EmptyGenericMapperRepositoryMock;

/**
 * @author s-ermakov
 */
public class CategoryKnowledgeRepositoryMock
    extends EmptyGenericMapperRepositoryMock<CategoryKnowledge, Long> implements CategoryKnowledgeRepository {

    public CategoryKnowledgeRepositoryMock() {
        super(CategoryKnowledge::getCategoryId);
    }
}
