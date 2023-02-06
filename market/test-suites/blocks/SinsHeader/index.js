import {makeSuite, makeCase} from '@yandex-market/ginny';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Шапка магазина', {
    story: {
        'По-умолчанию': {
            'Имеет корректную ссылку в логотипе': makeCase({
                id: 'marketfront-4356',
                async test() {
                    return this.browser.allure.runStep(
                        'Проверяем ссылку в логотипе',
                        async () => {
                            const titleLogoLink = await this.sinsHeader.getTitleLogoLink();

                            return this.browser.expect(titleLogoLink)
                                .to.be.link(
                                    this.params.expectedTitleLogoLink,
                                    {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }
                                );
                        }
                    );
                },
            }),
            'Можно нажать на баблы про экспресс-доставку(1), рейтинг(2), график работы(3) и увидеть модальные окна с детальной информацией': makeCase({
                id: 'marketfront-5388',
                async test() {
                    await this.expressDelivery.clickButton();
                    await this.expressDelivery.waitForPopupVisible();
                    const isExpressDeliveryPopupVisible = await this.expressDelivery.isPopupVisible();
                    await this.expressDelivery.clickOutsidePopup();
                    await this.expressDelivery.waitForButtonVisible();

                    await this.rating.clickButton();
                    await this.rating.waitForPopupVisible();
                    const isRatingPopupVisible = await this.rating.isPopupVisible();
                    await this.rating.clickOutsidePopup();
                    await this.rating.waitForButtonVisible();

                    await this.schedule.clickButton();
                    await this.schedule.waitForPopupVisible();
                    const isSchedulePopupVisible = await this.schedule.isPopupVisible();

                    const allPopupsWereDisplayed = isExpressDeliveryPopupVisible && isRatingPopupVisible && isSchedulePopupVisible;

                    return this.browser.expect(allPopupsWereDisplayed).to.be.equal(true, 'Все модальные окна отобразились');
                },
            }),
        },
    },
});
