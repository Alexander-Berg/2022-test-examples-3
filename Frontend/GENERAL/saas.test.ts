import assert from 'assert';

import { SAAS_TIMEOUT, SAAS_IZ_RANK_VALUE, SAAS_PRON } from 'config/constants';
import { Saas } from 'apphost';
import {
    allSearchGtaAttributes,
    articleTagGtaAttributes,
    createAuthorRequestItem,
    createAuthorsRequestItem,
    createBegemotRequestItem,
    createSaasRequestItem,
    createSaasSearchRequestItem,
    createSaasSearchFetchMoreByGroupRequestItem,
    createSaasSearchTagRequestItem,
    createSaasSearchTagFetchMoreByGroupRequestItem,
    createSaasTurboRequestItem,
    extractSaasFactors,
    getHealthSettingsRawData,
    getPatchedRequestForBegemot,
    KPS,
    searchGtaAttributesByCategory,
    turboArticleGtaAttributes,
} from './saas';

const contextStub = {
    appHostParams: {
        reqid: '123456789',
    },
    request: {
        headers: {
            'x-real-ip': '37.140.187.122',
            'x-yandex-expboxes': '132069,0,41;102711,0,40',
        },
        cookies_parsed: {
            yandexuid: '127565294155370059',
        },
    },
    getAuthorInfo() {
        return {
            isAuthor: false,
            publicId: null,
        };
    },
};

const defaultRequest = {
    timeout: SAAS_TIMEOUT,
    reqid: '123456789',
    'client-ip': '37.140.187.122',
    uuid: 'y127565294155370059',
    test_buckets: '132069,0,41;102711,0,40',
    kps: KPS.articlesTurbo,
    service: 'ya_health_articles_portal',
    p: undefined,
    how: undefined,
    g: '0..14.1',
    hr: 'json',
    gta: turboArticleGtaAttributes,
    relev: 'filter_border=0.35;border_keep_refine=1',
};

const transformedDefaultRequest = {
    ...defaultRequest,
    use_qtree: true,
    use_filter_classifiers: false,
    ms: '',
    ag0: [],
    g: [defaultRequest.g],
};

describe('createSaasRequestItem', () => {
    it('Без cgi параметров', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({}), contextStub),
            {
                ...defaultRequest,
                how: Saas.SortKeys.default,
                text: `${Saas.SortKeys.default}:<=${SAAS_IZ_RANK_VALUE}`,
            }
        );
    });

    it('С параметром text', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ text: ['диабет'] }), contextStub),
            {
                ...defaultRequest,
                text: 'диабет',
                how: 'rlv',
                template: '(%request%) <- iz_has_annotation:>0',
            }
        );
    });

    it('С параметрами text и ids', () => {
        assert.deepStrictEqual(
            createSaasRequestItem(
                extractSaasFactors({ text: ['диабет'], ids: ['45146,38176'] }),
                // @ts-ignore
                contextStub
            ),
            {
                ...defaultRequest,
                text: 'диабет',
                template: '(%request%) <- url:45146 | url:38176',
            }
        );
    });

    it('С параметром id', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ id: ['35197'] }), contextStub),
            {
                ...defaultRequest,
                text: 'url:35197',
                relev: 'u=35197;filter_border=0.35;border_keep_refine=1',
            }
        );
    });

    it('С параметром id и p', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ id: ['35197'], p: ['1'] }), contextStub),
            {
                ...defaultRequest,
                text: 'url:35197',
                p: 2,
                relev: 'u=35197;filter_border=0.35;border_keep_refine=1',
            }
        );
    });

    it('С параметрами text и id', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ text: ['диабет'], id: ['35197'] }), contextStub),
            {
                ...defaultRequest,
                text: 'диабет',
                template: '(%request%) <- url:35197',
                relev: 'u=35197;filter_border=0.35;border_keep_refine=1',
            }
        );
    });

    it('С параметрами text, id и ids', () => {
        assert.deepStrictEqual(
            createSaasRequestItem(
                extractSaasFactors({ text: ['диабет'], id: ['35197'], ids: ['45146,38176'] }),
                // @ts-ignore
                contextStub
            ),
            {
                ...defaultRequest,
                text: 'диабет',
                template: '(%request%) <- url:35197 | url:45146 | url:38176 <- url:35197',
                relev: 'u=35197;filter_border=0.35;border_keep_refine=1',
            }
        );
    });

    it('С параметрами id и ids', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ id: ['35197'], ids: ['45146,38176'] }), contextStub),
            {
                ...defaultRequest,
                text: 'url:35197 | url:45146 | url:38176 <- url:35197',
                relev: 'u=35197;filter_border=0.35;border_keep_refine=1',
            }
        );
    });

    it('С параметрами ids', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ ids: ['45146,38176'] }), contextStub),
            {
                ...defaultRequest,
                text: 'url:45146 | url:38176',
            }
        );
    });

    it('В контексте нет params', () => {
        const context = { ...contextStub, appHostParams: {} };
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ ids: ['45146,38176'] }), context),
            {
                ...defaultRequest,
                reqid: '',
                text: 'url:45146 | url:38176',
            }
        );
    });

    it('В контексте нет yandexuid куки', () => {
        const context = { ...contextStub, request: { cookies_parsed: {}, headers: contextStub.request.headers } };
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ ids: ['45146,38176'] }), context),
            {
                ...defaultRequest,
                uuid: '',
                text: 'url:45146 | url:38176',
            }
        );
    });

    it('В контексте нет нужных заголовков', () => {
        const context = {
            ...contextStub,
            request: {
                headers: {},
                cookies_parsed:
                    {
                        ...contextStub.request.cookies_parsed,
                    },
            },
        };
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasRequestItem(extractSaasFactors({ ids: ['45146,38176'] }), context),
            {
                ...defaultRequest,
                'client-ip': '',
                test_buckets: '',
                text: 'url:45146 | url:38176',
            }
        );
    });
});

