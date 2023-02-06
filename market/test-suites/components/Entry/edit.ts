'use strict';

import {makeSuite, makeCase} from 'ginny';

// eslint-disable-next-line max-len
const TRADEMARK_DOCUMENT_LINK =
    'https://vendors-public.s3.mdst.yandex.net/offer-documents/e0c554c5-2de7-4015-a1cc-64d7e48972b0/test.docx';

/**
 * @param {PageObject.Entry} item – элемент списка заявок
 * @param {PageObject.ListContainer} list - список заявок
 */
export default makeSuite('Обработка заявки.', {
    feature: 'Заявки',
    environment: 'kadavr',
    id: 'vendor_auto-245',
    issue: 'VNDFRONT-2935',
    params: {
        user: 'Пользователь',
    },
    story: {
        'Статус заявки и логин менеджера': {
            'при нажатии на кнопку "В работу"': {
                сохраняются: makeCase({
                    issue: 'VNDFRONT-2935',
                    id: 'vendor_auto-245',
                    async test() {
                        await this.browser.allure.runStep('Нажимаем кнопку "В работу"', () =>
                            this.item.inWorkButton.click(),
                        );

                        // Считаем, что изменения применились, когда скроется кнопка "В работу"
                        await this.browser.allure.runStep('Дожидаемся обновления заявки', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const existing = await this.item.inWorkButton.vndIsExisting();

                                    return existing === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "В работу" отображается',
                            ),
                        );

                        await this.item.status
                            .getText()
                            .should.eventually.be.equal('заявка в работе', 'Статус заявки отображается корректно');

                        await this.item.modifiedDate
                            .getText()
                            .should.eventually.include('Обновлено Сегодня', 'Время обновления отображается корректно');

                        await this.browser.allure.runStep('Обновляем страницу', () => this.browser.refresh());

                        await this.allure.runStep('Ожидаем появления заявки', () => this.item.waitForExist());

                        await this.allure.runStep('Раскрываем заявку', () => this.item.root.click());

                        await this.browser.allure.runStep(
                            'Проверяем, что заявка открылась и отображается корректно',
                            () =>
                                this.item.manager
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Менеджер заявки: Vasily P. (manageruserforvendors)',
                                        'Информация о менеджере отображается корректно',
                                    ),
                        );
                    },
                }),
            },
        },
        'Статус заявки и дата операции': {
            'при отказе или одобрении заявки': {
                сохраняются: makeCase({
                    issue: 'VNDFRONT-3431',
                    id: 'vendor_auto-246',
                    async test() {
                        await this.browser.allure.runStep('Нажимаем кнопку "Отказать"', () =>
                            this.item.rejectButton.click(),
                        );

                        // Считаем, что изменения применились, когда скроется кнопка "Отказать"
                        await this.browser.allure.runStep('Дожидаемся обновления заявки', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const existing = await this.item.rejectButton.vndIsExisting();

                                    return existing === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "Отказать" отображается',
                            ),
                        );

                        await this.item.status
                            .getText()
                            .should.eventually.be.equal('отказ', 'Статус заявки отображается корректно');

                        await this.item.modifiedDate
                            .getText()
                            .should.eventually.include('Обновлено Сегодня', 'Время обновления отображается корректно');

                        await this.item.rejectButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка «Отказать» не отображается');

                        await this.browser.allure.runStep('Обновляем страницу', () => this.browser.refresh());

                        await this.allure.runStep('Ожидаем появления заявки', () => this.item.waitForExist());

                        await this.allure.runStep('Раскрываем заявку', () => this.item.root.click());

                        await this.item.manager
                            .getText()
                            .should.eventually.be.equal(
                                'Менеджер заявки: Vasily P. (manageruserforvendors)',
                                'Информация о менеджере в заголовке заявки отображается корректно',
                            );

                        await this.item.rejectButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка «Отказать» не отображается');

                        await this.browser.allure.runStep('Нажимаем кнопку "Одобрить кампанию"', () =>
                            this.item.acceptButton.click(),
                        );

                        // Считаем, что изменения применились, когда скроется кнопка "Одобрить кампанию"
                        await this.browser.allure.runStep('Дожидаемся обновления заявки', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const existing = await this.item.acceptButton.vndIsExisting();

                                    return existing === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "Одобрить кампанию" отображается',
                            ),
                        );

                        await this.item.status
                            .getText()
                            .should.eventually.be.equal('заявка обработана', 'Статус заявки отображается корректно');

                        await this.item.acceptButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка «Одобрить кампанию» не отображается');

                        await this.item.modifiedDate
                            .getText()
                            .should.eventually.include('Обновлено Сегодня', 'Время обновления отображается корректно');
                    },
                }),
            },
        },
        'В офертной заявке': {
            'содержатся корректные ссылки на скачивание документов': makeCase({
                issue: 'VNDFRONT-3431',
                id: 'vendor_auto-39',
                async test() {
                    this.setPageObjects({
                        form() {
                            return this.createPageObject('Form');
                        },
                        file() {
                            return this.createPageObject(
                                'DownloadableFile',
                                this.form.getDocumentUploadFieldByName('trademarkDocuments'),
                            );
                        },
                        downloadLink() {
                            return this.createPageObject('Link', this.file, 'a');
                        },
                    });

                    await this.file
                        .isExisting()
                        .should.eventually.be.equal(true, 'Поле "Свидетельства на товарный знак" отображается');

                    await this.file.root.vndHoverToElement();

                    await this.downloadLink
                        .isVisible()
                        .should.eventually.be.equal(true, 'Ссылка на скачивание документа отображается');

                    await this.downloadLink.getUrl().should.eventually.be.link(TRADEMARK_DOCUMENT_LINK);
                },
            }),
        },
    },
});
