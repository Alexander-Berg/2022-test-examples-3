'use strict';
import assert from 'assert';
import OrdListPage from '../page-objects/desktop/ordListPage';
import { authorize } from '../auth';
import { ordPath } from '../../../.config/vars';
import { ReportStatus } from '../page-objects/enums';
import { Product, PRODUCT_LIST } from '../../../src/constants/services';

describe('Маркировка / ОРД', function() {
    beforeEach(async({ browser }) => {
        await authorize(browser);
    });

    describe('Список отчетов', function() {
        it('Статусы отчетов (черновик/отправлен)', async({ browser }) => {
            await browser.yaOpenPageByUrl(ordPath);
            await browser.yaWaitForVisible('table > thead > tr');

            let reports_count = 0;
            const reportsArray = await browser.$$('tbody > tr').map(el => el.getText());
            reportsArray.forEach(row => {
                if (row.includes('ya.draft')) {
                    assert(row.includes('Черновик'));
                    ++reports_count;
                } else if (row.includes('ya.sent')) {
                    assert(row.includes('Отправлен'));
                    ++reports_count;
                }
            });
            if (reports_count < 2) {
                assert(false, 'Expected at least 2 reports to show up, but found ' + reports_count);
            }
        });

        it('Отчеты отображаются', async({ browser }) => {
            await browser.yaOpenPageByUrl(ordPath);
            await browser.yaWaitForVisible('table > thead > tr');

            let reports_count = 0;
            const reportsArray = await browser.$$('tbody > tr').map(el => el.getText());
            reportsArray.forEach(_row => {
                ++reports_count;
            });
            assert(reports_count >= 2, 'Expected at least 2 reports to show up, but found ' + reports_count);
        });

        it('Удаленные отчеты не отображаются', async({ browser }) => {
            await browser.yaOpenPageByUrl(ordPath);
            await browser.yaWaitForVisible('table > thead > tr');

            const reportsArray = await browser.$$('tbody > tr').map(el => el.getText());
            reportsArray.forEach(row => {
                assert(!row.includes('ya.deleted'));
            });
        });

        it('Иконки всех площадок отображаются', async({ browser }) => {
            const page: OrdListPage = new OrdListPage(browser);
            await page.open();
            await browser.yaWaitForPageLoad();
            await browser.$('td a div svg path').waitForDisplayed();

            const ServiceTypesMap = new Map<Product | 'test', string>(
                (Object.keys(PRODUCT_LIST) as Array<keyof typeof PRODUCT_LIST>)
                    .map(key => [key, PRODUCT_LIST[key].toLowerCase()] as const)
            );
            ServiceTypesMap.set('test', 'test');
            ServiceTypesMap.delete(Product.facebook); // Пока нету сервиса
            ServiceTypesMap.delete(Product.google); // Пока нету сервиса
            ServiceTypesMap.delete(Product.tiktok); // Пока нету сервиса

            for (const row of await page.tableRow.map((value, index) => ({ index, value }))) {
                const rowText = (await row.value.getText()).toLowerCase();
                for (const [pictureKey, name] of ServiceTypesMap) {
                    if (rowText.includes(name)) {
                        await browser.assertView(
                            pictureKey,
                            '//tbody/tr[' + (row.index + 1) + ']//*[name()="svg"]');
                        ServiceTypesMap.delete(pictureKey);
                        break;
                    }
                }
            }
            assert(ServiceTypesMap.size === 0, 'Not all supported icon types are visible');
        });

        describe('Фильтры', function() {
            // TODO: Period, Period From, Period To
            // TODO: Sorting
            it('По статусу отчета -- UI', async({ browser }) => {
                let page: OrdListPage = new OrdListPage(browser);
                await page.open();
                let rows;

                page.selectStatus(ReportStatus.All);
                rows = await page.rows;
                rows = rows.toString();
                assert(rows.includes('Черновик') && rows.includes('Отправлен'), 'Both drafts and sents are expected');

                await page.selectStatus(ReportStatus.Draft);
                rows = await page.rows;
                rows = rows.toString();
                assert(rows.includes('Черновик') && !rows.includes('Отправлен'), 'Only dratfs are expected');

                await page.selectStatus(ReportStatus.Sent);
                rows = await page.rows;
                rows = rows.toString();
                assert(!rows.includes('Черновик') && rows.includes('Отправлен'), 'Only sents are expected');
            });

            it('По статусу отчета -- URL', async({ browser }) => {
                let page: OrdListPage = new OrdListPage(browser);
                let rows;

                await page.open();
                rows = await page.rows;
                rows = rows.toString();
                assert(rows.includes('Черновик') && rows.includes('Отправлен'), 'Both drafts and sents are expected by default');

                await page.open(undefined, '?status=' + ReportStatus.All);
                rows = await page.rows;
                rows = rows.toString();
                assert(rows.includes('Черновик') && rows.includes('Отправлен'), 'Both drafts and sents are expected');

                await page.open(undefined, '?status=' + ReportStatus.Draft);
                rows = await page.rows;
                rows = rows.toString();
                assert(rows.includes('Черновик') && !rows.includes('Отправлен'), 'Only dratfs are expected');

                await page.open(undefined, '?status=' + ReportStatus.Sent);
                rows = await page.rows;
                rows = rows.toString();
                assert(!rows.includes('Черновик') && rows.includes('Отправлен'), 'Only sents are expected');
            });
        });
    });
});
