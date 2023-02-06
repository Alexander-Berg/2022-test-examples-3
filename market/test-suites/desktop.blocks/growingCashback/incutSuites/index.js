import {makeSuite, mergeSuites, prepareSuite, makeCase} from 'ginny';

// PageObjects
import GrowingCashbackIncutBanner from '@self/root/src/components/GrowingCashbackIncutBanner/__pageObject';

import {createState, ROUTE_PARAMS} from '@self/root/src/spec/hermione/fixtures/growingCashbackIncut';

const DISTRIBUTION_LINK = 'https://market.yandex.ru/special/growing-cashback';

async function prepareState({isPromoAvailable, viewType}) {
    await this.browser.setState('report', createState(isPromoAvailable, DISTRIBUTION_LINK));

    const pageId = viewType === 'list' ? 'market:list' : 'market:catalog';

    return this.browser.yaOpenPage(pageId, {
        ...ROUTE_PARAMS,
        viewtype: viewType,
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
                    () => this.growingCashbackIncutBanner.isExisting()
                        .should.eventually.to.be.equal(
                            this.params.shouldBeShown,
                            `Врезка акции "Растущего кешбэка" ${this.params.shouldBeShown ? '' : 'не'} должна отображаться`
                        )
                );

                if (this.params.shouldBeShown) {
                    await this.browser.allure.runStep(
                        'Проверяем текст врезки',
                        () => this.growingCashbackIncutBanner.getIncutText()
                            .should.eventually.to.be.equal(
                                'Получите 1 550 баллов Плюса\nза первые 3 заказа в приложении',
                                'Врезка должна содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст бейджа с баллами',
                        () => this.growingCashbackIncutBanner.getBadgeText()
                            .should.eventually.to.be.equal(
                                '1 550',
                                'Бейдж с баллами должен содержать корректное значение'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст кнопки перехода на лэндинг',
                        () => this.growingCashbackIncutBanner.getDetailedButtonText()
                            .should.eventually.to.be.equal(
                                'Подробнее'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку на лэндинг',
                        () => this.growingCashbackIncutBanner.getDetailedButtonLink()
                            .should.eventually.to.be.link({
                                pathname: '/special/growing-cashback',
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
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
    id: 'marketfront-5298',
    params: {
        viewType: 'Страница каталога листовая или гридовая',
    },
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                growingCashbackIncutBanner: () => this.createPageObject(GrowingCashbackIncutBanner),
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
                const {viewType} = this.params;

                await prepareState.call(this, {isPromoAvailable: true, viewType});
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
                const {viewType} = this.params;

                await prepareState.call(this, {isPromoAvailable: false, viewType});
            },
        },
    })),
});
