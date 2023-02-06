const PO = require('./PO');

describe('Dispenser: Заявка на оборудование', function() {
    describe('Просмотр остатка для выдачи в заказе', function() {
        it('1. на русском языке', async function() {
            // открыть страницу заявки 4758 (/hardware/4758)
            await this.browser.openIntranetPage({
                pathname: '/hardware/4758',
            }, { user: 'robot-abc-002' });
            await this.browser.waitForVisible(PO.changes(), 5000);
            // кликнуть на карточку CPU
            await this.browser.click(PO.changes.cpu());
            // блок ресурса с неактивной кнопкой "Осталось выдать" [remainder-closed-ru]
            await this.browser.assertView('remainder-closed-ru', PO.changes.details());
            // кликнуть на кнопку "Осталось выдать"
            await this.browser.click(PO.changes.remaindersHandler());
            // блок ресурса с открытым попапом остатка [remainder-open-ru]
            await this.browser.assertView('remainder-open-ru', PO.changes.details());
        });

        it('2. на английском языке', async function() {
            // открыть страницу заявки 4758 на английском языке (/hardware/4758?lang=en)
            await this.browser.openIntranetPage({
                pathname: '/hardware/4758',
                query: { lang: 'en' },
            }, { user: 'robot-abc-002' });
            await this.browser.waitForVisible(PO.changes(), 5000);
            // кликнуть на карточку CPU
            await this.browser.click(PO.changes.cpu());
            // блок ресурса с неактивной кнопкой "Осталось выдать" [remainder-closed-en]
            await this.browser.assertView('remainder-closed-en', PO.changes.details());
            // кликнуть на кнопку "Осталось выдать"
            await this.browser.click(PO.changes.remaindersHandler());
            // блок ресурса с открытым попапом остатка [remainder-open-en]
            await this.browser.assertView('remainder-open-en', PO.changes.details());
        });
    });
});
