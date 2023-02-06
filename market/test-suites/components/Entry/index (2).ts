'use strict';

import {makeSuite, importSuite, mergeSuites, PageObject} from 'ginny';

const Entry = PageObject.get('Entry');

export default makeSuite('Заявка.', {
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления заявки', () => this.item.waitForExist());

                await this.allure.runStep('Раскрываем заявку', () => this.item.root.click());

                // Считаем, что заявка открылась, когда появилась кнопка Одобрить кампанию
                await this.browser.allure.runStep(
                    'Дожидаемся открытия заявки',
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    () => this.item.waitForExist(Entry.acceptButton),
                );

                await this.browser.allure.runStep(
                    'Проверяем, что заявка открылась и отображается корректно',
                    async () => {
                        await this.item.inWorkButton
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Кнопка "В работу" отображается');

                        await this.item.rejectButton
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Кнопка "Отказать" отображается');

                        await this.item.acceptButton
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Кнопка "Одобрить кампанию" отображается');
                    },
                );
            },
        },
        importSuite('Entry/edit'),
        importSuite('Entry/createFromEntry', {
            suiteName: 'Создание карточки из заявки.',
            meta: {
                id: 'vendor_auto-247',
                issue: 'VNDFRONT-2937',
                environment: 'kadavr',
            },
            params: {
                expectedPath: '/new?entryId=666&brandId=12804226&brandName=Sony%20Creative',
            },
        }),
        importSuite('Entry/createFromEntry', {
            suiteName: 'Создание карточки и вендора из заявки.',
            meta: {
                id: 'vendor_auto-248',
                issue: 'VNDFRONT-2937',
                environment: 'kadavr',
            },
            params: {
                expectedPath: '/new?entryId=666',
            },
        }),
    ),
});
