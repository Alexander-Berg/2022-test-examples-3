import {makeSuite, makeCase} from 'ginny';

import {
    growingCashbackPerk,
    growingCashbackFullRewardPerk,
} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
// pageObject
import GrowingCashbackPromoBanner from '@self/root/src/components/GrowingCashbackPromoBanner/__pageObject';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {offerMock, skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareCartPageBySkuId} from '@self/root/src/spec/hermione/scenarios/cart';

export const bannerSuite = makeSuite('Баннер "Растущий кешбэк"', {
    params: {
        shouldBeShown: 'Должен ли показываться баннер',
        isPromoAvailable: 'Доступна ли акция для пользователя',
        isGotFullReward: 'Набрал ли пользователь максимально количество баллов',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                growingCashbackPromoBanner: () => this.createPageObject(GrowingCashbackPromoBanner),
            });

            let perks = [];

            if (this.params.isPromoAvailable) {
                perks = this.params.isGotFullReward ? [growingCashbackFullRewardPerk] : [growingCashbackPerk];
            }

            await this.browser.setState('Loyalty.collections.perks', perks);
            await this.browser.setState('S3Mds.files', {
                '/toggles/all_growing-cashback-web.json': {
                    id: 'all_growing-cashback-web',
                    value: true,
                },
            });

            const offer = createOffer(offerMock, offerMock.wareId);
            const reportState = mergeState([offer]);

            const sku = {
                ...skuMock,
                offers: {
                    items: [offerMock],
                },
            };

            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock,
                        offerMock,
                        count: 1,
                    }],
                }),
            ];

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                carts,
                {existingReportState: reportState}
            );

            await this.browser.yaScenario(
                this,
                prepareCartPageBySkuId,
                {
                    items: [{
                        skuId: skuMock.id,
                        offerId: offerMock.wareId,
                        count: 1,
                    }],
                    region: 213,
                    reportSkus: [sku],
                }
            );
        },
        'Содержит корректный контент': makeCase({
            async test() {
                const {shouldBeShown} = this.params;

                await this.browser.allure.runStep('Проверяем наличие баннера',
                    () => this.growingCashbackPromoBanner.isVisible()
                        .should.eventually.to.be.equal(
                            shouldBeShown,
                            `Баннер "Растущий кешбэк" ${shouldBeShown ? '' : 'не'} должна отображаться`
                        )
                );

                if (shouldBeShown) {
                    await this.browser.allure.runStep(
                        'Проверяем текст заголовка',
                        () => this.growingCashbackPromoBanner.getTitleText()
                            .should.eventually.to.be.equal(
                                '1 550 баллов Плюса',
                                'Заголовок должен содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст подзаголовка',
                        () => this.growingCashbackPromoBanner.getSubtitleText()
                            .should.eventually.to.be.equal(
                                'За первые 3 заказа от 3 500 ₽ в приложении',
                                'Подзаголовок должен содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст кнопки перехода в приложения',
                        () => this.growingCashbackPromoBanner.getAppButtonText()
                            .should.eventually.to.be.equal(
                                'Открыть'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку кнопки перехода в приложения',
                        () => this.growingCashbackPromoBanner.getAppButtonLink()
                            .should.eventually.be.link({
                                hostname: 'nquw.adj.st',
                                pathname: '/',
                                query: {
                                    adj_t: '6pjqmbb',
                                    adj_campaign: 'growing_cashback_banner_in_cart',
                                    adj_adgroup: 'touch:cart',
                                },
                            }, {
                                skipProtocol: true,
                            })
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку на условия',
                        () => this.growingCashbackPromoBanner.getRulesLink()
                            .should.eventually.be.link({
                                pathname: '/special/growing-cashback-legal',
                            }, {
                                skipHostname: true,
                                skipProtocol: true,
                            })
                    );

                    // кликаем по кнопке закрытия баннера
                    await this.growingCashbackPromoBanner.clickCloseButton();

                    await this.browser.allure.runStep('Проверяем, что баннер не отображается на странице',
                        () => this.growingCashbackPromoBanner.waitForVisible(1000, true)
                    );
                }
            },
        }),
    },
});
