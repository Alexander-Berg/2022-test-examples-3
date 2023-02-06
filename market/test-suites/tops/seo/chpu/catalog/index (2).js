import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import MenuCatalogChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/MenuCatalog/chpu';
import SearchOptionsChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOptions/chpu';
// page-objects
import MenuCatalog from '@self/platform/spec/page-objects/components/MenuCatalog';
import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';
import FilterCompound from '@self/platform/components/FilterCompound/__pageObject';
import SelectFilter from '@self/platform/components/SelectFilter/__pageObject';
import FilterPopup from '@self/platform/containers/FilterPopup/__pageObject';
import Filters from '@self/platform/components/Filters/__pageObject';

export default makeSuite('Каталог', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Главная страница', {
            story: mergeSuites(
                {
                    beforeEach() {
                        return this.browser.yaOpenPage('touch:index');
                    },
                },
                prepareSuite(MenuCatalogChpuSuite, {
                    pageObjects: {
                        menuCatalog() {
                            return this.createPageObject(MenuCatalog);
                        },
                        header() {
                            return this.createPageObject(Header);
                        },
                        sideMenu() {
                            return this.createPageObject(SideMenu);
                        },
                    },
                })
            ),
        }),
        makeSuite('Страница Каталога.', {
            story: mergeSuites(
                createStories({
                    catalog: {
                        description: 'Каталог',
                        route: 'touch:list',
                        routeParams: routes.catalog.list,
                        params: {
                            pageRoot: 'bloki-pitaniia-dlia-kompiuterov',
                        },
                    },
                }, ({route, routeParams, params}) => prepareSuite(SearchOptionsChpuSuite, {
                    hooks: {
                        beforeEach() {
                            return this.browser.yaOpenPage(route, routeParams);
                        },
                    },
                    pageObjects: {
                        searchOptions() {
                            return this.createPageObject(SearchOptions);
                        },
                        filterCompound() {
                            return this.createPageObject(FilterCompound);
                        },
                        selectFilter() {
                            return this.createPageObject(SelectFilter);
                        },
                        filterPopup() {
                            return this.createPageObject(FilterPopup);
                        },
                        filters() {
                            return this.createPageObject(Filters);
                        },
                    },
                    params,
                }))
            ),
        })
    ),
});
