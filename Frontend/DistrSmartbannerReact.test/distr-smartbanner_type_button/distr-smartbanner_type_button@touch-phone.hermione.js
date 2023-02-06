'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn('searchapp-phone'); // смартбаннера нет в поисковом приложении
specs({
    feature: 'Смартбаннер React',
    type: 'button',
}, () => {
    hermione.also.in('iphone-dark');
    it('Скромный смартбаннер, короткий заголовок', async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 1198104246,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-button-centered', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    hermione.also.in('iphone-dark');
    it('Скромный смартбаннер, эталонный заголовок', async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 1270428695,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-button', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    it('Скромный смартбаннер, эталонный заголовок, проверка кликабельных областей', async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 1270428695,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.execute(function(selectors, colors) {
            selectors.forEach(function(selector, index) {
                $(selector).css('background-color', colors[index]);
            });
        }, [PO.smartInfo.action(), PO.smartInfo.close()], ['blue', 'red']);

        await this.browser.yaAssertViewIsolated('smartbanner-button-clickable-area', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    hermione.also.in('iphone-dark');
    it('Скромный смартбаннер, длинный заголовок с обрезанием текста и троеточием', async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 720303474,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-button-long', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    hermione.also.in('iphone-dark');
    it('Скромный смартбаннер, установка цвета', async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 4008142256,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-button-colored', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    it('По клику на смартбаннер он должен скрываться', async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 1270428695,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.action(), {
            path: '/$page/microdistribution/ok',
        });

        await this.browser.yaWaitForHidden(PO.smartInfo());
    });

    it('По клику на кнопку закрытия смартбаннер должен скрываться', async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 1270428695,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
            path: '/$page/microdistribution/close[@tags@close=1 and @behaviour@type="dynamic"]',
        });

        await this.browser.yaWaitForHidden(PO.smartInfo());
    });
});
