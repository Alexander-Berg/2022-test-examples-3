
import {makeSuite, makeCase} from 'ginny';
import {
    skuMock as skuKettle,
    offerKettleBnpl,
} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import COOKIE_CONSTANTS from '@self/root/src/constants/cookie';
import {BNPL_PRICE_LABEL} from '@self/root/src/entities/bnplInfo/constants';
import BnplSwitch from '@self/root/src/components/BnplSwitch/__pageObject';

export const bnplWidgetSuite = makeSuite('Блок бнпл.', {
    feature: 'БНПЛ',
    id: 'bluemarket-4090',
    issue: 'MARKETFRONT-56000',
    environment: 'kadavr',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        item: {
            sku: skuKettle,
            offer: offerKettleBnpl,
            count: 1,
        },
        firstOrder: false,
        isAuthWithPlugin: true,
        cookie: {
            [COOKIE_CONSTANTS.EXP_FLAGS]: {
                name: COOKIE_CONSTANTS.EXP_FLAGS,
                value: 'all_bnpl=on',
            },
        },
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                bnplSwitch: () => this.createPageObject(BnplSwitch),
            });
        },
        'Содержит ожидаемые данные.': makeCase({
            async test() {
                await this.bnplInfo.isVisible().should.eventually.be.equal(
                    true,
                    'Блок бнпл должен быть виден'
                );

                await this.bnplInfo.getPayNowText().should.eventually.be.match(
                    /[\d| ]*₽ сегодня/,
                    'Правильный текст про оплату сегодня'
                );

                await this.bnplInfo.getPayLaterText().should.eventually.be.match(
                    /и[\d| ]*₽ потом/,
                    'Правильный текст про оплату потом'
                );

                await this.bnplInfo.chart.isVisible().should.eventually.be.equal(
                    true,
                    'График бнпл должен быть виден'
                );

                await this.bnplInfo.button.isVisible().should.eventually.be.equal(
                    true,
                    'Кнопка "Оформить" должна быть видна'
                );

                await this.bnplInfo.detailsLink.isVisible().should.eventually.be.equal(
                    true,
                    'Ссылка "Подробнее" должна быть видна'
                );
            },
        }),
        'Переходим по "Оформить".': makeCase({
            async test() {
                await this.bnplInfo.button.click();
                await this.confirmationPage.waitForVisible(5000);
                await this.bnplSwitch.isVisible().should.eventually.be.equal(
                    true,
                    'Переключатель bnpl должен быть виден'
                );
                await this.bnplSwitch.checkbox.getAttribute('checked').should.eventually.be.equal(
                    'true',
                    'Переключатель bnpl должен быть включен'
                );
            },
        }),
    },
});

export const bnplPriceLabelSuite = makeSuite('Подпись около цены.', {
    feature: 'БНПЛ',
    id: 'bluemarket-4090',
    issue: 'MARKETFRONT-56000',
    environment: 'kadavr',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        item: {
            sku: skuKettle,
            offer: offerKettleBnpl,
            count: 1,
        },
        firstOrder: false,
        isAuthWithPlugin: true,
        cookie: {
            [COOKIE_CONSTANTS.EXP_FLAGS]: {
                name: COOKIE_CONSTANTS.EXP_FLAGS,
                value: 'all_bnpl=on',
            },
        },
    },
    story: {
        'Содержит ожидаемые данные.': makeCase({
            async test() {
                await this.defaultOffer.bnplLabel.isVisible().should.eventually.be.equal(
                    true,
                    'Подпись у цены должна быть видна'
                );
                await this.defaultOffer.bnplLabel.getText().should.eventually.be.equal(
                    BNPL_PRICE_LABEL,
                    'Текст подписи должен быть правильным'
                );
            },
        }),
    },
});
