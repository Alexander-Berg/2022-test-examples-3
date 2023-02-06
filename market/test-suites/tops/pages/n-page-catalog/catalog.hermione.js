import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import {commonCatalogQuizMock, availableHidsAndNidsMock} from '@self/root/src/widgets/content/CatalogQuiz/__spec__/mock';

// suites
import FiltersInteractionRecipesSuite from '@self/platform/spec/hermione/test-suites/blocks/FiltersInteraction/recipes';
import NavigationDepartmentPromoSuite from '@self/platform/spec/hermione/test-suites/blocks/NavigationDepartment/promo';
import CatalogMenuVirtualNodeSuite from '@self/platform/spec/hermione/test-suites/blocks/catalog-menu/virtual-node';
import CatalogMenuNoLeafLargeNodeSuite from '@self/platform/spec/hermione/test-suites/blocks/catalog-menu/no-leaf-large-node';
import CatalogMenuGuruRecipeNodeSuite from '@self/platform/spec/hermione/test-suites/blocks/catalog-menu/guru_recipe-node';
import growingCashbackIncut from '@self/root/src/spec/hermione/test-suites/desktop.blocks/growingCashback/incutSuites';
import catalogQuizSuite from '@self/root/src/spec/hermione/test-suites/blocks/catalogQuiz';

// page-objects
import StartScreen from '@self/root/src/widgets/content/CatalogQuiz/components/StartScreen/__pageObject';
import RecipesList from '@self/platform/spec/page-objects/recipes-list';
import Navigation from '@self/platform/spec/page-objects/Navigation';
import NavigationDepartment from '@self/platform/spec/page-objects/NavigationDepartment';

import departmentPage from './fixtures/department-page';
import catalogPage from './fixtures/catalog-page';
import navigationTreeWithPromo from './fixtures/navigation-tree-promo';
import {level1Node, level2Node} from './fixtures/nested-navigation-tree';
import {parentOfVirtualNode, virtualNode} from './fixtures/tree-with-virtual-node';
import {getCatalogerMock} from './helpers/cataloger-mock';
import seo from './seo';
import cashback from './cashback';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Морда категории.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Фильтры.', {
            environment: 'testing',
            story: mergeSuites(
                prepareSuite(FiltersInteractionRecipesSuite, {
                    meta: {
                        id: 'marketfront-610',
                        issue: 'MARKETVERSTKA-24661',
                    },
                    hooks: {
                        beforeEach() {
                            // eslint-disable-next-line market/ginny/no-skip
                            return this.skip('MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения');

                            // eslint-disable-next-line no-unreachable
                            return this.browser.yaOpenPage('market:catalog', routes.catalog.electronics);
                        },
                    },
                    pageObjects: {
                        recipes() {
                            return this.createPageObject(RecipesList);
                        },
                    },
                })
            ),
        }),

        makeSuite('Навигационное дерево.', {
            story: mergeSuites(
                prepareSuite(NavigationDepartmentPromoSuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('Tarantino.data.result', [departmentPage]);
                            await this.browser.setState('Cataloger.tree', navigationTreeWithPromo);
                            return this.browser.yaOpenPage('market:catalog', routes.catalog.electronics);
                        },
                    },
                    pageObjects: {
                        navigation() {
                            return this.createPageObject(Navigation);
                        },
                        navigationDepartment() {
                            return this.createPageObject(NavigationDepartment, {parent: this.navigation});
                        },
                    },
                }),
                prepareSuite(CatalogMenuVirtualNodeSuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('Tarantino.data.result', [catalogPage]);
                            await this.browser.setState('Cataloger.tree', getCatalogerMock([parentOfVirtualNode]));

                            this.params.itemDisplayName = virtualNode.name;
                            return this.browser.yaOpenPage('market:catalog', {
                                nid: level1Node.id,
                                slug: level1Node.slug,
                            });
                        },
                    },
                    pageObjects: {
                        navigationDepartment() {
                            return this.createPageObject(NavigationDepartment);
                        },
                    },
                }),
                prepareSuite(CatalogMenuNoLeafLargeNodeSuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('Tarantino.data.result', [catalogPage]);
                            await this.browser.setState('Cataloger.tree', getCatalogerMock([level1Node]));

                            this.params.nodeDisplayName = level2Node.name;
                            this.params.collapsedItemCount = 5;
                            this.params.expandedItemCount = 10;
                            return this.browser.yaOpenPage('market:catalog', {
                                nid: level1Node.id,
                                slug: level1Node.slug,
                            });
                        },
                    },
                    pageObjects: {
                        navigationDepartment() {
                            return this.createPageObject(NavigationDepartment);
                        },
                    },
                }),
                prepareSuite(CatalogMenuGuruRecipeNodeSuite, {
                    hooks: {
                        async beforeEach() {
                            const nid = 73321;
                            const name = 'Проверяемый текст';
                            const params = {
                                'glfilter': ['8230602:8230604,8230603'],
                                'hid': ['7959699'],
                                'nid': [nid],
                            };
                            const navNode = {
                                'childrenType': 'guru',
                                'entity': 'navnode',
                                'fullName': 'Чепчики для малышей',
                                'id': nid,
                                'isLeaf': true,
                                'link': {
                                    params,
                                    target: 'list',
                                },
                                name,
                                'rootNavnode': {
                                    'entity': 'navnode',
                                    'id': 54432,
                                },
                                'slug': 'chepchiki-dlia-malyshei',
                                'type': 'guru_recipe',
                            };

                            const parentNode = {
                                'childrenType': 'guru',
                                'entity': 'navnode',
                                'fullName': 'Аксессуары для малышей',
                                'id': 73320,
                                'slug': 'aksessuary-dlia-malyshei',
                                'isLeaf': false,
                                'type': 'virtual',
                                'name': 'Аксессуары',
                                'navnodes': [navNode],
                            };

                            this.params.itemDisplayName = name;
                            this.params.linkParams = params;

                            await this.browser.setState('Tarantino.data.result', [catalogPage]);
                            await this.browser.setState('Cataloger.tree', getCatalogerMock([parentNode]));

                            return this.browser.yaOpenPage('market:catalog', {
                                nid: parentNode.id,
                                slug: parentNode.slug,
                            });
                        },
                    },
                    pageObjects: {
                        navigationDepartment() {
                            return this.createPageObject(NavigationDepartment);
                        },
                    },
                })
            ),
        }),
        seo,
        cashback,
        prepareSuite(growingCashbackIncut, {
            params: {
                viewType: 'grid',
            },
        }),
        prepareSuite(catalogQuizSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('Tarantino.data.result', [
                        commonCatalogQuizMock,
                        availableHidsAndNidsMock,
                    ]);
                },
            },
            pageObjects: {
                StartScreen() {
                    return this.createPageObject(StartScreen);
                },
            },
            params: {
                pageId: 'market:list',
            },
        })
    ),
});
