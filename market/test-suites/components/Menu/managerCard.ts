'use strict';

import {mergeSuites, makeSuite, makeCase, PageObject} from 'ginny';

const ManagerCard = PageObject.get('ManagerCard');

const Manager = {
    name: 'Vendors Manager',
    avatar: 'https://s3.mdst.yandex.net/vendors-public/manager-avatars/robot-vendorsmanager.jpg',
    email: 'manager@yandex-team.ru',
    phone: '+74956666666 доб. 6666',
};

/**
 * @param {PageObject.ManagerCard} managerCard - блок менеджера/саппорта в боковом меню
 */
export default makeSuite('Боковое меню. Блок менеджера', {
    feature: 'Меню',
    environment: 'kadavr',
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
                    async test() {
                        await this.managerCard.managerName
                            .getText()
                            .should.eventually.be.equal(Manager.name, 'Имя менеджера отображается');

                        await this.managerCard.managerTitle
                            .getText()
                            .should.eventually.be.equal('Ваш менеджер', 'Подпись "Ваш менеджер" присутствует');

                        await this.managerCard.managerAvatar
                            .getAttribute('src')
                            .should.eventually.be.equal(Manager.avatar, 'Ссылка на изображение верна');

                        await this.managerCard.phone
                            .getText()
                            .should.eventually.be.equal(Manager.phone, 'Номер телефона присутствует');

                        if (this.test._meta.id === 'vendor_auto-641') {
                            await this.managerCard.becomeManagerLink
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка "Стать менеджером" присутствует');
                        }

                        if (this.test._meta.id === 'vendor_auto-744') {
                            await this.managerCard.transferLink
                                .vndIsExisting()
                                .should.eventually.be.equal(false, 'Кнопка "Перевести в поддержку" отсутствует');

                            await this.managerCard.becomeManagerLink
                                .vndIsExisting()
                                .should.eventually.be.equal(false, 'Кнопка "Стать менеджером" отсутствует');
                        }
                    },
                }),
            },
        },
    ),
});
