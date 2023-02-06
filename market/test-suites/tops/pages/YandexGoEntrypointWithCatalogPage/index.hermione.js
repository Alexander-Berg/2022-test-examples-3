import {makeSuite, mergeSuites} from '@yandex-market/ginny';

// utils
import {prepareCartItemsBySkuId} from '@self/root/src/spec/hermione/scenarios/cart';
import {prepareCart} from '@self/root/src/spec/hermione/scenarios/cartResource';

// fixtures and mocks
import {YandexGoEntrypointWithCatalogPageCmsLayout} from '@self/root/src/spec/hermione/kadavr-mock/tarantino/YandexGoEntrypointWithCatalogPage';
import {PAGE_IDS_YANDEX_GO} from '@self/root/src/constants/pageIds';
import {MOSCOW_REGION_ID, DEFAULT_GPS_COORDINATE} from '@self/root/src/constants/express';
import {UGC_TEST3_DEFAULT_USER} from '@self/root/src/spec/hermione/fixtures/user';
import {offerMock, skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import expressNavigationTree from '@self/root/src/spec/hermione/kadavr-mock/cataloger/expressNavigationTree';

// suites
import ExpressCatalogEntrypointsSuite from '@self/platform/spec/hermione2/test-suites/blocks/ExpressCatalogEntrypoints';
import YandexGoCartButtonPopupSuite from '@self/platform//spec/hermione2/test-suites/blocks/YandexGoCartButtonPopup';

// page-objects
import ExpressCatalogEntrypointsPO from '@self/root/src/widgets/content/ExpressCatalogEntrypoints/__pageObject';

const CART_SKU = {
    ...skuMock,
    offers: {items: [offerMock]},
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('[Yandex.Go] Главная в вебе.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-64987',
    story: mergeSuites(
        {
            async beforeEach() {
                hermione.setPageObjects.call(this, {
                    expressCatalogEntrypoints: () => this.browser.createPageObject(ExpressCatalogEntrypointsPO),
                });

                const cartItems = await this.browser.yaScenario(this, prepareCartItemsBySkuId, {
                    region: this.params.region,
                    items: [{skuId: skuMock.id, offerId: offerMock.wareId, count: 1}],
                    reportSkus: [CART_SKU],
                });
                await this.browser.yaScenario(this, prepareCart, {offers: cartItems});

                await this.browser.setState(
                    'Tarantino.data.result',
                    [YandexGoEntrypointWithCatalogPageCmsLayout]
                );
                await this.browser.setState(
                    'Cataloger.tree',
                    expressNavigationTree
                );
                await this.browser.setState(
                    'schema',
                    {users: [UGC_TEST3_DEFAULT_USER]}
                );

                await this.browser.yaProfile('ugctest3', PAGE_IDS_YANDEX_GO.MAIN, {
                    lr: MOSCOW_REGION_ID,
                    gps: DEFAULT_GPS_COORDINATE,
                });
            },
            ExpressCatalogEntrypointsSuite,
            YandexGoCartButtonPopupSuite,
        }
    ),
});
