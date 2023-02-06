'use strict';

import {makeSuite, mergeSuites, makeCase} from 'ginny';

export default makeSuite('Бренд-зона.', {
    environment: 'testing',
    feature: 'Статистика',
    story: mergeSuites(
        {
            'Отчёт по показателям эффективности.': {
                'При формировании отчёта': {
                    'генерируется ссылка на скачивание': makeCase({
                        issue: 'VNDFRONT-3614',
                        id: 'vendor_auto-1178',
                        async test() {
                            await this.generateReportButton
                                .isVisible()
                                .should.eventually.be.equal(true, 'Кнопка "Сформировать отчёт" отображается');

                            await this.browser.allure.runStep('Нажимаем на кнопку "Сформировать отчёт"', () =>
                                this.generateReportButton.click(),
                            );

                            await this.browser.allure.runStep('Дожидаемся появления ссылки', () =>
                                this.firstReportLink.waitForExist(),
                            );

                            const url = await this.firstReportLink.getUrl();

                            await this.browser.allure.runStep('Проверяем название файла в URL', () =>
                                url.path.should.match(
                                    new RegExp('stats/global/brandzone_stats/[0-9a-z-]+/[a-zA-Z0-9-_]+.xlsx'),
                                    'Название файла верное',
                                ),
                            );
                        },
                    }),
                },
            },
        },
        {
            'Общий вид страницы': makeCase({
                id: 'vendor_auto-1163',
                issue: 'VNDFRONT-3614',
                async test() {
                    await this.browser.allure.runStep('Дожидаемся показа текстового блока на странице', () =>
                        this.vendorTariffTitle.waitForExist(),
                    );

                    await this.vendorTariffTitle
                        .getText()
                        .should.eventually.includes('Ваш тариф — «Продажи', 'Значение корректное');
                },
            }),
        },
        {
            'Показы баннеров.': {
                'Нет информации по оставшимся показам': {
                    'когда не было периодов активности': makeCase({
                        issue: 'VNDFRONT-3929',
                        id: 'vendor_auto-1165',
                        async test() {
                            await this.metricValueElem
                                .isVisible()
                                .should.eventually.be.equal(true, 'Элемент отображается');

                            await this.metricValueElem.getText().should.eventually.be.equal('0', 'Значение корректное');
                        },
                    }),
                },
            },
        },
    ),
});
