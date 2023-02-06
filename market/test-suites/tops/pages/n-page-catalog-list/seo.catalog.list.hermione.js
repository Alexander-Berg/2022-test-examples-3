import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createEntityPicture, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import SnippetCard2SeoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-card2/seo';
import SnippetCell2SeoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-cell2/seo';
// page-objects
import SnippetList from '@self/root/src/widgets/content/search/Serp/components/Page/__pageObject';
import SnippetCard2 from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import SnippetCell2 from '@self/project/src/components/Search/Snippet/Cell/__pageObject';

import {phonePicture1} from './fixtures/pictures';
import {product} from './seo/mocks/seo.mock';
import {GRID_VIEW_DATA_STATE, LIST_VIEW_DATA_STATE, MINIMAL_DATA_STATE} from './constants';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница каталога.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    snippetList: () => this.createPageObject(
                        SnippetList,
                        {
                            root: `${SnippetList.root}:nth-child(1)`,
                        }
                    ),
                    snippetCard2: () => this.createPageObject(
                        SnippetCard2,
                        {
                            parent: this.snippetList,
                            root: `${SnippetCard2.root}:nth-of-type(1)`,
                        }
                    ),
                    snippetCell2: () => this.createPageObject(
                        SnippetCell2,
                        {
                            parent: this.snippetList,
                            root: `${SnippetCell2.root}:nth-of-type(1)`,
                        }
                    ),
                });
            },
        },

        makeSuite('Листовая выдача.', {
            story: mergeSuites({
                async beforeEach() {
                    const reportState = mergeState([
                        createEntityPicture(
                            phonePicture1,
                            'product',
                            product.id,
                            phonePicture1.url
                        ),
                        product.mock,
                        MINIMAL_DATA_STATE,
                        LIST_VIEW_DATA_STATE,
                    ]);

                    await this.browser.setState('report', reportState);
                },
            },
            makeSuite('Тип "Гуру".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 54726,
                                slug: 'mobilnye-telefony',
                                viewtype: 'list',
                            });
                        },
                    },
                    prepareSuite(SnippetCard2SeoSuite)
                ),
            }),
            makeSuite('Тип "Гуру-лайт".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 57743,
                                slug: 'parketnaia-doska',
                                viewtype: 'list',
                            });
                        },
                    },
                    prepareSuite(SnippetCard2SeoSuite)
                ),
            }),

            makeSuite('Тип "Кластеризованная".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 57254,
                                slug: 'zhenskie-kolgotki-i-chulki',
                                viewtype: 'list',
                            });
                        },
                    },
                    prepareSuite(SnippetCard2SeoSuite)
                ),
            }),

            makeSuite('Тип "Книги".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 56587,
                                slug: 'otechestvennaia-proza',
                                viewtype: 'list',
                            });
                        },
                    },
                    prepareSuite(SnippetCard2SeoSuite)
                ),

            }),

            makeSuite('Тип "Одежда".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 7811901,
                                slug: 'vse-tovary',
                                viewtype: 'list',
                            });
                        },
                    },
                    prepareSuite(SnippetCard2SeoSuite)
                ),
            }),

            makeSuite('Тип "Выдачи с параметром text".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                text: 'люк под плитку',
                                nid: 6517808,
                                slug: 'luk-pod-plitky',
                                viewtype: 'list',
                            });
                        },
                    },
                    prepareSuite(SnippetCard2SeoSuite)
                ),
            })

            ),
        }),

        makeSuite('Гридовая выдача.', {
            story: mergeSuites({
                async beforeEach() {
                    const reportState = mergeState([
                        createEntityPicture(
                            phonePicture1,
                            'product',
                            product.id,
                            phonePicture1.url
                        ),
                        product.mock,
                        MINIMAL_DATA_STATE,
                        GRID_VIEW_DATA_STATE,
                    ]);

                    await this.browser.setState('report', reportState);
                },
            },
            makeSuite('Тип "Гуру".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 54726,
                                slug: 'mobilnye-telefony',
                                viewtype: 'grid',
                            });
                        },
                    },
                    prepareSuite(SnippetCell2SeoSuite)
                ),
            }),

            makeSuite('Тип "Гуру-лайт".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 57743,
                                slug: 'parketnaia-doska',
                                viewtype: 'grid',
                            });
                        },
                    },
                    prepareSuite(SnippetCell2SeoSuite)
                ),
            }),

            makeSuite('Тип "Кластеризованная".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 57254,
                                slug: 'zhenskie-kolgotki-i-chulki',
                                viewtype: 'grid',
                            });
                        },
                    },
                    prepareSuite(SnippetCell2SeoSuite)
                ),
            }),

            makeSuite('Тип "Книги".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 56587,
                                slug: 'otechestvennaia-proza',
                                viewtype: 'grid',
                            });
                        },
                    },
                    prepareSuite(SnippetCell2SeoSuite)
                ),
            }),

            makeSuite('Тип "Одежда".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                nid: 7811901,
                                slug: 'vse-tovary',
                                viewtype: 'grid',
                            });
                        },
                    },
                    prepareSuite(SnippetCell2SeoSuite)
                ),
            }),

            makeSuite('Тип "Выдачи с параметром text".', {
                story: mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:list', {
                                text: 'люк под плитку',
                                nid: 6517808,
                                slug: 'luk-pod-plitky',
                                viewtype: 'grid',
                            });
                        },
                    },
                    prepareSuite(SnippetCell2SeoSuite)
                ),
            })

            ),
        }
        )

    )}
);
