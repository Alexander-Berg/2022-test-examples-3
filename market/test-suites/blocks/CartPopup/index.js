import {makeCase, makeSuite} from 'ginny';
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';
import {
    promocodeWithItemsInfo,
    genericBundlePromo,
    flashDiscountPromo,
    cashbackPromo,
    cheapestAsGiftPromo,
    discountPromo,
} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';
import {offerMock} from '@self/project/src/spec/hermione/fixtures/offer/offer';


export default makeSuite('Попап перехода в корзину.', {
    params: {
        pageId: 'id страницы, которая будет открыта',
        routeParams: 'Параметры страницы для роутинга',
        reportMock: 'Данные для мокирования запроса в репорт',
    },
    story: {
        'Отображается с верным промо': {
            'Флеш дискаунт': makeCase({
                async test() {
                    await this.browser.setState('Carter.items', []);
                    await this.browser.setState('report', this.params.createReportMock({promos: [flashDiscountPromo]}));

                    await this.browser.yaOpenPage(this.params.pageId, this.params.routeParams);

                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);

                    await this.cartButton.click();

                    return this.browser.waitForVisible(CartPopup.flashDiscountPromo, 5000);
                },
            }),
            'Кэшбек': makeCase({
                async test() {
                    await this.browser.setState('Carter.items', []);
                    await this.browser.setState('report', this.params.createReportMock({promos: [cashbackPromo]}));

                    await this.browser.yaOpenPage(this.params.pageId, this.params.routeParams);

                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);

                    await this.cartButton.click();

                    return this.browser.waitForVisible(CartPopup.cashbackPromo, 5000);
                },
            }),
            'Промокод': makeCase({
                async test() {
                    await this.browser.setState('Carter.items', []);
                    await this.browser.setState('report', this.params.createReportMock({promos: [promocodeWithItemsInfo]}));

                    await this.browser.yaOpenPage(this.params.pageId, this.params.routeParams);

                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);

                    await this.cartButton.click();

                    return this.browser.waitForVisible(CartPopup.promoCodePromo, 5000);
                },
            }),
            'Подарок': makeCase({
                async test() {
                    await this.browser.setState('Carter.items', []);

                    const reportMock = this.params.createReportMock({promos: [genericBundlePromo]});

                    const giftOffer = {
                        ...offerMock,
                        showUid: '16291235305976641110606000',
                        feeShow: 'THi2k-pxPNIY94CGe9J2sshgurE1A28',
                        offerId: 'MRkHuflzbQ8rDi_6fP1y0A',
                    };
                    reportMock.data.offers = [giftOffer];
                    reportMock.collections.offer.cCKC4HrCLP9aaFdOcCGSsQ = giftOffer;

                    await this.browser.setState('report', reportMock);

                    await this.browser.yaOpenPage(this.params.pageId, this.params.routeParams);

                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);

                    await this.cartButton.click();


                    return this.browser.waitForVisible(CartPopup.giftPromo, 10000);
                },
            }),
            'n по цене n-1': makeCase({
                async test() {
                    await this.browser.setState('Carter.items', []);
                    await this.browser.setState('report', this.params.createReportMock({promos: [cheapestAsGiftPromo]}));

                    await this.browser.yaOpenPage(this.params.pageId, this.params.routeParams);

                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);

                    await this.cartButton.click();

                    return this.browser.waitForVisible(CartPopup.cheapestAsGiftPromo, 5000);
                },
            }),
            'Прямая скидка': makeCase({
                async test() {
                    await this.browser.setState('Carter.items', []);
                    await this.browser.setState('report', this.params.createReportMock(discountPromo));

                    await this.browser.yaOpenPage(this.params.pageId, this.params.routeParams);

                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);

                    await this.cartButton.click();

                    return this.browser.waitForVisible(CartPopup.discountPromo, 5000);
                },
            }),
        },
    },
});
