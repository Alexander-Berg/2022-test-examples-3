'use strict';
const _ = require('lodash');
const { checkNodeExists } = require('../../../UniSearch.test/helpers');
const PO = require('./UniSearchEducation.page-object/index@common');
const { openPreview } = require('./helpers');

/**
 * Открывает реальную страницу с курсами с нужным набором флагов,
 * потому что пока что фича под экспериментом.
 * Указание test-id в тестах не работает.
 */
function openExp() {
    return this.browser.yaOpenSerp({
        text: 'python курсы',
        init_meta: 'enable-src-education_saas',
        exp_flags: [
            'unisearch_education_enabled=1',
            'unisearch_education_relev=formula=proxima_static',
        ],
        rearr: 'scheme_Local/UnisearchEducation/Enabled=yes',
    }, PO.UniSearchEducation());
}

specs({
    feature: 'Универсальный колдунщик образования',
}, function() {
    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 3938174157,
            numdoc: 1,
        }, PO.UniSearchEducation());

        await this.browser.assertView('plain', PO.UniSearchEducation());
    });

    it('Внешний вид рубрикатора при выборе нескольких профессий', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 1374151066,
            exp_flags: 'unisearch_ed_filters_desktop=1',
        }, PO.UniSearchEducation());

        await this.browser.assertView('plain', PO.UniSearchEducation.Header.MainFilter());
    });

    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Дозагрузка', async function() {
        await openExp.call(this);

        const { length: currentCount } = await this.browser.elements(PO.UniSearchEducation.Content.List.Item());
        await this.browser.click(PO.UniSearchEducation.Footer.More());
        await this.browser.yaWaitUntil('Не загрузились следующие курсы', async () => {
            const { length: nextCount } = await this.browser.elements(PO.UniSearchEducation.Content.List.Item());
            return nextCount > currentCount;
        }, 3000);

        // Убираем курсор с того места, где появится следующий курс, чтобы он не подсвечивался.
        await this.browser.moveToObject(PO.UniSearchEducation.Footer.More());
        await this.browser.assertView('append', PO.UniSearchEducation());

        await this.browser.yaCheckBaobabCounter(() => {}, {
            path: '/$page/$main/$result/unisearch_education/more',
            fast: { wzrd: 'unisearch/education', subtype: 'list' },
            behaviour: { type: 'dynamic' },
        });
    });

    describe('Обогащённое превью', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 603351045,
        };

        hermione.only.notIn('searchapp-phone', 'https://st.yandex-team.ru/SERP-141914');
        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид', async function() {
            await openPreview(this, serpParams);
            // Убираем незначительные для скриншота элементы выдачи под превью,
            // чтобы их изменения не затрагивали тест.
            await this.browser.execute(() => {
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
            await this.browser.assertView('plain', PO.UniSearchPreview());
        });

        it('Переход по ссылке', async function() {
            await openPreview(this, serpParams);

            await this.browser.yaCheckBaobabCounter(PO.UniSearchEducationPreview.Action(), {
                path: '/$page/$main/$result/unisearch_education/preview_content/preview-action-button[@url="https://skillbox.ru/course/python-basic/"]',
                fast: { wzrd: 'unisearch/education', subtype: 'list' },
            });
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Короткое описание', async function() {
            await openPreview(this, serpParams);

            await this.browser.assertView('folded', PO.UniSearchEducationPreview.Content.LongText());
            await this.browser.click(PO.UniSearchEducationPreview.Content.LongText.Toggle());
            await this.browser.assertView('unfolded', PO.UniSearchEducationPreview.Content.LongText());
        });

        it('preview_content нет в дереве, если Превью не было открыто', async function() {
            const assertNode = checkNodeExists.bind(this);

            await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

            try {
                await assertNode({
                    path: '$page/$main/$result/unisearch_education/preview_content',
                    attrs: {
                        url: 'https://skillbox.ru/course/python-basic/',
                        title: 'Python Basic',
                    },
                });
            } catch (e) {
                assert(/В Баобаб не найден узел по пути \$\w+\/\$main\/\$result\/unisearch_education\/preview/.test(e.message));
            }
        });
    });

    describe('Курсы со скрытой ценой', function() {
        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид курса в сниппете', async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 3975423565,
            }, PO.UniSearchEducation());

            await this.browser.assertView('plain', PO.UniSearchEducation.Content.List.Item());
        });
    });

    hermione.only.in(['chrome-desktop', 'chrome-phone', 'chrome-desktop-dark', 'iphone-dark'], 'Не браузерозависимо');
    describe('Курсы без цены в данных', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 3817151708,
        };

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид курса в сниппете', async function() {
            await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

            await this.browser.assertView('plain', PO.UniSearchEducation.Content.List.Item());
        });

        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Внешний вид в заголовке превью', async function() {
            await openPreview(this, serpParams);

            await this.browser.assertView('plain', PO.UniSearchEducationPreview.PriceList());
        });
    });

    describe('Cсылка на агрегатора в шторке', function() {
        it('Разметка ссылки в baobab', async function() {
            const assertNode = checkNodeExists.bind(this);
            await openPreview(this, {
                text: 'foreverdata',
                foreverdata: 920191366,
                data_filter: 'unisearch/education',
            });

            await assertNode({
                path: '$page/$main/$result/unisearch_education/preview_content/aggregator-link',
                attrs: {
                    url: 'https://цифровыепрофессии.рф/catalog',
                },
            });
        });
    });
});
