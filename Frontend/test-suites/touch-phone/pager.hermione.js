'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs('Пейджер', function() {
    it('Проверка ссылки и счётчика перехода на следующую страницу', function() {
        return this.browser
            .yaOpenSerp('text=тёмная материя')
            .yaWaitForVisible(PO.pager(), 'Пейджер не появился')
            .yaCheckLink(PO.pager.page(), { checkClickability: true, target: '' })
            .yaCheckBaobabCounter(PO.pager.page(), {
                path: '/$page/$main/$pager/button[@pageno=1]'
            });
    });

    it('Проверка перехода на вторую страницу и возврата обратно', function() {
        return this.browser
            .yaOpenSerp('text=тёмная материя')
            .yaWaitForVisible(PO.pager(), 'Пейджер не появился')
            .yaWaitUntilPageReloaded(() => {
                this.browser.click(PO.pager.page());
            })
            .getText(PO.pager.current())
            .then(page => {
                assert.equal(page, 2);
            })
            .getUrl()
            .then(url => {
                assert(url.indexOf('&p=1') !== -1);
                assert.equal((url.match(/\&p=\d+/g) || []).length, 1);
            })
            .yaWaitUntilPageReloaded(() => {
                this.browser.click(PO.pager.page());
            })
            .getText(PO.pager.current())
            .then(page => {
                assert.equal(page, 1);
            })
            .getUrl()
            .then(url => {
                assert(url.indexOf('&p=0') !== -1);
                assert.equal((url.match(/\&p=\d+/g) || []).length, 1);
            });
    });

    hermione.only.notIn('searchapp', 'в searchapp нет поисковой стрелки');
    it('Проверка перехода на вторую страницу и перезапроса', function() {
        return this.browser
            .yaOpenSerp('text=тёмная материя')
            .yaWaitForVisible(PO.pager(), 'Пейджер не появился')
            .yaWaitUntilPageReloaded(() => {
                this.browser.click(PO.pager.page());
            })
            .getText(PO.pager.current())
            .then(page => {
                assert.equal(page, 2, `Должна быть выбрана 2-я страница, а выбрана ${page}-я`);
            })
            .click(PO.search.input())
            .yaWaitUntilPageReloaded(() => {
                this.browser.click(PO.search.button());
            })
            .getText(PO.pager.current())
            .then(page => {
                assert.equal(page, 1, `Должна быть выбрана 1-я страница, а выбрана ${page}-я`);
            });
    });

    it('Проверка некликабельности текущей страницы пейджера', function() {
        return this.browser
            .yaOpenSerp('text=тёмная материя&p=99')
            .yaWaitForVisible(PO.pager(), 'Пейджер не появился')
            .getTagName(PO.pager.current())
            .then(tagName => {
                assert.equal(tagName, 'span');
            });
    });

    it('Проверка отсутствия пейджера в пустой выдаче', function() {
        return this.browser
            .yaOpenSerp('text=')
            .yaShouldNotBeVisible(PO.pager(), 'Пейджер не появился');
    });
});
