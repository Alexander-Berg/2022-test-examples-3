'use strict';

import {mergeSuites, makeCase, makeSuite, importSuite} from 'ginny';
import moment from 'moment';

const campaignNameLabel = 'Название';
const budgetLabel = 'Бюджет, у. е.';

/**
 * Тесты на создание маркетинговых кампаний
 * @param params
 * @param {string} params.marketingServicesPageUrl - url страницы со списком кампаний
 * @param {string} params.serviceStatusText - текст статуса маркетинговой кампании
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.SelectAdvanced} campaignSelect - дропдаун выбора услуги
 * @param {PageObject.Suggest} categorySuggest - саджест выбора категории
 * @param {PageObject.PopupB2b} popup - селект выбора услуги
 * @param {PageObject.MarketingCampaignForm} form - форма создания кампании
 * @param {PageObject.DatePicker} datePicker - контрол выбора периода проведения кампании
 * @param {PageObject.PagedList} list - список маркетинговых кампаний
 * @param {PageObject.MarketingServicesListItem} listItem - маркетинговая кампания из списка
 * @param {PageObject.SelectAdvanced} businessModelSelect - выпадающий список выбора бизнес модели
 */
export default makeSuite('Форма создания кампании', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.form
                    .isVisible()
                    .should.eventually.be.equal(true, 'Форма создания кампании отображается на странице');
            },
        },
        {
            'При открытии страницы': {
                'есть доступ к созданию только той маркетинговой кампании,которая доступна пользователю': makeCase({
                    id: 'vendor_auto-1136',
                    issue: 'VNDFRONT-3300',
                    async test() {
                        await this.campaignSelect
                            .isExisting()
                            .should.eventually.be.equal(true, 'Контрол выбора услуги присутствует');

                        await this.browser.vndScrollToTop();

                        await this.browser.allure.runStep(
                            'Выбираем единственную доступную услугу "Баннеры"',
                            async () => {
                                await this.campaignSelect.click();

                                await this.popup
                                    .waitForPopupShown()
                                    .should.eventually.be.equal(true, 'Дропдаун с выбором услуги появился');

                                await this.campaignSelect.selectItem('Баннеры');

                                await this.popup
                                    .waitForPopupHidden()
                                    .should.eventually.be.equal(true, 'Дропдаун исчез');

                                await this.campaignSelect
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Баннеры',
                                        'Значение фильтра заполнено выбранным значением',
                                    );
                            },
                        );

                        await this.browser.allure.runStep(
                            'Проверяем, что остальные услуги недоступны для выбора',
                            async () => {
                                await this.campaignSelect.click();

                                await this.popup
                                    .waitForPopupShown()
                                    .should.eventually.be.equal(true, 'Дропдаун с выбором услуги появился');

                                await this.campaignSelect
                                    .isItemByTitleDisabled('Лендинг')
                                    .should.eventually.be.equal(true, 'Услуга "Лендинг" недоступна');

                                await this.campaignSelect
                                    .isItemByTitleDisabled('Участие в промоакциях')
                                    .should.eventually.be.equal(true, 'Услуга "Лендинг" недоступна');

                                await this.campaignSelect
                                    .isItemByTitleDisabled('Рассылки')
                                    .should.eventually.be.equal(true, 'Услуга "Рассылки" недоступна');

                                await this.campaignSelect
                                    .isItemByTitleDisabled('Страница магазина')
                                    .should.eventually.be.equal(true, 'Услуга "Страница магазина" недоступна');

                                await this.campaignSelect
                                    .isItemByTitleDisabled('Брендирование')
                                    .should.eventually.be.equal(true, 'Услуга "Брендирование" недоступна');

                                await this.campaignSelect
                                    .isItemByTitleDisabled('Размещение логотипа')
                                    .should.eventually.be.equal(true, 'Услуга "Размещение логотипа" недоступна');
                            },
                        );
                    },
                }),
            },
        },
        importSuite('FinalFormField/textValidate', {
            suiteName: 'Валидация поля "Название".',
            meta: {
                id: 'vendor_auto-1138',
                issue: 'VNDFRONT-3312',
                environment: 'kadavr',
            },
            params: {
                label: campaignNameLabel,
                maxLength: 200,
            },
        }),
        importSuite('FinalFormField/textRequired', {
            suiteName: 'Проверка необходимости заполнить поле "Название".',
            meta: {
                id: 'vendor_auto-1138',
                issue: 'VNDFRONT-3312',
                environment: 'kadavr',
            },
            params: {
                label: campaignNameLabel,
                errorText: 'Напишите название кампании',
            },
        }),
        importSuite('FinalFormField/numberValidate', {
            suiteName: 'Проверка валидации поля "Бюджет".',
            meta: {
                id: 'vendor_auto-1139',
                issue: 'VNDFRONT-3312',
                environment: 'kadavr',
            },
            params: {
                label: budgetLabel,
                errorText: 'Укажите бюджет кампании в у. е.',
            },
        }),
        importSuite('MarketingServicesCampaigns/calendarValidate', {
            meta: {
                id: 'vendor_auto-1140',
                issue: 'VNDFRONT-3312',
                environment: 'kadavr',
            },
            params: {
                label: 'Период проведения',
            },
        }),
        {
            'При вводе валидных параметров': {
                'кампания успешно создается': makeCase({
                    id: 'vendor_auto-1144',
                    issue: 'VNDFRONT-3312',
                    async test() {
                        const {isManager} = this.params;

                        await this.browser.allure.runStep(`Вводим данные в поле "${campaignNameLabel}"`, () =>
                            this.form.setInputValue(campaignNameLabel, 'Твоя мамка'),
                        );

                        await this.browser.allure.runStep('Выбираем категорию', async () => {
                            await this.categorySuggest.setFocus();

                            await this.categorySuggest.setText('Моноблоки');

                            await this.popup.waitForPopupShown();

                            await this.categorySuggest.waitForPopupItemsCount(1);

                            await this.categorySuggest.selectItem(0);
                        });

                        if (isManager) {
                            await this.browser.allure.runStep('Выбираем бизнес модель', async () => {
                                await this.businessModelSelect.click();

                                await this.popup
                                    .waitForPopupShown()
                                    .should.eventually.be.equal(true, 'Дропдаун с выбором бизнес модели появился');

                                await this.businessModelSelect.selectItem('1P');

                                await this.popup
                                    .waitForPopupHidden()
                                    .should.eventually.be.equal(true, 'Дропдаун исчез');

                                await this.businessModelSelect
                                    .getText()
                                    .should.eventually.be.equal('1P', 'Значение фильтра заполнено выбранным значением');
                            });
                        } else {
                            await this.businessModelSelect
                                .isExisting()
                                .should.eventually.be.equal(false, 'Дропдаун выбора бизнес модели скрыт');
                        }

                        await this.browser.allure.runStep(`Вводим данные в поле "${budgetLabel}"`, () =>
                            this.form.setInputValue(budgetLabel, '1'),
                        );

                        await this.browser.allure.runStep('Выбираем услугу "Баннеры"', async () => {
                            await this.campaignSelect.click();

                            await this.popup
                                .waitForPopupShown()
                                .should.eventually.be.equal(true, 'Дропдаун с выбором услуги появился');

                            await this.browser.vndScrollToTop();

                            await this.campaignSelect.selectItem('Баннеры');

                            await this.popup.waitForPopupHidden().should.eventually.be.equal(true, 'Дропдаун исчез');

                            await this.campaignSelect
                                .getText()
                                .should.eventually.be.equal(
                                    'Баннеры',
                                    'Значение фильтра заполнено выбранным значением',
                                );
                        });

                        await this.datePicker.open();

                        await this.datePicker.selectDate(moment());

                        await this.browser
                            .vndWaitForChangeUrl(() => this.form.clickSubmitButton())
                            .should.eventually.be.link(this.params.marketingServicesPageUrl, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });

                        await this.list.waitForLoading();

                        await this.listItem.isVisible().should.eventually.be.equal(true, 'Кампания появилась в списке');

                        await this.listItem.status
                            .getText()
                            .should.eventually.be.equal(this.params.serviceStatusText, 'Статус кампании верный');
                    },
                }),
            },
        },
    ),
});
