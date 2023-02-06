'use strict';
const { checkNodeExists } = require('../../../UniSearch.test/helpers');
const PO = require('./UniSearchMedicine.page-object/index@touch-phone');
const PREVIEW_OPEN_TIMEOUT = 3000;

async function openPreview(_this, serpParams) {
    await _this.browser.yaOpenSerp(serpParams, PO.UniSearchMedicine());

    await _this.browser.click(PO.UniSearchMedicine.Content.List.Item());
    return _this.browser.yaWaitForVisible(
        PO.UniSearchMedicinePreview(),
        PREVIEW_OPEN_TIMEOUT,
        'Не удалось открыть полную карточку врача',
    );
}

const defaultParams = {
    text: 'терапевт москва',
    data_filter: 'unisearch/medicine',
};

specs({
    feature: 'Универсальный колдунщик поиска врачей',
}, function() {
    describe('Фильтры', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 843763667,
            data_filter: 'unisearch/medicine',
        };

        hermione.also.in(['iphone-dark']);
        it('Внешний вид', async function() {
            await this.browser.yaOpenSerp(serpParams, PO.UniSearchMedicine());

            await this.browser.assertView('plain', PO.UniSearchMedicine.Header());
        });

        hermione.only.in(['chrome-phone'], 'Не браузерозависимо');
        it('Баобаб-разметка фильтров', async function() {
            const assertNode = checkNodeExists.bind(this);

            await this.browser.yaOpenSerp(serpParams, PO.UniSearchMedicine());

            await assertNode({
                path: '/$page/$main/$result/unisearch_medicine/scroller/filters/filter-select-button',
                attrs: {
                    title: 'Специализация по возрасту',
                    order: 1,
                },
            });

            await assertNode({
                path: '/$page/$main/$result/unisearch_medicine/scroller/filters/filter-select-button',
                attrs: {
                    title: 'Цена приёма',
                    order: 3,
                },
            });

            await assertNode({
                path: '/$page/$main/$result/unisearch_medicine/scroller/filters/filter-select-button',
                attrs: {
                    title: 'Стаж',
                    order: 4,
                },
            });

            await assertNode({
                path: '/$page/$main/$result/unisearch_medicine/scroller/filters/filter-bool-button',
                attrs: {
                    title: 'Онлайн-запись',
                    order: 5,
                },
            });

            await assertNode({
                path: '/$page/$main/$result/unisearch_medicine/scroller/filters/filter-select-button',
                attrs: {
                    title: 'Учёная степень',
                    order: 6,
                },
            });
        });
    });

    describe('Обогащённое превью', function() {
        it('Скрытие превью при возврате назад', async function() {
            const serpParams = {
                text: 'терапевт москва',
                data_filter: 'unisearch/medicine',
            };
            await this.browser.yaOpenSerp(serpParams, PO.UniSearchMedicine());
            const firstUrl = await this.browser.getUrl();
            await this.browser.click(PO.UniSearchMedicine.Content.List.Item());
            await this.browser.yaWaitForVisible(
                PO.UniSearchMedicinePreview(),
                PREVIEW_OPEN_TIMEOUT,
                'Не удалось открыть полную карточку врача',
            );
            await this.browser.yaWaitForVisible(PO.UniSearchMedicinePreview.Reviews.List());
            await this.browser.back();
            // после возврата назад ссылка должна быть та же
            await this.browser.yaCheckURL(firstUrl, await this.browser.getUrl());
            await this.browser.yaWaitForHidden(PO.UniSearchMedicinePreview(), 'Не удалось закрыть полную карточку врача');

            await this.browser.forward();
            await this.browser.yaWaitForVisible(PO.UniSearchMedicinePreview());

            await this.browser.execute(selector => {
                $(selector).click();
            }, PO.Drawer.overlay());
            await this.browser.yaWaitForHidden(PO.UniSearchMedicinePreview(), 'Не удалось закрыть полную карточку врача');
        });

        //удалить после раскатки unisearch_medicine_appointments_slots_desk
        it('Разметка превью baobab', async function() {
            const assertNode = checkNodeExists.bind(this);
            await openPreview(this, defaultParams);

            await assertNode({
                path: '/$page/$main/$result/unisearch_medicine/preview/unisearch-clinics/clinic/title-link',
                attrs: {
                    url: '?text=%D0%97%D0%B4%D0%BE%D1%80%D0%BE%D0%B2%D1%8C%D0%B5&oid=b:1025778665&serp-reload-from=unisearch/medicine',
                },
            });

            await assertNode({
                path: '$page/$main/$result/unisearch_medicine/preview/unisearch-review',
            });
        });
    });

    describe('Список врачей', function() {
        hermione.also.in(['iphone-dark']);
        it('Рейтинг', async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 4119715856,
                data_filter: 'unisearch/medicine',
            }, PO.UniSearchMedicine());

            await this.browser.assertView(
                'plain',
                PO.UniSearchMedicine.Content.List.Item.Rating(),
            );
        });
    });
});
