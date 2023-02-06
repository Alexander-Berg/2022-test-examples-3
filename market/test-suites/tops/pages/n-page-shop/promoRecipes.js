import {makeSuite, prepareSuite} from 'ginny';
import {createPromoRecipe, createShopInfo, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import ShopInformationPromoSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShopInformation/__promo';
import ShopInformation from '@self/platform/widgets/content/ShopInformation/__pageObject';

import shopInfoMockData from './mocks/shopInfo.mock.json';
import tarantinoMockData from './mocks/tarantino.mock.json';

export default makeSuite('Акции.', {
    environment: 'kadavr',
    story: prepareSuite(ShopInformationPromoSuite, {
        hooks: {
            async beforeEach() {
                const promocodeRecipeState = createPromoRecipe({
                    id: '1235',
                    type: 'shop-promo-code',
                    shop_name: shopInfoMockData.shopName,
                    link: `/promo/promocodes_for_${shopInfoMockData.id}`,
                }, '1235');

                const promoRecipeState = createPromoRecipe({
                    id: '1234',
                    type: 'shop-all-promo',
                    shop_name: shopInfoMockData.shopName,
                    link: `/promo/promo_for_${shopInfoMockData.id}`,
                }, '1234');

                const state = mergeState([
                    createShopInfo(shopInfoMockData, shopInfoMockData.id),
                    promocodeRecipeState,
                    promoRecipeState,
                ]);

                await this.browser.setState('report', state);
                await this.browser.setState('Tarantino.data.result', [tarantinoMockData]);

                return this.browser.yaOpenPage('market:shop', {
                    shopId: shopInfoMockData.id,
                    slug: shopInfoMockData.slug,
                });
            },
        },
        params: {
            shopId: shopInfoMockData.id,
        },
        pageObjects: {
            shopInformation() {
                return this.createPageObject(ShopInformation);
            },
        },
    }),
});
