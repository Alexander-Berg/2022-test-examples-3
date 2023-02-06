import {makeSuite, mergeSuites, prepareSuite, makeCase} from 'ginny';

// PageObjects
import GrowingCashbackPromoBanner from '@self/root/src/components/GrowingCashbackPromoBanner/__pageObject';

import {createState, ROUTE_PARAMS} from '@self/root/src/spec/hermione/fixtures/growingCashbackIncut';

const DISTRIBUTION_LINK = 'https://nquw.adj.st?adj_t=k9n4hks&adj_campaign=Touch_Product_list_Plus_growing';

async function prepareState({isPromoAvailable}) {
    await this.browser.setState('report', createState(isPromoAvailable, DISTRIBUTION_LINK));

    return this.browser.yaOpenPage('touch:list', {
        ...ROUTE_PARAMS,
    });
}

const suite = makeSuite('Врезка акции "Растущего кешбэка"', {
    params: {
        shouldBeShown: 'Должна ли показываться врезка',
    },
    story: {
        'Содержит корректный контент': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, отображение на странице',
                    () => this.growingCashbackPromoBanner.isExisting()
                        .should.eventually.to.be.equal(
                            this.params.shouldBeShown,
                            `Врезка акции "Растущего кешбэка" ${this.params.shouldBeShown ? '' : 'не'} должна отображаться`
                        )
                );

                if (this.params.shouldBeShown) {
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
                                    adj_t: 'k9n4hks',
                                    adj_campaign: 'Touch_Product_list_Plus_growing',
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
                }
            },
        }),
    },
});

export default makeSuite('Врезка акции "Растущего кешбэка"', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-72774',
    feature: 'Растущий кешбэк',
    id: 'marketfront-5304',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                growingCashbackPromoBanner: () => this.createPageObject(GrowingCashbackPromoBanner),
            });

            await this.browser.setState('S3Mds.files', {
                '/toggles/all_growing-cashback-web.json': {
                    id: 'all_growing-cashback-web',
                    value: true,
                },
            });
        },
    },
    prepareSuite(suite, {
        suiteName: 'Акция доступна',
        params: {
            shouldBeShown: true,
        },
        hooks: {
            async beforeEach() {
                await prepareState.call(this, {isPromoAvailable: true});
            },
        },
    }),
    prepareSuite(suite, {
        suiteName: 'Акция не доступна',
        params: {
            shouldBeShown: false,
        },
        hooks: {
            async beforeEach() {
                await prepareState.call(this, {isPromoAvailable: false});
            },
        },
    })),
});
