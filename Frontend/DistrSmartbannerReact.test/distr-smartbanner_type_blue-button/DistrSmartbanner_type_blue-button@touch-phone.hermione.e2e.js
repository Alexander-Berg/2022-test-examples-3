'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

// https://wiki.yandex-team.ru/users/morozovadi/bannery-dlja-testirovanija-na-serpe/
hermione.only.notIn('searchapp-phone'); // смартбаннера нет в поисковом приложении
specs({
    feature: 'Смартбаннер React',
    type: 'blue-button',
}, () => {
    it('Присутствует на странице', async function() {
        const { browser } = this;

        await browser.deleteCookie();
        await browser.yaOpenSerp({
            text: 'txt',
            'banner-id': '72057605999494468',
            exp_flags: 'smartbanner_atom=1',
        }, PO.page());
        await browser.yaWaitForVisible(PO.smartInfo(), 'Смартбаннер не показался');
        await browser.click(PO.smartInfo.close());
        await browser.yaWaitForHidden(PO.smartInfo(), 'Смартбаннер не закрылся');
    });
});
