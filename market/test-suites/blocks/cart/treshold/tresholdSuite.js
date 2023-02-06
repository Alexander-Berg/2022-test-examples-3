import {
    makeSuite,
    makeCase,
} from 'ginny';
import {yandexPlusPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

import DeliveryThresholdTerms
    from '@self/root/src/widgets/content/cart/CartDeliveryTermsNotifier/components/DeliveryThresholdTerms/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

import {OFFER_PRICE_TRESHOLD_EXCEED, OFFER_PRICE_TRESHOLD_UNREACHED} from './common';
import {ComponentContent} from './constants';

const getCart = (price, hasYaPlus) => buildCheckouterBucket({
    properties: {
        yandexPlusUser: hasYaPlus,
    },
    items: [{
        skuMock: kettle.skuMock,
        offerMock: {
            ...kettle.offerMock,
            prices: {
                currency: 'RUR',
                value: `${price}`,
                isDeliveryIncluded: false,
                rawValue: `${price}`,
                discount: null,
            },
        },
        count: 1,
    }],
});

export default makeSuite('Содержимое компонента', {
    params: {
        isOrderMoreThanTreshold: 'Стоимость товаров в корзине больше трешхолда',
        hasYaPlus: 'У пользователя есть подписка Yandex Plus',
        isAuthWithPlugin: 'Пользователь авторизован',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            const {
                isOrderMoreThanTreshold,
                hasYaPlus,
            } = this.params;

            this.setPageObjects({
                deliveryThresholdTerms: () => this.createPageObject(DeliveryThresholdTerms),
            });

            const cartPrice = isOrderMoreThanTreshold ? OFFER_PRICE_TRESHOLD_EXCEED : OFFER_PRICE_TRESHOLD_UNREACHED;

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [getCart(cartPrice, hasYaPlus)]
            );

            if (hasYaPlus) {
                await this.browser.setState('Loyalty.collections.perks', [yandexPlusPerk]);
            }

            await this.browser.yaScenario(this, waitForCartActualization);
            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);
        },
        'Учитывает подписку YaPlus и трешхолд': makeCase({
            async test() {
                const {
                    isOrderMoreThanTreshold,
                    hasYaPlus,
                } = this.params;

                if (!isOrderMoreThanTreshold && !hasYaPlus) {
                    await this.deliveryThresholdTerms.isExisting()
                        .should.eventually.to.be.equal(
                            false,
                            'Компонент не отображается'
                        );
                }

                if (isOrderMoreThanTreshold && !hasYaPlus) {
                    await this.deliveryThresholdTerms.getDeliveryThresholdTermsText()
                        .should.eventually.to.be.equal(
                            ComponentContent.TRESHOLD_EXCEED_WITH_NO_YAPLUS,
                            'Текст компонента соответствует ожидаемому'
                        );

                    await this.deliveryThresholdTerms.getlinkYaPlusSubscribe()
                        .should.eventually.to.be.link({
                            hostname: 'plus.yandex.ru',
                            pathname: '/',
                            query: {
                                utm_source: 'market',
                                utm_medium: 'link',
                                utm_campaign: 'MSCAMP-77',
                                utm_term: 'src_market',
                                utm_content: 'threshold_block',
                                message: 'market',
                            },
                        }, {
                            skipProtocol: true,
                        });
                }

                if (!isOrderMoreThanTreshold && hasYaPlus) {
                    await this.deliveryThresholdTerms.getDeliveryThresholdTermsText()
                        .should.eventually.to.be.equal(
                            ComponentContent.TRESHOLD_UNREACHED_WITH_YAPLUS,
                            'Текст компонента соответствует ожидаемому'
                        );
                }

                if (isOrderMoreThanTreshold && hasYaPlus) {
                    await this.deliveryThresholdTerms.getDeliveryThresholdTermsText()
                        .should.eventually.to.be.equal(
                            '',
                            'Текст компонента соответствует ожидаемому'
                        );
                }
            },
        }),
    },
});