describe('getHealthSettingsRawData', () => {
    it('Корректно транформирует запрос Saas, при переходах с колдунщика c ids', () => {
        // @ts-ignore
        const requestData = createSaasRequestItem(extractSaasFactors({ ids: ['45146,38176'] }), contextStub);
        assert.deepStrictEqual(getHealthSettingsRawData(requestData), transformedDefaultRequest);
    });

    it('Корректно транформирует запрос Saas, при поисковых запросах', () => {
        // @ts-ignore
        const requestData = createSaasRequestItem(extractSaasFactors({ text: ['диабет'] }), contextStub);
        assert.deepStrictEqual(getHealthSettingsRawData(requestData), {
            ...transformedDefaultRequest,
            template: '(%request%) <- iz_has_annotation:>0',
            how: 'rlv',
        });
    });

    it('Корректно транформирует запрос Saas, при переходах с колдунщиков (наличие text и ids)', () => {
        const requestData = createSaasRequestItem(extractSaasFactors({
            text: ['болит'],
            ids: ['64749,86753,76741,75191,79956,80601,90845,77642,92735,76626'],
            // @ts-ignore
        }), contextStub);
        assert.deepStrictEqual(getHealthSettingsRawData(requestData), {
            ...transformedDefaultRequest,
            template: '(%request%) <- url:64749 | url:86753 | url:76741 | url:75191 | url:79956 | url:80601 | url:90845 | url:77642 | url:92735 | url:76626',
        });
    });
});

describe('createBegemotRequestItem', () => {
    it('Корректно отдает данные', () => {
        // @ts-ignore
        const requestData = createSaasRequestItem(extractSaasFactors({ ids: ['45146,38176'] }), contextStub);
        assert.deepStrictEqual(createBegemotRequestItem(requestData),
            {
                all: {
                    health_settings: JSON.stringify({
                        reqid: '123456789',
                        uuid: 'y127565294155370059',
                        'client-ip': '37.140.187.122',
                        test_buckets: '132069,0,41;102711,0,40',
                        timeout: SAAS_TIMEOUT,
                        service: 'ya_health_articles_portal',
                        g: ['0..14.1'],
                        hr: 'json',
                        relev: 'filter_border=0.35;border_keep_refine=1',
                        kps: KPS.articlesTurbo,
                        gta: turboArticleGtaAttributes,
                        use_qtree: true,
                        use_filter_classifiers: false,
                        ms: '',
                        ag0: [],
                    }),
                },
                version: 'INIT.flags 2.0',
                type: 'flags',
            });
    });
});

