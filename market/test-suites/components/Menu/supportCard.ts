'use strict';

import {mergeSuites, makeSuite, makeCase, PageObject} from 'ginny';

const ManagerCard = PageObject.get('ManagerCard');

/**
 * @param {PageObject.ManagerCard} managerCard - блок менеджера/саппорта в боковом меню
 */
export default makeSuite('Боковое меню. Блок службы поддержки', {
    feature: 'Меню',
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                return this.browser.yaWaitForPageObject(ManagerCard);
            },
        },
        {
            'При просмотре': {
                'отображается корректно': makeCase({
                    id: 'vendor_auto-642',
                    issue: 'VNDFRONT-2790',
                    async test() {
                        await this.allure.runStep('Дожидаемся отображения блока службы поддержки', () =>
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.browser.yaWaitForPageObject(ManagerCard),
                        );

                        await this.managerCard.managerName
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Заголовок блока службы поддержки отображается');

                        await this.managerCard.managerName
                            .getText()
                            .should.eventually.be.equal('Служба поддержки', 'Текст заголовка корректный');

                        await this.managerCard.managerTitle
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Подпись "Ваш менеджер" отсутствует');

                        await this.managerCard.managerAvatar
                            .getAttribute('alt')
                            .should.eventually.be.equal('Служба поддержки', 'Иконка службы поддержки отображается');

                        await this.managerCard.phone
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Номер телефона отсутствует');

                        await this.managerCard.transferLink
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Кнопка "Перевести в поддержку" присутствует');

                        await this.managerCard.becomeManagerLink
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка "Стать менеджером" отсутствует');
                    },
                }),
            },
        },
    ),
});
