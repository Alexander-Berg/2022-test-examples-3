import {describe} from 'spec/utils';

import type {Ctx} from '..';
import {assertPlacementFilter, openDropdown, uploadFile} from '../utils';

const fixedTableHeaderOffset = -100;

export default describe<Ctx>(
    {
        title: 'Выполнение операций с каталогом под менеджером ридером.',
        feature: 'Ассортимент',
        environment: 'testing',
    },
    it => {
        it(
            {
                title: 'Проверка',
                id: 'marketmbi-6299',
                issue: 'MARKETPARTNER-23733',
            },
            async ctx => {
                await ctx.app.offers.table.root.waitForVisible();

                await ctx.step('1. Таблица с товарами', async () => {
                    await ctx.step('В меню для товара клик на пункт "Скрыть с витрины"', async () => {
                        const row = await ctx.app.offers.getRow(0);
                        // нужно небольшой офсет, иначе фиксированная шапка таблицы наезжает на строку
                        await ctx.browser.scroll(row.PO.controlCellButton, 0, fixedTableHeaderOffset);
                        await openDropdown(ctx, row);
                        await row.clickOfferHide();
                    });

                    await ctx.toast.waitForKeyVisible('pages.common:error.forbidden-access.title');
                    await ctx.toast.waitForKeyVisible('pages.common:error.forbidden-access.details');
                    await ctx.toast.close();
                    await ctx.toast.waitForClosed();
                });

                await ctx.step('2. Шапка страницы', async () => {
                    await ctx.browser.scroll(ctx.app.header.PO.root);
                    await assertPlacementFilter(ctx, {
                        value: 'OTHER',
                    });

                    const row = await ctx.app.offers.getRow(0);
                    await row.controlAddOfferToCurrentPlacement.click();
                    await ctx.app.addPlacementModal.modal.waitForOpen(5000);
                });

                await ctx.step('3. Заполняем поля', async () => {
                    await ctx.app.addPlacementModal.form.priceTextField.setValue({
                        name: 'Актуальная цена',
                        value: '5000',
                    });
                    await ctx.app.addPlacementModal.form.urlTextField.setValue({
                        name: 'Ссылка',
                        value: 'https://autotest.ru',
                    });
                    await ctx.app.addPlacementModal.form.addPlacementButton.click();
                    await ctx.toast.waitForKeyVisible('pages.common:error.forbidden-access.title');
                    await ctx.toast.waitForKeyVisible('pages.common:error.forbidden-access.details');
                    await ctx.toast.close();
                    await ctx.toast.waitForClosed();
                    await ctx.app.addPlacementModal.modal.clickOutside();
                    await ctx.app.addPlacementModal.modal.waitForClosed(10000);
                });

                await ctx.step('4. Шапка страницы', async () => {
                    await ctx.browser.scroll(ctx.app.header.PO.root);
                    await ctx.app.uploadFeedButton.click();
                    await ctx.app.upload.catalogUploadModal.waitForVisible();

                    await ctx
                        .expect(ctx.app.upload.catalogUploadModal.waitForVisible(3000))
                        .equal(true, 'Появилась модалка загрузки файла');

                    const file = require.resolve('../testData/feed_for_readonly_new.csv');
                    await uploadFile({ctx, file});
                });

                await ctx.step('5. Попап результатов валидации', async () => {
                    await ctx.app.upload.feedAddButton.click();
                    await ctx.toast.waitForKeyVisible('pages.common:error.forbidden-access.title');
                    await ctx.toast.waitForKeyVisible('pages.common:error.forbidden-access.details');
                    await ctx.toast.close();
                    await ctx.toast.waitForClosed();
                    await ctx.app.upload.modal.clickOutside();
                });

                await ctx.step('6. Фильтры', async () => {
                    await ctx.browser.scroll(ctx.app.header.PO.root);

                    await ctx.step('Клик на кнопку Добавить из магазинов', async () => {
                        await ctx.app.offers.addAtPlacementModelBtn.click();
                        await ctx.app.batchAddPlacementModal.waitForVisible(3000);
                    });

                    await ctx.step('В открывшейся модалке клик на Добавить в магазин', async () => {
                        await ctx.app.batchAddPlacementModal.copyOffersButton.click();
                    });

                    await ctx.toast.waitForKeyVisible('pages.common:error.forbidden-access.title');
                    await ctx.toast.waitForKeyVisible('pages.common:error.forbidden-access.details');
                });
            },
        );
    },
);
