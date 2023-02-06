'use strict';
const { checkNodeExists } = require('../../../UniSearch.test/helpers');
const PO = require('./UniSearchMedicine.page-object/index@common');

const PREVIEW_OPEN_TIMEOUT = 3000;

// Убираем незначительные для скриншота элементы выдачи под превью,
// чтобы их изменения не затрагивали тест.
function onlyPreview() {
    return this.browser.execute(() => {
        [
            document.querySelector('.main'),
            document.querySelector('.serp-footer'),
            document.querySelector('.serp-header'),
            document.querySelector('.HeaderPhone'),
            document.querySelector('.serp-navigation'),
        ].filter(Boolean).forEach(node => {
            node.style.display = 'none';
        });
    });
}

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
    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp(defaultParams, PO.UniSearchMedicine());

        await this.browser.assertView('plain', PO.UniSearchMedicine());
    });

    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Дозагрузка', async function() {
        await this.browser.yaOpenSerp(defaultParams, PO.UniSearchMedicine());

        const { length: currentCount } = await this.browser.elements(PO.UniSearchMedicine.Content.List.Item());
        await this.browser.click(PO.UniSearchMedicine.Footer.More());
        await this.browser.yaWaitUntil('Не загрузились следующие врачи', async () => {
            const { length: nextCount } = await this.browser.elements(PO.UniSearchMedicine.Content.List.Item());
            return nextCount > currentCount;
        }, 3000);

        // Убираем курсор с того места, где появится следующий врач, чтобы он не подсвечивался.
        await this.browser.moveToObject(PO.UniSearchMedicine.Footer.More());
        await this.browser.assertView('append', PO.UniSearchMedicine());

        await this.browser.yaCheckBaobabCounter(() => { }, {
            path: '/$page/$main/$result/unisearch_medicine/more',
            fast: { wzrd: 'unisearch/medicine', subtype: 'list' },
            behaviour: { type: 'dynamic' },
        });
    });

    it('Баобаб-разметка айтемов', async function() {
        const assertNode = checkNodeExists.bind(this);

        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 95446167,
        }, PO.UniSearchMedicine());

        await assertNode({
            path: '/$page/$main/$result/unisearch_medicine/item',
            attrs: {
                url: 'https://zoon.ru/msk/p-doctor/tatyana_vladimirovna_shapovalenko/',
            },
        });
    });

    it('Баобаб-разметка ТБ', async function() {
        const assertNode = checkNodeExists.bind(this);

        await this.browser.yaOpenSerp({
            text: 'Терапевты москва',
        }, PO.UniSearchMedicine());

        await assertNode({
            path: '/$page/$main/$result/unisearch_medicine/title',
        });
    });

    describe('Обогащённое превью', function() {
        hermione.only.notIn('searchapp-phone', 'https://st.yandex-team.ru/SERP-141914');

        describe('Секция с описанием врача', function() {
            hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
            it('Внешний вид', async function() {
                await openPreview(this, {
                    ...defaultParams,
                    foreverdata: 698374499,
                });
                await onlyPreview.call(this);

                await this.browser.assertView('plain', PO.UniSearchMedicinePreview.Main());
                await this.browser.click(PO.UniSearchMedicinePreview.Main.DescriptionMore());
                await this.browser.assertView('expanded', PO.UniSearchMedicinePreview.Main());
            });
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид', async function() {
            await openPreview(this, defaultParams);
            await onlyPreview.call(this);
            await this.browser.yaWaitForVisible(PO.UniSearchMedicinePreview.Reviews.List());
            await this.browser.assertView('plain', PO.UniSearchPreview());
        });

        it('preview_content нет в дереве, если Превью не было открыто', async function() {
            const assertNode = checkNodeExists.bind(this);

            await this.browser.yaOpenSerp(defaultParams, PO.UniSearchMedicine());

            try {
                await assertNode({
                    path: '$page/$main/$result/unisearch_medicine/preview',
                    attrs: {
                        url: 'https://zoon.ru/msk/p-doctor/tatyana_vladimirovna_shapovalenko/',
                    },
                });
            } catch (e) {
                assert(/В Баобаб не найден узел по пути \$\w+\/\$main\/\$result\/unisearch_medicine\/preview/.test(e.message));
            }
        });

        describe('UGC отзывы', function() {
            hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
            it('Внешний вид c аспектами', async function() {
                const params = {
                    text: 'foreverdata',
                    foreverdata: 1530189269,
                    data_filter: 'unisearch/medicine',
                };
                await openPreview(this, params);
                await onlyPreview.call(this);
                await this.browser.yaWaitForVisible(PO.UniSearchMedicinePreview.Reviews.List());
                await this.browser.yaScroll(PO.UniSearchMedicinePreview.Reviews.List.Item());
                await this.browser.assertView('plain', PO.UniSearchMedicinePreview.Reviews.List.Item());
            });
        });

        hermione.only.in('chrome-desktop'); //ждем раскатки unisearch_medicine_appointments_slots_desk
        it('Разметка контента baobab', async function() {
            const assertNode = checkNodeExists.bind(this);
            await openPreview(this, defaultParams);

            await assertNode({
                path: '/$page/$main/$result/unisearch_medicine/preview/unisearch-offer/link',
                attrs: {
                    offer_url: 'http://docdoc.ru/doctor/Komissarenko_Irina?pid=27997',
                    offer_source: 'СберЗдоровье',
                    offer_price: {
                        product: 'за приём',
                        value: 5000,
                        currency: 'RUR',
                        text: '5000 ₽ за приём',
                    },
                    offer_rating: '5',
                },
            });

            await assertNode({
                path: '/$page/$main/$result/unisearch_medicine/preview/unisearch-clinic/link',
                attrs: {
                    name: 'Гута Клиник',
                    url: '?text=%D0%93%D1%83%D1%82%D0%B0+%D0%9A%D0%BB%D0%B8%D0%BD%D0%B8%D0%BA&oid=b:242158516982&serp-reload-from=unisearch/medicine',
                },
            });

            await assertNode({
                path: '$page/$main/$result/unisearch_medicine/preview/unisearch-review',
            });
        });
    });
});
