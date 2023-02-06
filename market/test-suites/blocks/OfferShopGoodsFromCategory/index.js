import {makeSuite, makeCase, mergeSuites} from 'ginny';

/**
 * @param {PageObject.ScrollBox} ScrollBox
 * @param {PageObject.ShopInfo} ShopInfo
 */
export default makeSuite('Карусель "Товары из категории от магазина"', {
    story: mergeSuites(
        {
            'По умолчанию': {
                'содержится на странице': makeCase({
                    async test() {
                        await this.scrollBox.isExisting();
                    },
                }),
            },

            'При переходе по товару из карусели': {
                'магазин товара, по которому перешли, совпадает с магазином товара, со страницы которого сделан переход': makeCase({
                    async test() {
                        await this.scrollBox.getItemByIndex(1).click();
                        await this.browser.yaWaitForPageReady();
                        const shopId = await this.shopInfo.getShopId();

                        return this.expect(shopId, 'id магазинов должны совпадать')
                            .to.be.equal(this.params.expectedShopId, 'Id магазинов совпадают');
                    },
                }),
            },
        }
    ),
});
