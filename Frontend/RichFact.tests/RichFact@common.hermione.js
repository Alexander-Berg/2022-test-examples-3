'use strict';

const PO = require('./RichFact.page-object');

specs('Расширенный факт', () => {
    it('Несколько источников', async function() {
        const { browser } = this;
        const href = 'https://languages.oup.com/google-dictionary-ru/';

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: '1459605187', data_filter: 'rich_fact' }, PO.richFact());
        await browser.assertView('plain', PO.richFact());
        await browser.yaCheckLink2({
            url: { href },
            selector: PO.richFact.sourceLink.link(),
            baobab: { path: '/$page/$main/$result/rich_fact/source-link' },
            target: '_blank',
            message: 'Неправильная ссылка',
        });
    });

    it('Выделение цветом', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: '2010421768', data_filter: 'rich_fact' }, PO.richFact());
        await browser.assertView('plain', PO.richFact());
    });

    hermione.also.in('iphone-dark');
    it('Несколько правил', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: '1182951221', data_filter: 'rich_fact' }, PO.richFact());
        await browser.assertView('plain', PO.richFact());
    });

    it('Таблица', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: '3462775540', data_filter: 'rich_fact' }, PO.richFact());
        await browser.assertView('plain', PO.richFact());
    });

    it('Брендинг', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 422557946, data_filter: false }, PO.richFact());

        await browser.assertView('plain', [PO.factHeader(), PO.searchResultsHeader()]);
    });

    it('Длинный список', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 1917632869, data_filter: false }, PO.richFact());
        await browser.assertView('plain', PO.richFact());
    });

    it('Разворачивание списка', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: '3317947206', data_filter: 'rich_fact' }, PO.richFact());
        await browser.assertView('collapsed', PO.richFact());

        await this.browser.yaCheckBaobabCounter(PO.richFact.listCollapserToggle(), {
            path: '/$page/$main/$result/rich_fact/list-collapser-toggle',
        });

        await browser.assertView('expanded', PO.richFact());
    });

    hermione.also.in('iphone-dark');
    it('Фрагмент', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text: 'foreverdata', foreverdata: 2703197087, data_filter: false, exp_flags: 'fact_doc_sum=chapter' }, PO.richFact());
        await browser.assertView('plain', PO.richFact());

        await browser.yaScroll(0);
        await browser.yaCheckBaobabCounter(PO.richFact.chapter(), { path: '/$page/$main/$result/rich_fact/chapter' });

        await browser.assertView('opened', PO.richFact());

        await browser.yaCheckBaobabCounter(PO.richFact.collapser(), { path: '/$page/$main/$result/rich_fact/Collapser/open' });

        await browser.assertView('opened-collapser', PO.richFact());
    });

    it('Проверка source в $result', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'как сделать скриншот на макбуке',
            data_filter: 'rich_fact',
        }, PO.richFact());

        await browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result[@wizard_name="rich_fact" and @source="apple_facts"]',
        });
    });

    describe('ECom факт', async function() {
        /** TODO: При раскатке удалить исключение WizardsEnabled из yaCheckForbiddenWords.js https://nda.ya.ru/t/iqavkNI04ugAu3 */
        it('Внешний вид', async function() {
            await this.browser.yaOpenSerp({
                text: 'как выбрать холодильник',
                rearr: 'scheme_blender/commands/after_blend/move_mds_ecom={\"Cmd\":\"nail_intent_doc\",\"on\":1,\"MoveTo\":2,\"IntentName\":\"mds_ecom\"}',
                srcparams: [
                    'FASTRES2=WizardsEnabled/ecom_research_amorgun11=1',
                ],
            }, PO.ecom());

            await this.browser.assertView('plain', PO.ecom());
        });

        it('Внешний вид с расхлопом', async function() {
            await this.browser.yaOpenSerp({
                text: 'как выбрать холодильник',
                rearr: 'scheme_blender/commands/after_blend/move_mds_ecom={\"Cmd\":\"nail_intent_doc\",\"on\":1,\"MoveTo\":2,\"IntentName\":\"mds_ecom\"}',
                srcparams: [
                    'FASTRES2=WizardsEnabled/ecom_research_amorgun11=1',
                ],
            }, PO.ecom());

            await this.browser.click(PO.ecom.secondECFragmentCollapser());
            await this.browser.moveToObject(PO.ecom());
            await this.browser.assertView('plain', PO.ecom.secondECFragmentCollapser());
        });

        describe('Проверка счётчиков', async function() {
            it('Кнопка снизу факта', async function() {
                await this.browser.yaOpenSerp({
                    text: 'как выбрать холодильник',
                    rearr: 'scheme_blender/commands/after_blend/move_mds_ecom={\"Cmd\":\"nail_intent_doc\",\"on\":1,\"MoveTo\":2,\"IntentName\":\"mds_ecom\"}',
                    srcparams: [
                        'FASTRES2=WizardsEnabled/ecom_research_amorgun11=1',
                    ],
                }, PO.ecom());

                await this.browser.yaCheckBaobabCounter(PO.ecom.eightECFragment.button(), {
                    path: '$page/$main/$result/rich_fact/detailed',
                });
            });

            it('Кнопка в расхлопнутом колапсере', async function() {
                await this.browser.yaOpenSerp({
                    text: 'как выбрать холодильник',
                    rearr: 'scheme_blender/commands/after_blend/move_mds_ecom={\"Cmd\":\"nail_intent_doc\",\"on\":1,\"MoveTo\":2,\"IntentName\":\"mds_ecom\"}',
                    srcparams: [
                        'FASTRES2=WizardsEnabled/ecom_research_amorgun11=1',
                    ],
                }, PO.ecom());

                await this.browser.yaCheckBaobabCounter(PO.ecom.secondECFragmentCollapser(), {
                    path: '$page/$main/$result/rich_fact/Collapser/open',
                });

                await this.browser.yaWaitForVisible(PO.ecom.secondECFragmentCollapser.detailButton());
                await this.browser.yaCheckBaobabCounter(PO.ecom.secondECFragmentCollapser.detailButton(), {
                    path: '$page/$main/$result/rich_fact/Collapser/detailed',
                });
            });
        });
    });
});
