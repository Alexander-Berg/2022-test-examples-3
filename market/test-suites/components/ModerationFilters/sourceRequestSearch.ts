'use strict';

import {PageObject, makeSuite, makeCase} from 'ginny';

const BrandEditRequest = PageObject.get('BrandEditRequest');

export default makeSuite('Корректирующая заявка. Поиск исходной заявки.', {
    id: 'vendor_auto-293',
    environment: 'kadavr',
    issue: 'VNDFRONT-3869',
    feature: 'Модерация',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При клике на номер исходной заявки': {
            'выполняется поиск этой заявки': makeCase({
                async test() {
                    this.setPageObjects({
                        item() {
                            return this.createPageObject('BrandEditRequest', this.list, this.list.getItemByIndex(0));
                        },
                        sourceRequestLink() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('Link', BrandEditRequest.sourceRequest);
                        },
                        requestId() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('TextLevitan', this.item, BrandEditRequest.companyID);
                        },
                    });

                    await this.allure.runStep('Ожидаем появления заявки', () => this.item.waitForExist());

                    await this.moderationList.clickItemByIndex();

                    await this.browser.allure.runStep(
                        'Дожидаемся открытия заявки (появления ссылки на корректирующую заявку)',
                        () => this.sourceRequestLink.waitForExist(),
                    );

                    await this.sourceRequestLink
                        .getText()
                        .should.eventually.be.equal('#48504', 'Текст ссылки корректный');

                    await this.browser.allure.runStep('Кликаем по ссылке с номером корректирующей заявки', () =>
                        this.sourceRequestLink.click(),
                    );

                    await this.filters.searchInput
                        .getValue()
                        .should.eventually.be.equal('48504', 'В текстовом фильтре корректное значение');
                },
            }),
        },
    },
});