describe('createSaasTurboRequestItem', () => {
    it('Без cgi параметров', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasTurboRequestItem(extractSaasFactors({}), contextStub),
            {
                ...defaultRequest,
                text: `${Saas.SortKeys.default}:<=${SAAS_IZ_RANK_VALUE}`,
                how: Saas.SortKeys.default,
                gta: turboArticleGtaAttributes,
                kps: KPS.articlesTurbo,
            }
        );
    });
});

describe('getPatchedRequestForBegemot', () => {
    const text = 'болит голова';
    const extraParams = {
        wizextra: ['usextsyntax=1', 'health_saas_softness=6'],
        /* eslint-disable-next-line max-len */
        restrict_config: ['i_:ATTR_INTEGER,doc,template\niz_:ATTR_INTEGER,zone,template\ns_:ATTR_LITERAL,doc,template\nsz_:ATTR_LITERAL,zone,template\nz_:ZONE,doc,template\n']
    };
    const requestStub = {
        params: {
            text: [text],
        },
    };
    const saasRequestStub = {
        template: '(%request%) <- iz_has_annotation:>0',
        text,
    };

    it('Должен добавить дополнительные параметры и изменить text', () => {
        // @ts-ignore
        assert.deepStrictEqual(getPatchedRequestForBegemot(requestStub, saasRequestStub, true), {
            params: {
                ...requestStub.params,
                ...extraParams,
                text: [`(${text}) <- iz_has_annotation:>0`],
            },
        });
    });

    it('Должен добавить дополнительные параметры', () => {
        // @ts-ignore
        assert.deepStrictEqual(getPatchedRequestForBegemot(requestStub, {}), {
            params: {
                ...requestStub.params,
                ...extraParams,
            },
        });
    });
});

const defaultSearchRequest = {
    ...defaultRequest,
    kps: [KPS.pills, KPS.articlesTurbo, KPS.encyclopedia].join(','),
    how: Saas.SortKeys.default,
    pron: SAAS_PRON,
    text: `${Saas.SortKeys.default}:<=${SAAS_IZ_RANK_VALUE}`,
    g: '1.health_internal_search.10.12',
};

describe('createSaasSearchRequestItem', () => {
    it('Без groupId', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasSearchRequestItem(extractSaasFactors({}), contextStub),
            {
                ...defaultSearchRequest,
                g: '1.health_internal_search.10.6',
                gta: allSearchGtaAttributes,
            }
        );
    });

    it('С параметром GroupId = pills', () => {
        assert.deepStrictEqual(
            createSaasSearchRequestItem(
                extractSaasFactors({}),
                // @ts-ignore
                contextStub,
                { groupId: Saas.CategoryNames.pills }
            ),
            {
                ...defaultSearchRequest,
                gta: searchGtaAttributesByCategory.pills,
            }
        );
    });
});

describe('createSaasSearchFetchMoreByGroupRequestItem', () => {
    it('С параметром GroupId = pills', () => {
        assert.deepStrictEqual(
            createSaasSearchFetchMoreByGroupRequestItem(
                extractSaasFactors({}),
                // @ts-ignore
                contextStub,
                { groupId: Saas.CategoryNames.pills }
            ),
            {
                ...defaultSearchRequest,
                gta: searchGtaAttributesByCategory.pills,
                kps: KPS.pills,
                g: '0..12.1'
            }
        );
    });
});

const defaultSearchTagRequest = {
    ...defaultRequest,
    gta: [
        ...searchGtaAttributesByCategory[Saas.CategoryNames.articles],
        ...articleTagGtaAttributes,
    ],
    kps: KPS.articlesTurbo,
    how: Saas.SortArticleKeys.totalViews,
    pron: SAAS_PRON,
    text: `${Saas.ArticleTagGtaAttributeKey.tags}:(аллергия) | ${Saas.ArticleTagGtaAttributeKey.tags}:(анализ крови)`,
    g: '1.health_internal_search.10.12',
    template: '(%request%) <- iz_has_annotation:>0',
};

