'use strict';

import {PageObject, mergeSuites, makeSuite, makeCase, importSuite} from 'ginny';
import moment from 'moment';

const campaignNameLabel = 'Название';
const budgetLabel = 'Бюджет, у. е.';

const ButtonLevitan = PageObject.get('ButtonLevitan');

/**
 * Редактирование кампании
 * @param params
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.PagedList} list - список созданных кампаний
 */
export default makeSuite('Модальное окно с редактированием кампании.', {
    issue: 'VNDFRONT-3313',
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                const {isManager} = this.params;

                this.params.serviceStatusText = isManager
                    ? 'Необходимо подтверждение производителя'
                    : 'Необходимо подтверждение менеджера';

                this.setPageObjects({
                    modal() {
                        return this.createPageObject('Modal');
                    },
                    toast() {
                        return this.createPageObject('NotificationLevitan');
                    },
                    popup() {
                        return this.createPageObject('PopupB2b');
                    },
                    listItem() {
                        return this.createPageObject(
                            'MarketingServicesListItem',
                            this.list,
                            this.list.getItemByIndex(0),
                        );
                    },
                    contextMenu() {
                        return this.createPageObject('SelectAdvanced', this.browser, this.listItem.contextMenu);
                    },
                    form() {
                        return this.createPageObject('MarketingCampaignForm', this.modal.content);
                    },
                    campaignSelect() {
                        return this.createPageObject('SelectAdvanced').setCustomToggler(
                            this.form.getFieldByLabelText('Услуга', 'div//div'),
                        );
                    },
                    categorySuggest() {
                        return this.createPageObject(
                            'Suggest',
                            this.form,
                            this.form.getFieldByLabelText('Категория', 'div'),
                        );
                    },
                    businessModelSelect() {
                        return this.createPageObject(
                            'SelectAdvanced',
                            this.browser,
                            this.form.getFieldByLabelText('Бизнес модель', 'div'),
                        );
                    },
                    datePicker() {
                        return this.createPageObject('DatePicker').setCustomToggler(
                            this.form.getFieldByLabelText('Период проведения', 'div'),
                        );
                    },
                    saveButton() {
                        return this.createPageObject(
                            'ButtonLevitan',
                            this.browser,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.form.elem(ButtonLevitan.getByText('Отправить на подтверждение')),
                        );
                    },
                    cancelButton() {
                        return this.createPageObject(
                            'ButtonLevitan',
                            this.browser,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.form.elem(ButtonLevitan.getByText('Отмена')),
                        );
                    },
                });

                return this.browser.allure.runStep('Открываем модальное окно', async () => {
                    await this.contextMenu.click();
                    await this.popup.waitForPopupShown();
                    await this.contextMenu.selectItem('Изменить условия');

                    await this.modal.waitForVisible();
                    await this.modal.waitForLoading();

                    await this.form
                        .isVisible()
                        .should.eventually.be.equal(true, 'Форма редактирования кампании отображается на странице');

                    await this.campaignSelect
                        .isExisting()
                        .should.eventually.be.equal(true, 'Дропдаун выбора услуги отображается');

                    await this.cancelButton
                        .isExisting()
                        .should.eventually.be.equal(true, 'Кнопка [Отмена] отображается');

                    await this.saveButton
                        .isExisting()
                        .should.eventually.be.equal(true, 'Кнопка [Отправить на подтверждение] отображается');

                    if (isManager) {
                        await this.businessModelSelect
                            .isExisting()
                            .should.eventually.be.equal(true, 'Дропдаун выбора бизнес модели отображается');
                    } else {
                        await this.businessModelSelect
                            .isExisting()
                            .should.eventually.be.equal(false, 'Дропдаун выбора бизнес модели скрыт');
                    }
                });
            },
        },
        importSuite('FinalFormField/textValidate', {
            suiteName: 'Валидация поля "Название".',
            meta: {
                id: 'vendor_auto-1146',
                issue: 'VNDFRONT-3313',
                environment: 'kadavr',
            },
            params: {
                initialValue: 'Привет твоей маме',
                label: campaignNameLabel,
                maxLength: 200,
            },
        }),
        importSuite('FinalFormField/textRequired', {
            suiteName: 'Проверка необходимости заполнить поле "Название".',
            meta: {
                id: 'vendor_auto-1146',
                issue: 'VNDFRONT-3313',
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
                id: 'vendor_auto-1147',
                issue: 'VNDFRONT-3313',
                environment: 'kadavr',
            },
            params: {
                label: budgetLabel,
                errorText: 'Укажите бюджет кампании в у. е.',
            },
        }),
        importSuite('MarketingServicesCampaigns/calendarValidate', {
            meta: {
                id: 'vendor_auto-1148',
                issue: 'VNDFRONT-3313',
                environment: 'kadavr',
            },
            params: {
                label: 'Период проведения',
            },
        }),
        {
            'При попытке выбора селекта с типом услуги': {
                'ничего не происходит, выбор услуги недоступен для редактирования': makeCase({
                    id: 'vendor_auto-1149',
                    test() {
                        return this.campaignSelect
                            .isEnabled()
                            .should.eventually.be.equal(false, 'Выбор услуги недоступен для редактирования');
                    },
                }),
            },
        },
        {
            'После смены категории и бизнес-модели кампании в статусе "Запланирована"': {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {isManager} = this.params;

                    if (isManager) {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.currentTest._meta.id = 'vendor_auto-1215';
                    }
                },
                'кампания не отправляется на повторное подтверждение': makeCase({
                    id: 'vendor_auto-1214',
                    issue: 'VNDFRONT-3883',
                    async test() {
                        const {isManager} = this.params;

                        await this.browser.allure.runStep('Выбираем категорию', async () => {
                            await this.categorySuggest.setFocus();

                            await this.categorySuggest.setText('Моноблоки');

                            await this.popup.waitForPopupShown();

                            await this.categorySuggest.waitForPopupItemsCount(1);

                            return this.categorySuggest.selectItem(0);
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

                                return this.businessModelSelect
                                    .getText()
                                    .should.eventually.be.equal('1P', 'Значение фильтра заполнено выбранным значением');
                            });
                        }

                        await this.saveButton.click();

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.equal(
                                'Условия кампании изменены',
                                'Текст всплывающего сообщения верный',
                            );

                        await this.browser.allure.runStep('Ожидаем закрытия модального окна', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const isVisible = await this.modal.isVisible();

                                    return isVisible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Модальное окно не закрылось',
                            ),
                        );

                        await this.list.waitForLoading();

                        return this.browser.allure.runStep(
                            'Проверяем, что данные кампании сохранились, статус не изменился',
                            async () => {
                                await this.listItem.campaignName
                                    .getText()
                                    .should.eventually.be.equal('Привет твоей маме', 'Название кампании верное');

                                await this.listItem.serviceType
                                    .getText()
                                    .should.eventually.be.equal('Баннеры', 'Тип кампании верный');

                                await this.listItem.interval
                                    .getText()
                                    .should.eventually.be.equal('01.12.2077 – 30.11.2077', 'Период проведения верный');

                                await this.listItem.budget
                                    .getText()
                                    .should.eventually.be.equal('1 005', 'Бюджет кампании верный');

                                return this.listItem.status
                                    .getText()
                                    .should.eventually.be.equal('Запланирована', 'Статус кампании верный');
                            },
                        );
                    },
                }),
            },
        },
        {
            'После редактирования кампании': {
                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {isManager} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.newCampaignName = 'Новое скучное название';

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep(
                        `Вводим данные в поле "${campaignNameLabel}"`,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        () => this.form.setInputValue(campaignNameLabel, this.params.newCampaignName),
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Выбираем категорию', async () => {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.categorySuggest.setFocus();

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.categorySuggest.setText('Моноблоки');

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.popup.waitForPopupShown();

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.categorySuggest.waitForPopupItemsCount(1);

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.categorySuggest.selectItem(0);
                    });

                    if (isManager) {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.browser.allure.runStep('Выбираем бизнес модель', async () => {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            await this.businessModelSelect.click();

                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            await this.popup
                                .waitForPopupShown()
                                .should.eventually.be.equal(true, 'Дропдаун с выбором бизнес модели появился');

                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            await this.businessModelSelect.selectItem('1P');

                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            await this.popup.waitForPopupHidden().should.eventually.be.equal(true, 'Дропдаун исчез');

                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.businessModelSelect
                                .getText()
                                .should.eventually.be.equal('1P', 'Значение фильтра заполнено выбранным значением');
                        });
                    }

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep(
                        `Вводим данные в поле "${budgetLabel}"`,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        () => this.form.setInputValue(budgetLabel, '2'),
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.datePicker.open();

                    const nextMonth = moment().add(1, 'months');

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.datePicker.selectDate(nextMonth);
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.datePicker.selectDate(nextMonth);
                },
                'при клике на кнопку [Отмена] модальное окно закрывается, изменения не сохраняются': makeCase({
                    id: 'vendor_auto-1150',
                    async test() {
                        await this.cancelButton.click();

                        await this.browser.allure.runStep('Ожидаем закрытия модального окна', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const isVisible = await this.modal.isVisible();

                                    return isVisible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Модальное окно не закрылось',
                            ),
                        );

                        await this.list.waitForLoading();

                        return this.browser.allure.runStep(
                            'Проверяем, что данные кампании остались прежними',
                            async () => {
                                await this.listItem.campaignName
                                    .getText()
                                    .should.eventually.be.equal('Привет твоей маме', 'Название кампании верное');

                                await this.listItem.serviceType
                                    .getText()
                                    .should.eventually.be.equal('Баннеры', 'Тип кампании верный');

                                const date = moment({
                                    year: 2077,
                                    month: 11,
                                    day: 1,
                                }).format('DD.MM.YYYY');

                                await this.listItem.interval
                                    .getText()
                                    .should.eventually.be.equal(`${date} – ${date}`, 'Период проведения верный');

                                await this.listItem.budget
                                    .getText()
                                    .should.eventually.be.equal('1 005', 'Бюджет кампании верный');

                                return this.listItem.status
                                    .getText()
                                    .should.eventually.be.equal(
                                        this.params.serviceStatusText,
                                        'Статус кампании верный',
                                    );
                            },
                        );
                    },
                }),
                'при клике на кнопку сохранения модальное окно закрывается, новые данные сохраняются': makeCase({
                    id: 'vendor_auto-1151',
                    async test() {
                        await this.saveButton.click();

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.equal(
                                'Условия кампании изменены',
                                'Текст всплывающего сообщения верный',
                            );

                        await this.browser.allure.runStep('Ожидаем закрытия модального окна', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const isVisible = await this.modal.isVisible();

                                    return isVisible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Модальное окно не закрылось',
                            ),
                        );

                        await this.list.waitForLoading();

                        return this.browser.allure.runStep('Проверяем, что данные кампании изменились', async () => {
                            const nextMonth = moment().add(1, 'months');

                            await this.listItem.campaignName
                                .getText()
                                .should.eventually.be.equal(this.params.newCampaignName, 'Название кампании верное');

                            await this.listItem.serviceType
                                .getText()
                                .should.eventually.be.equal('Баннеры', 'Тип кампании верный');

                            const nextMonthText = nextMonth.format('DD.MM.YYYY');

                            await this.listItem.interval
                                .getText()
                                .should.eventually.be.equal(
                                    `${nextMonthText} – ${nextMonthText}`,
                                    'Период проведения верный',
                                );

                            await this.listItem.budget
                                .getText()
                                .should.eventually.be.equal('2', 'Бюджет кампании верный');

                            return this.listItem.status
                                .getText()
                                .should.eventually.be.equal(this.params.serviceStatusText, 'Статус кампании верный');
                        });
                    },
                }),
            },
        },
    ),
});
