import {makeSuite, makeCase} from 'ginny';

import COOKIE_NAME from '@self/root/src/constants/cookie';

export default makeSuite('Баннер "Дарим 500 баллов за заказ"', {
    story: {
        'По умолчанию': {
            'баннер отображается': makeCase({
                test() {
                    return this.welcomeCashbackPromoBannerTouch.isExisting()
                        .should.eventually.to.be.equal(
                            true,
                            'Баннер "Дарим 500 баллов за заказ" должен отображаться'
                        );
                },
            }),
            'cсылка "скачать" содержит корректный адрес': makeCase({
                test() {
                    return this.storeLink.getHref()
                        .should.eventually.to.be.link({
                            hostname: 'redirect.appmetrica.yandex.com',
                            pathname: '/serve/99283594781960123',
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
            'ссылка на правила содержит корректный адрес': makeCase({
                test() {
                    return this.rulesLink.getHref()
                        .should.eventually.to.be.link({
                            hostname: 'yandex.ru',
                            pathname: '/legal/market_welcome_cashback',
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
        },
        'При клике': {
            'на крестик ожидаемое поведение': makeCase({
                async test() {
                    await this.bannerCloseButton.click();
                    await this.welcomeCashbackPromoBannerTouch.waitForClosed()
                        .should.eventually.to.be.equal(
                            true,
                            'Баннер "Дарим 500 баллов за заказ" должен закрыться'
                        );
                    const cookie = await this.browser.getCookie(COOKIE_NAME.WELCOME_CASHBACK_TOUCH_SEARCH_BANNER_SHOW);
                    await this.expect(cookie.value).to.be.equal(
                        '1',
                        'Кука для скрытия должнна быть проставлена'
                    );
                },
            }),
        },
    },
});
