import {makeSuite, prepareSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import CompareTumblerSuite from '@self/platform/spec/hermione/test-suites/blocks/CompareTumbler/index';
// page-objects
import SearchSnippetComparisonButton from '@self/platform/spec/page-objects/containers/SearchSnippet/ComparisonButton';
import Notification from '@self/root/src/components/Notification/__pageObject';
import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';

import {productWithDefaultOffer} from '@self/platform/spec/hermione/fixtures/product';

export default makeSuite('Кнопка добавления в «Cравнение»', {
    environment: 'kadavr',
    story: {
        'Для неавторизованного пользователя.': prepareSuite(CompareTumblerSuite, {
            hooks: {
                async beforeEach() {
                    const reportState = mergeState([
                        productWithDefaultOffer,
                        {
                            data: {
                                search: {
                                    total: 1,
                                    totalOffers: 1,
                                },
                            },
                        },
                    ]);

                    await this.browser.setState('report', reportState);

                    return this.browser.yaOpenPage('touch:search', routes.search.default);
                },
            },
            pageObjects: {
                header() {
                    return this.createPageObject(Header);
                },
                sideMenu() {
                    return this.createPageObject(SideMenu);
                },
                compareTumbler() {
                    return this.createPageObject(SearchSnippetComparisonButton);
                },
                notification() {
                    return this.createPageObject(Notification);
                },
            },
        }),
        'Для авторизованного пользователя.': prepareSuite(CompareTumblerSuite, {
            hooks: {
                async beforeEach() {
                    const reportState = mergeState([
                        productWithDefaultOffer,
                        {
                            data: {
                                search: {
                                    total: 1,
                                    totalOffers: 1,
                                },
                            },
                        },
                    ]);

                    await this.browser.setState('report', reportState);

                    return this.browser.yaProfile('ugctest3', 'touch:search', routes.search.default);
                },
            },
            pageObjects: {
                header() {
                    return this.createPageObject(Header);
                },
                sideMenu() {
                    return this.createPageObject(SideMenu);
                },
                compareTumbler() {
                    return this.createPageObject(SearchSnippetComparisonButton);
                },
                notification() {
                    return this.createPageObject(Notification);
                },
            },
        }),
    },
});
