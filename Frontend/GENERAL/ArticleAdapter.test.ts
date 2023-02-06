import assert from 'assert';
import { IFullArticle } from 'store/articles/types';
import { Saas } from 'apphost';
import { transform } from './ArticleAdapter';

const createFakeResponse = (): Saas.IResponse => ({
    TotalDocCount: [100500],
    DebugInfo: {
        BaseSearchCount: 1,
        AnswerIsComplete: true,
        BaseSearchNotRespondCount: 0,
    },
    ErrorInfo: {
        GotError: 2,
    },
    Grouping: [{
        NumDocs: [100500],
        Group: [
            {
                CategoryName: Saas.CategoryNames.articles,
                RelevStat: [1],
                Document: [{
                    ArchiveInfo: {
                        GtaRelatedAttribute: [
                            {
                                Key: Saas.ArticleGtaAttributeKey.title,
                                Value: 'Как вам такой заголовок статьи?',
                            },
                            {
                                Key: Saas.ArticleGtaAttributeKey.imageUrl,
                                Value: 'https://mhealth.ru/pictures.jpg',
                            },
                        ],
                        Url: '12345',
                    },
                }],
            },
            {
                CategoryName: Saas.CategoryNames.articles,
                RelevStat: [1],
                Document: [{
                    ArchiveInfo: {
                        GtaRelatedAttribute: [
                            {
                                Key: Saas.ArticleGtaAttributeKey.title,
                                Value: 'Интересная статья?',
                            },
                            {
                                Key: Saas.ArticleGtaAttributeKey.imageUrl,
                                Value: 'https://whealth.ru/pictures.jpg',
                            },
                        ],
                        Url: '6789',
                    },
                }],
            },
        ],
    }],
});

const expectedArticles: IFullArticle[] = [
    {
        article_title: 'Как вам такой заголовок статьи?',
        image_url: 'https://mhealth.ru/pictures.jpg',
        articleId: '12345',
    },
    {
        article_title: 'Интересная статья?',
        image_url: 'https://whealth.ru/pictures.jpg',
        articleId: '6789',
    },
];

describe('ArticleAdapter', () => {
    it('should transform keys', () => {
        const result = transform(createFakeResponse());
        assert.deepStrictEqual(result, {
            totalCount: 100500,
            list: expectedArticles,
        });
    });
});
