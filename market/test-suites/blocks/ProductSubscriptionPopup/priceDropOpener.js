import {makeSuite, makeCase} from 'ginny';
import {assign} from 'ambar';
import CONFIRMED_ADVERTISING_SUBSCRIPTION from './subscription.mock';

/**
 * @property {PageObject.PriceDropOpener} this.priceDropOpener блок подписки на снижение цены
 */

export default makeSuite('Кнопка подписки на снижение цены.', {
    feature: 'Подписка на снижение цены',
    environment: 'kadavr',
    story: {
        'Ecли пользователь не подписан': {
            async beforeEach() {
                await this.browser.setState('marketUtils.data.subscriptions', []);
                await this.browser.yaPageReload(5000, ['state']);
            },
            'блок отображается.': makeCase({
                issue: 'MARKETVERSTKA-33967',
                id: 'marketfront-2650',
                async test() {
                    return this.priceDropOpener.isVisible()
                        .should.eventually.to.be.equal(true, 'Кнопка подписки отображается на странице');
                },
            }),
        },
        'Ecли пользователь уже был подписан': {
            async beforeEach() {
                const modelId = this.params.productId;

                await this.browser.setState('marketUtils.data.subscriptions', [
                    assign({}, CONFIRMED_ADVERTISING_SUBSCRIPTION, {parameters: {modelId}}),
                ]);
                await this.browser.yaPageReload(5000, ['state']);
            },

            'не должен отображаться на странице': makeCase({
                issue: 'MARKETVERSTKA-33967',
                id: 'marketfront-2652',
                test() {
                    return this.priceDropOpener.isVisible()
                        .should.eventually.to.be.equal(false, 'Кнопка подписки не отображается на странице');
                },
            }),
        },
    },
});
