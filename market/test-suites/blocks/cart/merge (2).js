import {
    makeSuite,
    makeCase,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {path, isEqual} from 'ambar';
import cartItemsIds from '@self/root/src/spec/hermione/configs/cart/items';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import {IS_LOGGED_IN_QUERY_PARAM} from '@self/root/src/constants/queryParams';
import profile from '@self/root/src/spec/hermione/configs/profile';
import {prepareCartItemsBySkuId} from '@self/root/src/spec/hermione/scenarios/cart';
import {CART_TITLE} from '@self/root/src/entities/checkout/cart/constants';

const {doNotTouchMe} = profile;
const {skuId, offerId} = cartItemsIds.asus;

export default makeSuite('Мерж корзин.', {
    feature: 'Корзина',
    environment: 'testing',
    id: 'bluemarket-2724',
    issue: 'BLUEMARKET-6356',
    defaultParams: {
        items: [{
            skuId,
            offerId,
        }],
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderTotal: () => this.createPageObject(OrderTotal),
                header: () => this.createPageObject(Header),
                cartHeader: () => this.createPageObject(CartHeader),
            });

            const {login, password} = doNotTouchMe;
            const {browser, params} = this;
            const {items, region} = params;

            await browser.yaMdaTestLogin(login, password);
            await browser.yaScenario(this, 'cartResource.clean');
            await browser.yaMdaTestLogout();
            const cartItems = await browser.yaScenario(this, prepareCartItemsBySkuId, {
                items,
                region,
            });
            await browser.yaScenario(this, 'cartResource.prepareCart', {offers: cartItems});
            // get параметр IS_LOGGED_IN_QUERY_PARAM необходим для мерджа корзин
            const retPath = await this.browser.yaBuildFullUrl(PAGE_IDS_COMMON.CART, {
                lr: region,
                [IS_LOGGED_IN_QUERY_PARAM]: 1,
            });
            await browser.yaMdaTestLogin(login, password, retPath);
            return browser.allure.runStep('Ждем завершения актуализации', async () => {
                const preloaderVisibility = await this.orderInfoPreloader.waitForVisible();
                if (preloaderVisibility) {
                    return this.orderInfoPreloader.waitForHidden(30 * 1000);
                }
            });
        },
        'Товары разлогина оказываются в корзине после залогина': makeCase({
            async test() {
                const itemsCount = this.params.items.length;
                await this.orderTotal.getItemsCount()
                    .should.eventually.to.be.equal(
                        itemsCount,
                        `В саммари должен быть ${itemsCount} товар`
                    );
                await this.cartHeader.getTitleText()
                    .should.eventually.to.be.include(
                        CART_TITLE,
                        `Заголовок корзины должен содержать текст "${CART_TITLE}"`
                    );
                const cartItems = path(
                    ['result', 'items'],
                    await this.browser.yaScenario(this, 'cartResource.getCartList')
                );
                return this.expect(isEqual(
                    cartItems.map(item => item.objId),
                    this.params.items.map(item => item.offerId)
                )).to.be.equal(true, 'В корзине должны быть офферы незалогина');
            },
        }),
    },
});
