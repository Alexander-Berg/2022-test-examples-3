'use strict';

import {makeCase, makeSuite, PageObject} from 'ginny';

const TextLevitan = PageObject.get('TextLevitan');
const ButtonLevitan = PageObject.get('ButtonLevitan');
const ProductInfoLabel = PageObject.get('ProductInfoLabel');
const OpinionsPromotionList = PageObject.get('OpinionsPromotionList');

/**
 * Тесты на работу масс-селекта
 *
 * @param {PageObject.PopupB2b} popup - выпадашка для масс-селекта
 * @param {PageObject.OpinionsPromotionList} list - таблица с товарами с отзывами за баллы
 * @param {PageObject.OpinionsPromotionAgitationStats} agitationStats - блок со статистикой по отзывам за баллы
 */
export default makeSuite('Масс-селект.', {
    issue: 'VNDFRONT-4000',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        async beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                agitationStatsSpinner() {
                    return this.createPageObject('SpinnerLevitan', this.agitationStats);
                },
                listHeader() {
                    return this.createPageObject('OpinionsPromotionListHeader', this.list);
                },
                firstItem() {
                    return this.createPageObject('OpinionsPromotionListItem', this.list, this.list.getItemByIndex(0));
                },
                firstItemCheckbox() {
                    return this.createPageObject('CheckboxB2b', this.firstItem.checkBoxColumn);
                },
                firstItemCashbackInput() {
                    return this.createPageObject('InputB2b', this.firstItem.cashbackContainer);
                },
                firstItemTargetInput() {
                    return this.createPageObject('InputB2b', this.firstItem.targetOpinionsContainer);
                },
                secondItem() {
                    return this.createPageObject('OpinionsPromotionListItem', this.list, this.list.getItemByIndex(1));
                },
                secondItemCheckbox() {
                    return this.createPageObject('CheckboxB2b', this.secondItem.checkBoxColumn);
                },
                secondItemCashbackInput() {
                    return this.createPageObject('InputB2b', this.secondItem.cashbackContainer);
                },
                secondItemTargetInput() {
                    return this.createPageObject('InputB2b', this.secondItem.targetOpinionsContainer);
                },
                thirdItem() {
                    return this.createPageObject('OpinionsPromotionListItem', this.list, this.list.getItemByIndex(2));
                },
                thirdItemCheckbox() {
                    return this.createPageObject('CheckboxB2b', this.thirdItem.checkBoxColumn);
                },
                thirdItemCashbackInput() {
                    return this.createPageObject('InputB2b', this.thirdItem.cashbackContainer);
                },
                thirdItemTargetInput() {
                    return this.createPageObject('InputB2b', this.thirdItem.targetOpinionsContainer);
                },
                massSelect() {
                    return this.createPageObject('SelectB2b', this.listHeader);
                },
                selectedModelsCount() {
                    return this.createPageObject(
                        'TextLevitan',
                        this.list.header,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        OpinionsPromotionList.selectedModelsText,
                    );
                },
                showModalButton() {
                    return this.createPageObject('ButtonLevitan', this.list, this.list.massSelectShowModalButton);
                },
                cancelPromotionButton() {
                    return this.createPageObject('ButtonLevitan', this.list, this.list.massSelectCancelButton);
                },
                modal() {
                    return this.createPageObject('Modal');
                },
                modalCashBackField() {
                    return this.createPageObject('OpinionsPromotionCashBackField', this.modal);
                },
                modalTargetOpinionsCountField() {
                    return this.createPageObject('OpinionsPromotionTargetOpinionsCountField', this.modal);
                },
                modalCashbackInput() {
                    return this.createPageObject('InputB2b', this.modalCashBackField);
                },
                modalTargetInput() {
                    return this.createPageObject('InputB2b', this.modalTargetOpinionsCountField);
                },
                modalSubmitButton() {
                    return this.createPageObject('ButtonLevitan', this.modal, `${ButtonLevitan.root}:nth-child(1)`);
                },
                toasts() {
                    return this.createPageObject('NotificationGroupLevitan');
                },
                toast() {
                    return this.createPageObject('NotificationLevitan', this.toasts, this.toasts.getItemByIndex(0));
                },
                agitationStatsModelsCount() {
                    return this.createPageObject(
                        'ProductInfoLabel',
                        this.agitationStats,
                        `${ProductInfoLabel.root}:nth-child(1)`,
                    );
                },
                agitationStatsModelsCountText() {
                    return this.createPageObject(
                        'TextLevitan',
                        this.agitationStatsModelsCount,
                        `${TextLevitan.root}:last-child`,
                    );
                },
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.browser.allure.runStep('Дожидаемся появления списка товаров', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.list.waitForExist(),
            );

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.list.waitForLoading();

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.massSelect
                .isVisible()
                .should.eventually.be.equal(true, 'Масс-селект в заголовке таблицы отображается');
        },
        'При выборе всех элементов на странице при помощи масс-селекта': {
            'список отображается': {
                корректно: makeCase({
                    id: 'vendor_auto-1334',
                    environment: 'kadavr',
                    async test() {
                        await this.list
                            .getSelectedItemsCount()
                            .should.eventually.be.equal(0, 'В списке нет отмеченных элементов');

                        await this.showModalButton
                            .isVisible()
                            .should.eventually.be.equal(false, 'Кнопка массовой настройки отзывов не отображается');

                        await this.cancelPromotionButton
                            .isVisible()
                            .should.eventually.be.equal(
                                false,
                                'Кнопка массового сброса элементов в списке не отображается',
                            );

                        await this.selectedModelsCount
                            .isVisible()
                            .should.eventually.be.equal(false, 'Количество выбранных моделей не отображается');

                        await this.massSelect.click();

                        await this.popup.waitForPopupShown();

                        await this.massSelect.selectItem('Все на странице');

                        await this.list
                            .getSelectedItemsCount()
                            .should.eventually.be.equal(20, 'Все элементы на странице отмечены');

                        await this.showModalButton
                            .isVisible()
                            .should.eventually.be.equal(true, 'Кнопка массовой настройки отзывов отображается');

                        await this.cancelPromotionButton
                            .isVisible()
                            .should.eventually.be.equal(
                                true,
                                'Кнопка массового сброса элементов в списке отображается',
                            );

                        await this.selectedModelsCount
                            .isVisible()
                            .should.eventually.be.equal(true, 'Количество выбранных моделей отображается');

                        await this.selectedModelsCount
                            .getText()
                            .should.eventually.be.equal(
                                '20 товаров из 20',
                                'Текст с количеством выбранных моделей корректный',
                            );

                        await this.massSelect.click();

                        await this.popup.waitForPopupShown();

                        await this.massSelect.selectItem('Отменить');

                        await this.list
                            .getSelectedItemsCount()
                            .should.eventually.be.equal(0, 'В списке нет отмеченных элементов');

                        await this.showModalButton
                            .isVisible()
                            .should.eventually.be.equal(false, 'Кнопка массовой настройки отзывов не отображается');

                        await this.cancelPromotionButton
                            .isVisible()
                            .should.eventually.be.equal(
                                false,
                                'Кнопка массового сброса элементов в списке не отображается',
                            );

                        await this.selectedModelsCount
                            .isVisible()
                            .should.eventually.be.equal(false, 'Количество выбранных моделей не отображается');
                    },
                }),
            },
        },
        'При назначении баллов нескольким выбранным товарам': {
            'новое значение баллов применяется': {
                корректно: makeCase({
                    id: 'vendor_auto-1336',
                    environment: 'kadavr',
                    async test() {
                        await this.browser.allure.runStep('Дожидаемся появления элемента списка товаров', () =>
                            this.secondItem.waitForExist(),
                        );

                        await this.secondItemCashbackInput.value.should.eventually.be.equal(
                            '',
                            'Исходное значение в инпуте с баллами корректное',
                        );

                        await this.secondItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Исходное значение в инпуте с целями корректное',
                        );

                        await this.agitationStatsModelsCountText
                            .getText()
                            .should.eventually.be.equal(
                                '2 товаров',
                                'Количество товаров, для которых назначен сбор отображается корректно',
                            );

                        await this.secondItemCheckbox.click();

                        await this.list
                            .getSelectedItemsCount()
                            .should.eventually.be.equal(1, 'Один элемент на странице отмечен');

                        await this.showModalButton
                            .isVisible()
                            .should.eventually.be.equal(true, 'Кнопка массовой настройки отзывов отображается');

                        await this.cancelPromotionButton
                            .isVisible()
                            .should.eventually.be.equal(
                                true,
                                'Кнопка массового сброса элементов в списке отображается',
                            );

                        await this.selectedModelsCount
                            .isVisible()
                            .should.eventually.be.equal(true, 'Количество выбранных моделей отображается');

                        await this.selectedModelsCount
                            .getText()
                            .should.eventually.be.equal(
                                '1 товар из 20',
                                'Текст с количеством выбранных моделей корректный',
                            );

                        await this.showModalButton.click();

                        await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                            this.modal.waitForExist(),
                        );

                        await this.modalCashbackInput.setValue('666');

                        await this.modalSubmitButton.click();

                        await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                            this.toasts.waitForVisible(),
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.equal(
                                'Настроен сбор отзывов для 1 товара',
                                'Текст всплывающего сообщения верный',
                            );

                        await this.secondItemCashbackInput.value.should.eventually.be.equal(
                            '666',
                            'Значение в инпуте с баллами корректное',
                        );

                        await this.secondItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с целями корректное',
                        );

                        await this.agitationStatsModelsCountText
                            .getText()
                            .should.eventually.be.equal(
                                '3 товаров',
                                'Количество товаров, для которых назначен сбор отображается корректно',
                            );
                    },
                }),
            },
        },
        'При нажатии кнопки «Отмена» в окне массового назначения баллов': {
            'введённые значения баллов и целей': {
                'не применяются': makeCase({
                    id: 'vendor_auto-1335',
                    environment: 'kadavr',
                    async test() {
                        this.setPageObjects({
                            cancelButton() {
                                return this.createPageObject(
                                    'ButtonLevitan',
                                    this.modal,
                                    `${ButtonLevitan.root}:nth-child(2)`,
                                );
                            },
                        });

                        await this.browser.allure.runStep('Дожидаемся появления элемента списка товаров', () =>
                            this.secondItem.waitForExist(),
                        );

                        await this.secondItemCashbackInput.value.should.eventually.be.equal(
                            '',
                            'Исходное значение в инпуте с баллами корректное',
                        );

                        await this.secondItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Исходное значение в инпуте с целями корректное',
                        );

                        await this.secondItemCheckbox.click();

                        await this.showModalButton.click();

                        await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                            this.modal.waitForExist(),
                        );

                        await this.modalCashbackInput
                            .isDisabled()
                            .should.eventually.equal(false, 'Инпут с баллами активен');

                        await this.modalTargetInput
                            .isDisabled()
                            .should.eventually.equal(true, 'Инпут с целями задизейблен');

                        await this.modalCashbackInput.setValue('666');

                        await this.modalTargetInput
                            .isDisabled()
                            .should.eventually.equal(false, 'Инпут с целями стал активен');

                        await this.modalTargetInput.setValue('333');

                        await this.cancelButton.click();

                        await this.secondItemCashbackInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с баллами осталось прежним',
                        );

                        await this.secondItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с целями осталось прежним',
                        );
                    },
                }),
            },
        },
        'При вводе некорректного значения баллов': {
            'сообщение об ошибке': {
                отображается: makeCase({
                    id: 'vendor_auto-1337',
                    environment: 'kadavr',
                    async test() {
                        this.setPageObjects({
                            modalErrorMessage() {
                                return this.createPageObject('ErrorMessageLevitan', this.modalCashBackField);
                            },
                        });

                        await this.browser.allure.runStep('Дожидаемся появления первого товара в списке', () =>
                            this.firstItem.waitForExist(),
                        );

                        await this.browser.allure.runStep('Дожидаемся появления третьего товара в списке', () =>
                            this.thirdItem.waitForExist(),
                        );

                        await this.firstItemCashbackInput.value.should.eventually.be.equal(
                            '100',
                            'Исходное значение в инпуте с баллами у первого товара корректное',
                        );

                        await this.firstItemTargetInput.value.should.eventually.be.equal(
                            '3',
                            'Исходное значение в инпуте с целями у первого товара корректное',
                        );

                        await this.thirdItemCashbackInput.value.should.eventually.be.equal(
                            '100',
                            'Исходное значение в инпуте с баллами у третьего товара корректное',
                        );

                        await this.thirdItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Исходное значение в инпуте с целями у третьего товара корректное',
                        );

                        await this.firstItemCheckbox.click();

                        await this.thirdItemCheckbox.click();

                        await this.showModalButton.click();

                        await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                            this.modal.waitForExist(),
                        );

                        await this.modalCashbackInput
                            .isDisabled()
                            .should.eventually.equal(false, 'Инпут с баллами активен');

                        await this.modalTargetInput
                            .isDisabled()
                            .should.eventually.equal(true, 'Инпут с целями задизейблен');

                        await this.modalSubmitButton.click();

                        await this.browser.allure.runStep('Ожидаем появления сообщения с ошибкой', () =>
                            this.modalErrorMessage.waitForExist(),
                        );

                        await this.modalErrorMessage
                            .getText()
                            .should.eventually.equal('Минимальное количество баллов - 50', 'Текст ошибки корректный');

                        await this.modalTargetInput
                            .isDisabled()
                            .should.eventually.equal(true, 'Инпут с целями задизейблен');

                        await this.modalCashbackInput.setValue('5');

                        await this.browser.allure.runStep('Ожидаем появления сообщения с ошибкой', () =>
                            this.modalErrorMessage.waitForExist(),
                        );

                        await this.modalErrorMessage
                            .getText()
                            .should.eventually.equal('Минимальное количество баллов - 50', 'Текст ошибки корректный');

                        await this.modalTargetInput
                            .isDisabled()
                            .should.eventually.equal(true, 'Инпут с целями задизейблен');

                        await this.modalCashbackInput.setValue('55555');

                        await this.browser.allure.runStep('Ожидаем появления сообщения с ошибкой', () =>
                            this.modalErrorMessage.waitForExist(),
                        );

                        await this.modalErrorMessage
                            .getText()
                            .should.eventually.equal(
                                'Максимальное количество баллов - 10000',
                                'Текст ошибки корректный',
                            );

                        await this.modalTargetInput
                            .isDisabled()
                            .should.eventually.equal(true, 'Инпут с целями задизейблен');

                        await this.modalCashbackInput.setValue('55');

                        await this.browser.allure.runStep('Ожидаем скрытия сообщения с ошибкой', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.modalErrorMessage.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия сообщения с ошибкой',
                            ),
                        );

                        await this.modalTargetInput
                            .isDisabled()
                            .should.eventually.equal(false, 'Инпут с целями стал активен');

                        await this.modalSubmitButton.click();

                        await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                            this.toasts.waitForVisible(),
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.equal(
                                'Настроен сбор отзывов для 2 товаров',
                                'Текст всплывающего сообщения верный',
                            );

                        await this.firstItemCashbackInput.value.should.eventually.be.equal(
                            '55',
                            'Значение в инпуте с баллами у первого товара применилось',
                        );

                        await this.firstItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с целями у первого товара обнулилось',
                        );

                        await this.thirdItemCashbackInput.value.should.eventually.be.equal(
                            '55',
                            'Значение в инпуте с баллами у третьего товара применилось',
                        );

                        await this.thirdItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с целями у третьего товара осталось прежним',
                        );
                    },
                }),
            },
        },
        'При массовом назначении баллов и целей товарам': {
            'новые значения баллов и целей применяются корректно': makeCase({
                id: 'vendor_auto-1338',
                environment: 'kadavr',
                async test() {
                    await this.browser.allure.runStep('Дожидаемся появления первого товара в списке', () =>
                        this.firstItem.waitForExist(),
                    );

                    await this.browser.allure.runStep('Дожидаемся появления второго товара в списке', () =>
                        this.secondItem.waitForExist(),
                    );

                    await this.browser.allure.runStep('Дожидаемся появления блока со статистикой', () =>
                        this.agitationStats.waitForExist(),
                    );

                    await this.browser.allure.runStep('Дожидаемся загрузки блока со статистикой', () =>
                        this.browser.waitUntil(
                            async () => {
                                const visible = await this.agitationStatsSpinner.isVisible();

                                return visible === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Не удалось дождаться скрытия спиннера',
                        ),
                    );

                    await this.agitationStatsModelsCountText
                        .getText()
                        .should.eventually.be.equal(
                            '2 товаров',
                            'Количество товаров, для которых назначен сбор отображается корректно',
                        );

                    await this.agitationStats.progressInfo
                        .getText()
                        .should.eventually.be.equal(
                            '2 из 3\nотзывов собрано для 1 цели',
                            'Прогресс по сбору целей отображается корректно',
                        );

                    await this.firstItemCashbackInput.value.should.eventually.be.equal(
                        '100',
                        'Исходное значение в инпуте с баллами у первого товара корректное',
                    );

                    await this.firstItemTargetInput.value.should.eventually.be.equal(
                        '3',
                        'Исходное значение в инпуте с целями у первого товара корректное',
                    );

                    await this.secondItemCashbackInput.value.should.eventually.be.equal(
                        '',
                        'Исходное значение в инпуте с баллами у второго товара корректное',
                    );

                    await this.secondItemTargetInput.value.should.eventually.be.equal(
                        '',
                        'Исходное значение в инпуте с целями у второго товара корректное',
                    );

                    await this.firstItemCheckbox.click();

                    await this.secondItemCheckbox.click();

                    await this.showModalButton.click();

                    await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                        this.modal.waitForExist(),
                    );

                    await this.modalCashbackInput
                        .isDisabled()
                        .should.eventually.equal(false, 'Инпут с баллами активен');

                    await this.modalTargetInput
                        .isDisabled()
                        .should.eventually.equal(true, 'Инпут с целями задизейблен');

                    await this.modalCashbackInput.setValue('80');

                    await this.modalTargetInput
                        .isDisabled()
                        .should.eventually.equal(false, 'Инпут с целями стал активен');

                    await this.modalTargetInput.setValue('4');

                    await this.modalSubmitButton.click();

                    await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                        this.toasts.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Ожидаем показа всплывающего сообщения с подтверждением', () =>
                        this.toast.waitForVisible(),
                    );

                    await this.toast
                        .getText()
                        .should.eventually.equal(
                            'Настроен сбор отзывов для 2 товаров',
                            'Текст всплывающего сообщения верный',
                        );

                    await this.firstItemCashbackInput.value.should.eventually.be.equal(
                        '80',
                        'Значение в инпуте с баллами у первого товара применилось',
                    );

                    await this.firstItemTargetInput.value.should.eventually.be.equal(
                        '4',
                        'Значение в инпуте с целями у первого товара применилось',
                    );

                    await this.secondItemCashbackInput.value.should.eventually.be.equal(
                        '80',
                        'Значение в инпуте с баллами у второго товара применилось',
                    );

                    await this.secondItemTargetInput.value.should.eventually.be.equal(
                        '4',
                        'Значение в инпуте с целями у второго товара применилось',
                    );

                    await this.agitationStatsModelsCountText
                        .getText()
                        .should.eventually.be.equal(
                            '3 товаров',
                            'Количество товаров, для которых назначен сбор отображается корректно',
                        );

                    await this.agitationStats.progressInfo
                        .getText()
                        .should.eventually.be.equal(
                            '2 из 8\nотзывов собрано для 2 целей',
                            'Прогресс по сбору целей отображается корректно',
                        );
                },
            }),
        },
        'При массовой отмене сбора отзывов за баллы': {
            'значения баллов и целей у товаров': {
                обнуляются: makeCase({
                    id: 'vendor_auto-1339',
                    environment: 'kadavr',
                    async test() {
                        await this.browser.allure.runStep('Дожидаемся появления первого товара в списке', () =>
                            this.firstItem.waitForExist(),
                        );

                        await this.browser.allure.runStep('Дожидаемся появления третьего товара в списке', () =>
                            this.thirdItem.waitForExist(),
                        );

                        await this.firstItemCashbackInput.value.should.eventually.be.equal(
                            '100',
                            'Исходное значение в инпуте с баллами у первого товара корректное',
                        );

                        await this.firstItemTargetInput.value.should.eventually.be.equal(
                            '3',
                            'Исходное значение в инпуте с целями у первого товара корректное',
                        );

                        await this.thirdItemCashbackInput.value.should.eventually.be.equal(
                            '100',
                            'Исходное значение в инпуте с баллами у третьего товара корректное',
                        );

                        await this.thirdItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Исходное значение в инпуте с целями у третьего товара корректное',
                        );

                        await this.firstItemCheckbox.click();

                        await this.thirdItemCheckbox.click();

                        await this.cancelPromotionButton.click();

                        await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                            this.toasts.waitForVisible(),
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.equal(
                                'Отменён сбор отзывов за баллы для 2 товаров',
                                'Текст всплывающего сообщения верный',
                            );

                        await this.firstItemCashbackInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с баллами у первого товара обнулилось',
                        );

                        await this.firstItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с целями у первого товара обнулилось',
                        );

                        await this.thirdItemCashbackInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с баллами у третьего товара обнулилось',
                        );

                        await this.thirdItemTargetInput.value.should.eventually.be.equal(
                            '',
                            'Значение в инпуте с целями у третьего товара осталось прежним',
                        );
                    },
                }),
            },
        },
    },
});
