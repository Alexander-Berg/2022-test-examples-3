'use strict';

import {makeSuite, makeCase, PageObject} from 'ginny';

const ReportDownloadButton = PageObject.get('ReportDownloadButton');
const ButtonLevitan = PageObject.get('ButtonLevitan');

/**
 * Тесты на формирование ссылки на отчёт
 * @param {Object} params
 * @param {number} params.vendor Идентификатор вендора
 */
export default makeSuite('Формирование отчёта.', {
    feature: 'Бренд на Маркете',
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При нажатии на кнопку': {
            'создаётся ссылка на скачивание XLSX-файла': makeCase({
                id: 'vendor_auto-1399',
                issue: 'VNDFRONT-4214',
                async test() {
                    this.setPageObjects({
                        reportDownload() {
                            return this.createPageObject('BrandAnalyticsReportDownload');
                        },
                        generateButton() {
                            return this.createPageObject(
                                'ButtonLevitan',
                                this.reportDownload,
                                `${ButtonLevitan.root}[role="button"]`,
                            );
                        },
                        processMessage() {
                            return this.createPageObject('ReportDownloadButtonProcessMessage', this.reportDownload);
                        },
                        downloadButton() {
                            return this.createPageObject(
                                'ReportDownloadButton',
                                this.reportDownload,
                                `${ReportDownloadButton.root}[role="link"]`,
                            );
                        },
                    });

                    await this.browser.allure.runStep('Ожидаем появления блока формирования отчёта', () =>
                        this.reportDownload.waitForVisible(),
                    );

                    await this.generateButton.click();

                    await this.browser.allure.runStep('Проверяем неактивность кнопки формирования отчёта', () =>
                        this.generateButton.isDisabled().should.eventually.be.equal(true, 'Кнопка заблокирована'),
                    );

                    await this.browser.allure.runStep(
                        'Ожидаем появления сообщения об активном формировании отчёта',
                        () => this.processMessage.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Проверяем текст сообщения об активном формировании отчёта', () =>
                        this.processMessage
                            .getText()
                            .should.eventually.be.equal('Формируем отчёт\nЗаймёт до 5 минут', 'Текст корректный'),
                    );

                    await this.browser.allure.runStep('Ожидаем появления ссылки на скачивание отчёта', () =>
                        this.downloadButton.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Проверяем параметры ссылки на скачивание отчёта', async () => {
                        const url = await this.downloadButton.getUrl();

                        await url.path.should.match(
                            new RegExp(`/stats/global/brands/[0-9a-z-]+/brands_${this.params.vendor}_[0-9-_]+.xlsx$`),
                            'URL-адрес корректный',
                        );

                        await this.downloadButton
                            .getTarget()
                            .should.eventually.be.equal('_blank', 'Ссылка откроется в новом окне');

                        await this.downloadButton
                            .getText()
                            .should.eventually.be.equal('Скачать', 'Текст ссылки корректный');
                    });

                    await this.browser.allure.runStep(
                        'Проверяем отсутствие сообщения об активном формировании отчёта',
                        () => this.processMessage.isExisting().should.eventually.be.equal(false, 'Сообщение скрыто'),
                    );

                    await this.browser.allure.runStep('Проверяем активность кнопки формирования отчёта', () =>
                        this.generateButton.isDisabled().should.eventually.be.equal(false, 'Кнопка разблокирована'),
                    );
                },
            }),
        },
    },
});