describe('createSaasSearchTagRequestItem', () => {
    it('С параметрами tags', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasSearchTagRequestItem(extractSaasFactors({ tags: ['аллергия', 'анализ крови'] }), contextStub, {}),
            defaultSearchTagRequest
        );
    });
});

describe('createSaasSearchTagFetchMoreByGroupRequestItem', () => {
    it('С параметрами tags', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createSaasSearchTagFetchMoreByGroupRequestItem(extractSaasFactors({ tags: ['аллергия', 'анализ крови'] }), contextStub, {}),
            {
                ...defaultSearchTagRequest,
                g: '0..12.1',
            }
        );
    });
});

const defaultAuthorRequest = {
    ...defaultRequest,
    kps: [KPS.articlesTurbo, KPS.authors].join(','),
    how: Saas.SortKeys.default,
    text: 's_author_uid:test',
};

describe('createAuthorRequestItem', () => {
    it('Без groupId', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createAuthorRequestItem(extractSaasFactors({}), contextStub, { uid: 'test' }),
            {
                ...defaultAuthorRequest,
                g: '1.health_internal_search.10.100',
                gta: [
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.articles],
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.authors],
                ],
            }
        );
    });

    it('Не автор, с параметрами sort и period', () => {
        const sort = 'views';

        assert.deepStrictEqual(
            // @ts-ignore
            createAuthorRequestItem(extractSaasFactors({ sort: [sort] }), contextStub, { uid: 'test' }),
            {
                ...defaultAuthorRequest,
                g: '1.health_internal_search.10.100',
                gta: [
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.articles],
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.authors],
                ],
            }
        );
    });

    it('Автор, с параметрами sort=views и без period', () => {
        const uid = 'test';

        const authorContextStub = {
            ...contextStub,
            getAuthorInfo() {
                return {
                    isAuthor: true,
                    publicId: uid,
                };
            },
        };

        const sort = 'views';
        const how = 'total_views';

        assert.deepStrictEqual(
            // @ts-ignore
            createAuthorRequestItem(extractSaasFactors({ sort: [sort] }), authorContextStub, { uid }),
            {
                ...defaultAuthorRequest,
                how,
                g: '1.health_internal_search.10.100',
                gta: [
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.articles],
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.authors],
                ],
            }
        );
    });

    it('Автор, с параметрами sort=views и period=day', () => {
        const uid = 'test';

        const authorContextStub = {
            ...contextStub,
            getAuthorInfo() {
                return {
                    isAuthor: true,
                    publicId: uid,
                };
            },
        };

        const sort = 'views';
        const period = 'day';
        const how = 'total_views_day';

        assert.deepStrictEqual(
            // @ts-ignore
            createAuthorRequestItem(extractSaasFactors({ sort: [sort], period: [period] }), authorContextStub, { uid }),
            {
                ...defaultAuthorRequest,
                how,
                g: '1.health_internal_search.10.100',
                gta: [
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.articles],
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.authors],
                ],
            }
        );
    });

    it('Автор, с параметром period=day', () => {
        const uid = 'test';

        const authorContextStub = {
            ...contextStub,
            getAuthorInfo() {
                return {
                    isAuthor: true,
                    publicId: uid,
                };
            },
        };

        const period = 'day';
        const how = 'total_views_day';

        assert.deepStrictEqual(
            // @ts-ignore
            createAuthorRequestItem(extractSaasFactors({ period: [period] }), authorContextStub, { uid }),
            {
                ...defaultAuthorRequest,
                how,
                g: '1.health_internal_search.10.100',
                gta: [
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.articles],
                    ...searchGtaAttributesByCategory[Saas.CategoryNames.authors],
                ],
            }
        );
    });
});

describe('createAuthorsRequestItem', () => {
    it('Без groupId', () => {
        assert.deepStrictEqual(
            // @ts-ignore
            createAuthorsRequestItem(extractSaasFactors({}), contextStub),
            {
                ...defaultRequest,
                kps: KPS.authors,
                how: Saas.SortKeys.default,
                text: `${Saas.SortKeys.default}:<=${SAAS_IZ_RANK_VALUE}`,
                g: '0..24.1',
                gta: searchGtaAttributesByCategory[Saas.CategoryNames.authors],
            }
        );
    });
});
