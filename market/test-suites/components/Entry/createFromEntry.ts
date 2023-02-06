'use strict';

import {makeSuite, makeCase} from 'ginny';

const NEW_VENDOR_NAME = 'KADAVR Заявка с приятной офертой';
const NEW_BRAND_NAME = 'Sony Creative (12804226)';
const ENTRY_PARAMS = {
    site: {
        value: 'сайт',
    },
    name: {
        value: 'Фронт',
    },
    phone: {
        value: '+79210000000',
    },
    email: {
        value: 'auto@test',
    },
    login: {
        value: 'auto-vendor',
    },
    address: {
        value: 'benya, 198504',
    },
};

/**
 * @param {PageObject.Entry} item – элемент списка заявок
 * @param {PageObject.Form} form - форма - блок заявки с полями из анкеты нового вендора
 * @param {PageObject.FinalForm} campaignsForm - форма со страницы "Добавление партнера"
 * @param {PageObject.Checkbox} checkbox - чекбокс с принятой офертой на добавлении партнера
 * @param {Object} params - параметры тест-сьюта
 * @param {string} params.expectedPath - url, который должен быть при переходе на страницу добавления партнера
 */
export default makeSuite('Создание карточки из заявки.', {
    feature: 'Заявки',
    params: {
        user: 'Пользователь',
    },
    environment: 'kadavr',
    story: {
        'При нажатии на кнопку "Создать карточку"': {
            'осуществляется переход на страницу добавления партнера': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Проверяем, что в заявке отображаются заполнены поля анкеты нового вендора',
                        async () => {
                            await this.form
                                .getReadonlyFieldByName('site')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Сайт" отображается');

                            await this.form
                                .getReadonlyFieldValue('site')
                                .should.eventually.be.equal(ENTRY_PARAMS.site.value, 'Поле "Сайт" заполнено');

                            await this.form
                                .getReadonlyFieldByName('name')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Контактное лицо" отображается');

                            await this.form
                                .getReadonlyFieldValue('name')
                                .should.eventually.be.equal(
                                    ENTRY_PARAMS.name.value,
                                    'Поле "Контактное лицо" заполнено',
                                );

                            await this.form
                                .getReadonlyFieldByName('phone')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Телефон" отображается');

                            await this.form
                                .getReadonlyFieldValue('phone')
                                .should.eventually.be.equal(ENTRY_PARAMS.phone.value, 'Поле "Телефон" заполнено');

                            await this.form
                                .getReadonlyFieldByName('email')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Эл. почта" отображается');

                            await this.form
                                .getReadonlyFieldValue('email')
                                .should.eventually.be.equal(ENTRY_PARAMS.email.value, 'Поле "Эл. почта" заполнено');

                            await this.form
                                .getReadonlyFieldByName('login')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Логин" отображается');

                            await this.form
                                .getReadonlyFieldValue('login')
                                .should.eventually.be.equal(ENTRY_PARAMS.login.value, 'Поле "Логин" заполнено');

                            await this.form
                                .getReadonlyFieldByName('address')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Адрес" отображается');

                            await this.form
                                .getReadonlyFieldValue('address')
                                .should.eventually.be.equal(ENTRY_PARAMS.address.value, 'Поле "Адрес" заполнено');

                            await this.form
                                .getDocumentUploadFieldByName('trademarkDocuments')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Свидетельства на товарный знак" заполнено');

                            await this.form
                                .getDocumentUploadFieldByName('guaranteeLetters')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Гарантийные письма" заполнено');
                        },
                    );

                    await this.browser.allure.runStep(
                        'Проверяем, что сматчившийся бренд отображается корректно',
                        async () => {
                            await this.item.brand
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Бренд отображается');

                            await this.item
                                .getBrandNameText()
                                .should.eventually.be.equal(NEW_BRAND_NAME, 'Название бренда заполнено');

                            await this.item.createCampaignButton
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка [Создать карточку] отображается');
                        },
                    );

                    await this.item.createCampaignAndVendorButton
                        .vndIsExisting()
                        .should.eventually.be.equal(true, 'Кнопка [Создать карточку и задать вендора] отображается');

                    // хак, т.к. тесты отличаются только кнопкой, которую надо нажать, и одной проверкой
                    if (this.test._meta.id === 'vendor_auto-247') {
                        await this.item.clickCreateCampaignButton();
                    }

                    if (this.test._meta.id === 'vendor_auto-248') {
                        await this.item.clickCreateCampaignAndVendorButton();
                    }

                    await this.browser.allure.runStep(
                        'В новой вкладке открылась форма создания карточки /new с корректными query-параметрами',
                        () =>
                            this.browser.getUrl().should.eventually.be.link(this.params.expectedPath, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            }),
                    );

                    await this.browser.allure.runStep('Дожидаемся открытия формы создания карточки', () =>
                        this.campaignsForm.waitForExist(),
                    );

                    await this.browser.allure.runStep(
                        'Проверяем, что поля информации о компании предзаполнены данными из заявки',
                        async () => {
                            const {site, name, phone, email, login, address} = ENTRY_PARAMS;

                            await this.campaignsForm
                                .getInputByLabelText('Компания-заявитель')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Компания-заявитель" отображается');

                            await this.campaignsForm
                                .getInputValue('Компания-заявитель')
                                .should.eventually.be.equal(NEW_VENDOR_NAME, 'Поле "Компания-заявитель" заполнено');

                            await this.campaignsForm
                                .getInputByLabelText('Ссылка на официальный сайт компании')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Поле "Ссылка на официальный сайт компании" отображается',
                                );

                            await this.campaignsForm
                                .getInputValue('Ссылка на официальный сайт компании')
                                .should.eventually.be.equal(
                                    site.value,
                                    'Поле "Ссылка на официальный сайт компании" заполнено',
                                );

                            await this.campaignsForm
                                .getInputByLabelText('Контактное лицо')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Контактное лицо" отображается');

                            await this.campaignsForm
                                .getInputValue('Контактное лицо')
                                .should.eventually.be.equal(
                                    name.value,
                                    'Поле "Контактное лицо" заполнено и отображается',
                                );

                            await this.campaignsForm
                                .getInputByLabelText('Телефон')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Телефон" отображается');

                            await this.campaignsForm
                                .getInputValue('Телефон')
                                .should.eventually.be.equal(phone.value, 'Поле "Телефон" заполнено');

                            await this.campaignsForm
                                .getInputByLabelText('Электронная почта')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Электронная почта" отображается');

                            await this.campaignsForm
                                .getInputValue('Электронная почта')
                                .should.eventually.be.equal(email.value, 'Поле "Электронная почта" заполнено');

                            await this.campaignsForm
                                .getFieldByLabelText('Логин')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Логин" отображается');

                            await this.campaignsForm
                                .getReadonlyFieldValue('Логин')
                                .should.eventually.be.equal(login.value, 'Поле "Логин" заполнено');

                            await this.campaignsForm
                                .getInputByLabelText('Почтовый адрес с индексом')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Почтовый адрес с индексом" отображается');

                            await this.campaignsForm
                                .getInputValue('Почтовый адрес с индексом')
                                .should.eventually.be.equal(
                                    address.value,
                                    'Поле "Почтовый адрес с индексом" заполнено',
                                );

                            await this.campaignsForm
                                .getReadonlyDocumentField('Свидетельства на товарный знак')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Свидетельства на товарный знак" заполнено');

                            await this.campaignsForm
                                .getReadonlyDocumentField('Гарантийные письма на фирменных бланках правообладателя')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Поле "Гарантийные письма" заполнено');

                            await this.checkbox.icon
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Чекбокс с принятой офертой отображается');
                        },
                    );

                    await this.campaignsForm
                        .getInputByLabelText('Бренд')
                        .vndIsExisting()
                        .should.eventually.be.equal(true, 'Поле "Бренд" отображается');

                    if (this.test._meta.id === 'vendor_auto-247') {
                        await this.campaignsForm
                            .getInputValue('Бренд')
                            .should.eventually.be.equal(NEW_BRAND_NAME, 'Поле "Бренд" предзаполнено');
                    }

                    if (this.test._meta.id === 'vendor_auto-248') {
                        await this.campaignsForm
                            .getInputValue('Бренд')
                            .should.eventually.be.equal('', 'Поле "Бренд" не заполнено');
                    }
                },
            }),
        },
    },
});
