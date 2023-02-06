/* eslint-disable no-unreachable */

import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import schema from 'js-schema';
import nodeConfig from '@self/platform/configs/current/node';

import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';
/**
 * Тест на уценку минивизитке КМ на вкладке Цены
 * @property {PageObject.ProductTabs} productTabs - табы навигации на КМ
 */
export default makeSuite('Минивизитка на вкладке "Цены"', {
    story: mergeSuites(
        makeSuite('Кнопка "В магазин"', {
            story: prepareSuite(MetricaClickSuite, {
                meta: {
                    id: 'marketfront-3282',
                    issue: 'MARKETVERSTKA-33040',
                },
                hooks: {
                    async beforeEach() {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('MARKETFRONT-40812 скипаем упавшие тесты ' +
                            'т к были оторваны табы КМ и потерялась точка входа');

                        await this.clickoutButton.waitForVisible();

                        this.params.selector = await this.clickoutButton.getSelector();
                    },
                },
                params: {
                    counterId: nodeConfig.yaMetrika.market.id,
                    expectedGoalName: 'product-offers-page_default-offer_to-shop_go-to-shop',
                    payloadSchema: schema({
                        isCutPrice: Boolean,
                    }),
                },
            }),
        })
    ),
});
