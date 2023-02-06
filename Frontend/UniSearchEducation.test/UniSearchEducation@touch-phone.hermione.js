'use strict';

const { checkNodeExists } = require('../../../UniSearch.test/helpers');
const PO = require('./UniSearchEducation.page-object/index@touch-phone');
const { openPreview } = require('./helpers');

const POPUP_OPEN_TIMEOUT = 5000;

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
            path: '/$page/$main/$result/unisearch_education/item/link',
            attrs: {
                url: 'https://yandex.ru/1',
                title: 'Как стать Python-разработчиком → плюс',
                itemPos: 0,
            },
        });

        await this.browser.yaCheckBaobabCounter(PO.UniSearchEducation.Content.List.Item(), {
            path: '/$page/$main/$result/unisearch_education/item/link[@url="https://yandex.ru/1"]',
        });
    });

    describe('Обогащённое превью', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 603351045,
        };

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

        it('Скрытие превью при возврате назад', async function() {
            await openPreview(this, serpParams);
            await this.browser.yaWaitForVisible(PO.UniSearchEducationPreview());
            await this.browser.back();
            await this.browser.yaWaitForHidden(PO.UniSearchEducationPreview(), 'Не удалось закрыть полную карточку врача');

            await this.browser.forward();
            await this.browser.yaWaitForVisible(PO.UniSearchEducationPreview());

            await this.browser.execute(selector => {
                $(selector).click();
            }, PO.Drawer.overlay());
            await this.browser.yaWaitForHidden(PO.UniSearchEducationPreview(), 'Не удалось закрыть полную карточку врача');
        });

        describe('UGC отзывы', function() {
            hermione.also.in(['iphone-dark']);
            it('Внешний вид', async function() {
                const params = {
                    text: 'foreverdata',
                    foreverdata: 3361526588,
                    data_filter: 'unisearch/education',
                };
                await openPreview(this, params);
                await this.browser.yaWaitForVisible(PO.UniSearchEducationPreview.Content.Reviews.List());

                await this.browser.execute(()=>{
                    window.$('.UniSearchPreview').css('position', 'relative');
                    window.$('.UniSearchPreview').css('height', 'auto');
                });

                await this.browser.yaStubImage(PO.UniSearchEducationPreview.Content.Reviews.List.Item.Avatar(), 40, 40);
                await this.browser.yaScroll(PO.UniSearchEducationPreview.Content.Reviews());

                await this.browser.assertView('plain', [
                    PO.UniSearchEducationPreview.Content.Reviews.List.Header(),
                    PO.UniSearchEducationPreview.Content.Reviews.List.Item()]);
            });

            it('Курсы где 0 отзывов', async function() {
                const params = {
                    text: 'foreverdata',
                    foreverdata: 1906630350,
                    data_filter: 'unisearch/education',
                };
                await openPreview(this, params);
                await this.browser.execute(()=>{
                    window.$('.UniSearchPreview').css('position', 'relative');
                    window.$('.UniSearchPreview').css('height', 'auto');
                });

                await this.browser.isVisibleWithinViewport(PO.UniSearchEducationPreview.Content.Reviews.Feedback(), 'Форма сбора отзывов не отобразилась');
            });
        });
    });

    describe('Курсы со скрытой ценой', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 3975423565,
        };

        hermione.also.in('iphone-dark');
        it('Внешний вид в заголовке превью', async function() {
            await openPreview(this, serpParams);

            await this.browser.assertView('plain', PO.UniSearchEducationPreview.Section());
        });

        hermione.also.in('iphone-dark');
        hermione.only.notIn('searchapp-phone', 'https://st.yandex-team.ru/SERP-141914');
        it('Внешний вид кнопки', async function() {
            await openPreview(this, serpParams);

            await this.browser.assertView('plain', PO.UniSearchEducationPreview.Action());
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
        hermione.also.in(['iphone-dark']);

        it('Внешний вид', async function() {
            await openPreview(this, {
                text: 'foreverdata',
                foreverdata: 920191366,
                data_filter: 'unisearch/education',
            });

            await this.browser.assertView('plain', PO.UniSearchEducationPreview.Section());
        });
    });

    describe('Фильтры', function() {
        const serpParams = {
            text: 'foreverdata',
            foreverdata: 1116343441,
            data_filter: 'unisearch/education',
        };

        hermione.also.in('iphone-dark');
        describe('Внешний вид', function() {
            it('Сниппет', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

                await this.browser.assertView('plain', PO.UniSearchEducation.Header());
            });

            it('Шторка с направлениями', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

                await this.browser.click(PO.UniSearchEducation.Header.FilterAppendix());

                await this.browser.yaWaitForVisible(
                    PO.UniSearchPopup(),
                    POPUP_OPEN_TIMEOUT,
                    'Не удалось открыть шторку с фильтрами',
                );

                await this.browser.assertView('plain', PO.Drawer.content());
            });

            it('Шторка с профессиями', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

                await this.browser.click(PO.UniSearchEducation.Header.MainFilter());

                await this.browser.yaWaitForVisible(
                    PO.UniSearchPopup(),
                    POPUP_OPEN_TIMEOUT,
                    'Не удалось открыть шторку с фильтрами',
                );

                await this.browser.assertView('plain', PO.Drawer.content());
            });

            it('Шторка с общими фильтрами', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

                await this.browser.click(PO.UniSearchEducation.Header.FilterScroller.Filter());

                await this.browser.yaWaitForVisible(
                    PO.UniSearchPopup(),
                    POPUP_OPEN_TIMEOUT,
                    'Не удалось открыть шторку с фильтрами',
                );

                await this.browser.assertView('plain', PO.Drawer.content());
            });
        });

        hermione.only.in(['chrome-phone'], 'Не браузерозависимо');
        describe('Baobab-разметка', function() {
            it('Кнопки фильтров в сниппете', async function() {
                const assertNode = checkNodeExists.bind(this);

                await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filter-appendix',
                    attrs: {
                        text: 'Все направления',
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filters/filter-select-button',
                    attrs: {
                        title: 'Направление',
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/scroller/filters/filter-select-button',
                    attrs: {
                        title: 'Цель обучения',
                        order: 1,
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/scroller/filters/filter-select-button',
                    attrs: {
                        title: 'Цена курса',
                        order: 2,
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/scroller/filters/filter-select-button',
                    attrs: {
                        title: 'Требуемый опыт',
                        order: 3,
                    },
                });
            });

            it('Шторка с направлениями', async function() {
                const assertNode = checkNodeExists.bind(this);

                await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

                await this.browser.click(PO.UniSearchEducation.Header.FilterAppendix());

                await this.browser.yaWaitForVisible(
                    PO.UniSearchPopup(),
                    POPUP_OPEN_TIMEOUT,
                    'Не удалось открыть шторку с фильтрами',
                );

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filters/filters-popup-content/filter-select-popup/filter-select-first-level-list-popup',
                    attrs: {
                        title: 'Направление',
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filters/filters-popup-content/filters-cancel-button-popup',
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filters/filters-popup-content/filters-accept-button-popup',
                });
            });

            it('Шторка с профессиями', async function() {
                const assertNode = checkNodeExists.bind(this);

                await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

                await this.browser.click(PO.UniSearchEducation.Header.MainFilter());

                await this.browser.yaWaitForVisible(
                    PO.UniSearchPopup(),
                    POPUP_OPEN_TIMEOUT,
                    'Не удалось открыть шторку с фильтрами',
                );

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filters/filters-popup-content/filter-select-popup/filter-select-second-level-list-popup',
                    attrs: {
                        title: 'Профессии',
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filters/filters-popup-content/filter-select-popup/filter-select-second-level-list-popup/filter-select-to-first-level-link-popup',
                    attrs: {
                        text: 'Направления',
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filters/filters-popup-content/filters-cancel-button-popup',
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/filters/filters-popup-content/filters-accept-button-popup',
                });
            });

            it('Шторка с общими фильтрами', async function() {
                const assertNode = checkNodeExists.bind(this);

                await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

                await this.browser.click(PO.UniSearchEducation.Header.FilterScroller.Filter());

                await this.browser.yaWaitForVisible(
                    PO.UniSearchPopup(),
                    POPUP_OPEN_TIMEOUT,
                    'Не удалось открыть шторку с фильтрами',
                );

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/scroller/filters/filters-popup-content/filter-select-popup',
                    attrs: {
                        title: 'Цель обучения',
                        order: 1,
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/scroller/filters/filters-popup-content/filter-select-popup',
                    attrs: {
                        title: 'Цена курса',
                        order: 2,
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/scroller/filters/filters-popup-content/filter-select-popup',
                    attrs: {
                        title: 'Требуемый опыт',
                        order: 3,
                    },
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/scroller/filters/filters-popup-content/filters-cancel-button-popup',
                });

                await assertNode({
                    path: '/$page/$main/$result/unisearch_education/scroller/filters/filters-popup-content/filters-accept-button-popup',
                });
            });
        });

        it('Кликнутый фильтр поднимается в шторке', async function() {
            await this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

            await this.browser.yaIndexify(PO.UniSearchEducation.Header.FilterScroller.Filter());
            await this.browser.click(PO.UniSearchEducation.Header.FilterScroller.FirstFilter());

            await this.browser.yaWaitForVisible(
                PO.UniSearchPopup.FilterTitle(),
                POPUP_OPEN_TIMEOUT,
                'Шторка с фильтрами не появилась после клика в первый фильтр',
            );

            await this.browser.yaIndexify(PO.UniSearchPopup.FilterTitle());
            const firstFilterClickFirstFilterTitle = await this.browser.getText(
                PO.UniSearchPopup.FirstFilterTitle(),
            );
            assert.strictEqual(
                firstFilterClickFirstFilterTitle,
                'Цель обучения',
                'После клика в первый фильтр первый заголовок в шторке имеет неверное значение',
            );
            const firstFilterClickSecondFilterTitle = await this.browser.getText(
                PO.UniSearchPopup.SecondFilterTitle(),
            );
            assert.strictEqual(
                firstFilterClickSecondFilterTitle,
                'Цена курса',
                'После клика в первый фильтр второй заголовок в шторке имеет неверное значение',
            );
            const firstFilterClickThirdFilterTitle = await this.browser.getText(
                PO.UniSearchPopup.ThirdFilterTitle(),
            );
            assert.strictEqual(
                firstFilterClickThirdFilterTitle,
                'Требуемый опыт',
                'После клика в первый фильтр третий заголовок в шторке имеет неверное значение',
            );

            await this.browser.click(PO.UniSearchPopup.CancelButton());

            await this.browser.yaWaitForHidden(PO.UniSearchPopup(), 'Шторка с фильтрами не закрылась');

            await this.browser.click(PO.UniSearchEducation.Header.FilterScroller.ThirdFilter());

            await this.browser.yaWaitForVisible(
                PO.UniSearchPopup.FilterTitle(),
                POPUP_OPEN_TIMEOUT,
                'Шторка с фильтрами не появилась после клика во второй фильтр',
            );

            await this.browser.yaIndexify(PO.UniSearchPopup.FilterTitle());
            const thirdFilterClickFirstFilterTitle = await this.browser.getText(
                PO.UniSearchPopup.FirstFilterTitle(),
            );
            assert.strictEqual(
                thirdFilterClickFirstFilterTitle,
                'Требуемый опыт',
                'После клика в третий фильтр первый заголовок в шторке имеет неверное значение',
            );
            const thirdFilterClickSecondFilterTitle = await this.browser.getText(
                PO.UniSearchPopup.SecondFilterTitle(),
            );
            assert.strictEqual(
                thirdFilterClickSecondFilterTitle,
                'Цель обучения',
                'После клика в третий фильтр второй заголовок в шторке имеет неверное значение',
            );
            const thirdFilterClickThirdFilterTitle = await this.browser.getText(
                PO.UniSearchPopup.ThirdFilterTitle(),
            );
            assert.strictEqual(
                thirdFilterClickThirdFilterTitle,
                'Цена курса',
                'После клика в третий фильтр третий заголовок в шторке имеет неверное значение',
            );
        });
    });
});
