import {makeSuite, prepareSuite} from 'ginny';

// siutes
import DefaultOfferContentSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer/content';

// fixtures
import {
    phoneProductRoute,
    productDrafaultDigitalOffer,
} from '@self/platform/spec/hermione/fixtures/product';


export default makeSuite('Цифровой оффер.', {
    story: prepareSuite(DefaultOfferContentSuite, {
        meta: {
            id: 'marketfront-5959',
            issue: 'MARKETFRONT-81052',
            environment: 'kadavr',
        },
        params: {
            deliveryTexts: ['Получение по электронной почте'],
            showReturnPolicy: true,
            policyText: 'Возврату не подлежит',
            policyHintText: 'Вернуть его нельзя, как и другие онлайн-подписки, карты оплаты и коды активации. Отменить заказ тоже не получится.',
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('report', productDrafaultDigitalOffer);
                await this.browser.yaOpenPage('touch:product', phoneProductRoute);
            },
        },
    }),
});
