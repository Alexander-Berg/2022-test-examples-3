import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import WishlistTumblerSuite from '@self/platform/spec/hermione/test-suites/blocks/WishlistTumbler';
// page-objects
import SearchSnippetWishlistButton from '@self/platform/spec/page-objects/containers/SearchSnippet/WishlistButton';
import WishlistEntrypoint from '@self/platform/spec/page-objects/widgets/core/WishlistEntrypoint';
import Notification from '@self/root/src/components/Notification/__pageObject';
import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';

import {productWithDefaultOffer} from '@self/platform/spec/hermione/fixtures/product';

export default makeSuite('Кнопка добавления в «избранное»', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(WishlistTumblerSuite, {
            hooks: {
                async beforeEach() {
                    const reportState = mergeState([
                        /**
                         * @todo (@weed)
                         * Выяснить какие поля обязательны для добавления в вишлист и выпилить отсюда этот жирнющий мок
                         */
                        productWithDefaultOffer,
                        {
                            data: {
                                search: {
                                    total: 1,
                                    totalOffers: 1,
                                    view: 'grid',
                                },
                            },
                        },
                    ]);

                    await this.browser.setState('report', reportState);

                    await this.browser.yaOpenPage('touch:search', routes.search.default);
                },
            },
            pageObjects: {
                header() {
                    return this.createPageObject(Header);
                },
                sideMenu() {
                    return this.createPageObject(SideMenu);
                },
                wishlistTumbler() {
                    return this.createPageObject(SearchSnippetWishlistButton);
                },
                wishlistEntrypoint() {
                    return this.createPageObject(WishlistEntrypoint, {
                        parent: SideMenu.root,
                    });
                },
                notification() {
                    return this.createPageObject(Notification);
                },
            },
        })
    ),
});
