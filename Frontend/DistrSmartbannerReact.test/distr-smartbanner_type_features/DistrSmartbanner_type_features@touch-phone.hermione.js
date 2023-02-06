'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn('searchapp-phone', 'смартбаннера нет в поисковом приложении');
specs({
    feature: 'Смартбаннер React',
    type: 'со списком фич',
}, () => {
    hermione.also.in('iphone-dark');
    it('Внешний вид смартбаннера без кнопки закрыть', async function() {
        await this.browser.yaOpenSerp({
            text: 'scorpions',
            foreverdata: 3295640503,
            srcskip: 'YABS_DISTR',
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'achievement',
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-features-cross', PO.smartInfo());

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
            path: '/$page/distr-smartbanner/close[@tags@close=1 and @behaviour@type="dynamic"]',
        });

        await this.browser.yaWaitForHidden(PO.smartInfo());
    });

    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 2162081419,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'achievement',
        }, PO.smartInfo());
    });

    hermione.also.in('iphone-dark');
    it('Внешний вид', async function() {
        await this.browser.yaAssertViewIsolated('features', PO.smartInfo());
    });

    it('По клику на кнопку \'Скачать\' смартбаннер должен скрываться', async function() {
        await this.browser.yaCheckBaobabCounter(PO.smartInfo.action(), {
            path: '/$page/distr-smartbanner/ok',
        });

        await this.browser.yaWaitForHidden(PO.smartInfo());
    });

    it('По клику на кнопку \'Скачать\' ссылка открывается в текущей вкладке', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.smartInfo.action(),
            target: '_self',
        });
    });

    it('По клику на кнопку \'Закрыть\' смартбаннер должен скрываться', async function() {
        await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
            path: '/$page/distr-smartbanner/close[@tags@close=1 and @behaviour@type="dynamic"]',
        });

        await this.browser.yaWaitForHidden(PO.smartInfo());
    });
});
