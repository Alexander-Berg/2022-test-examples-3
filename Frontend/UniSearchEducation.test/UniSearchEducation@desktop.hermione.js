'use strict';

const { checkNodeExists } = require('../../../UniSearch.test/helpers');
const PO = require('./UniSearchEducation.page-object/index@desktop');
const { openPreview } = require('./helpers');

specs({
    feature: 'Универсальный колдунщик образования',
}, function() {
    it('Клик по ссылке', async function() {
        const assertNode = checkNodeExists.bind(this);

        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 4087341600,
        }, PO.UniSearchEducation());

        await assertNode({
            path: '/$page/$main/$result/unisearch_education/item',
            attrs: {
                url: 'https://yandex.ru/1',
                title: 'Как стать Python-разработчиком → плюс',
            },
        });

        await this.browser.yaCheckBaobabCounter(PO.UniSearchEducation.Content.List.Item(), {
            path: '/$page/$main/$result/unisearch_education/item/link[@url="https://yandex.ru/1"]',
        });
    });

    it('Фильтр «Бесплатные»', async function() {
        await this.browser.yaOpenSerp({ text: 'python курсы' }, PO.UniSearchEducation());

        const { length: paidCount } = await this.browser.elements(PO.UniSearchEducation.Content.List.Item.Price());
        assert.isAbove(paidCount, 0, 'Изначально должен быть хотя бы один платный курс');
        await this.browser.click(PO.UniSearchEducation.Header.FilterScroller.QuickFilter());
        await this.browser.yaWaitUntil(`Фильтр не применился, нет '${PO.UniSearchEducation.Content.List.Item.DefinedPrice()}'`, async () => {
            const { length: paidCount } = await this.browser.elements(
                PO.UniSearchEducation.Content.List.Item.DefinedPrice(),
            );
            return paidCount === 0;
        }, 3000);

        await this.browser.yaCheckBaobabCounter(() => {}, {
            path: '/$page/$main/$result/unisearch_education/scroller/quick-filter',
            attrs: { title: 'Бесплатные', active: false },
            fast: { wzrd: 'unisearch/education', subtype: 'list' },
            behaviour: { type: 'dynamic' },
        });
    });

    describe('Обогащённое превью', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 603351045,
        };

        it('Счётчик закрытия по клику на крестик', async function() {
            await openPreview(this, serpParams);

            await this.browser.yaCheckBaobabCounter(PO.UniSearchPreview.Close(), {
                path: '/$page/$main/$result/unisearch_education',
                event: 'tech',
                type: 'preview-closed',
            });
        });

        it('Счётчик закрытия по нажатию esc', async function() {
            await openPreview(this, serpParams);

            await this.browser.yaCheckBaobabCounter(async () => await this.browser.yaKeyPress('Escape'), {
                path: '/$page/$main/$result/unisearch_education',
                event: 'tech',
                type: 'preview-closed',
            });
        });

        it('Счётчик закрытия по клику в паранжу', async function() {
            await openPreview(this, serpParams);

            // Прячем контент, чтобы он не получил клик
            await this.browser.execute(function(sel) {
                document.querySelector(sel).style.display = 'none';
            }, [PO.UniSearchPreview.Wrapper()]);

            await this.browser.yaCheckBaobabCounter(PO.UniSearchPreview.Overlay(), {
                path: '/$page/$main/$result/unisearch_education',
                event: 'tech',
                type: 'preview-closed',
            });
        });

        it('Разметка контента baobab', async function() {
            const assertNode = checkNodeExists.bind(this);
            await openPreview(this, serpParams);

            await assertNode({
                path: '$page/$main/$result/unisearch_education/preview_content',
                attrs: {
                    url: 'https://skillbox.ru/course/python-basic/',
                    title: 'Python Basic',
                },
            });

            await assertNode({
                path: '$page/$main/$result/unisearch_education/preview_content/key-value',
                attrs: {
                    duration: true,
                },
            });

            await assertNode({
                path: '$page/$main/$result/unisearch_education/preview_content/price-item',
                attrs: {
                    periodicity: 'в месяц',
                    full: 5572,
                    discount: 3900,
                },
            });

            await assertNode({
                path: '$page/$main/$result/unisearch_education/preview_content/extended-text',
                attrs: {
                    type: 'decription',
                    cutted: true,
                },
            });

            await assertNode({
                path: '$page/$main/$result/unisearch_education/preview_content/bullet-list',
                attrs: {
                    type: 'program',
                },
            });
        });

        describe('UGC отзывы', function() {
            hermione.also.in(['chrome-desktop-dark']);
            it('Внешний вид', async function() {
                const params = {
                    text: 'foreverdata',
                    foreverdata: 3361526588,
                    data_filter: 'unisearch/education',
                };
                await openPreview(this, params);
                await this.browser.yaWaitForVisible(PO.UniSearchEducationPreview.Content.Reviews.List());
                await this.browser.yaStubImage(PO.UniSearchEducationPreview.Content.Reviews.List.Item.Avatar(), 40, 40);
                await this.browser.yaScroll(PO.UniSearchEducationPreview.Content.Reviews());
                await this.browser.assertView('plain', PO.UniSearchEducationPreview.Content.Reviews());
            });
        });
    });

    describe('Курсы со скрытой ценой', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 3975423565,
        };

        hermione.also.in(['chrome-desktop-dark']);
        it('Внешний вид в заголовке превью', async function() {
            await openPreview(this, serpParams);

            await this.browser.assertView('plain', PO.UniSearchEducationPreview.HeaderSectionContent());
        });

        it('Описание цены в шапке превью имеет правильную baobab-разметку', async function() {
            const assertNode = checkNodeExists.bind(this);
            await openPreview(this, serpParams);

            await assertNode({
                path: '$page/$main/$result/unisearch_education/preview_content/price-item',
                attrs: {
                    text: 'Цена на сайте',
                },
            });
        });
    });

    describe('Cсылка на агрегатора в шторке', function() {
        hermione.also.in(['chrome-desktop-dark']);
        it('Внешний вид', async function() {
            await openPreview(this, {
                text: 'foreverdata',
                foreverdata: 920191366,
                data_filter: 'unisearch/education',
            });

            await this.browser.assertView('plain', PO.UniSearchEducationPreview.Footer());
        });
    });
});
