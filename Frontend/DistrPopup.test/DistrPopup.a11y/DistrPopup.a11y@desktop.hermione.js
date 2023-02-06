'use strict';

const PO = require('../DistrPopup.page-object');

specs({
    feature: 'Popup на СЕРПе',
}, function() {
    it('Доступность', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'foreverdata',
            foreverdata: '1318899850',
            data_filter: 'distr-popup',
        }, PO.page());
        await browser.yaWaitForVisible(PO.distrPopup());

        assert.equal(
            await browser.getAttribute(PO.distrPopup(), 'aria-hidden'),
            'true',
            `Элемент с селектором ${PO.distrPopup()} должен быть скрыт от a11y`,
        );
        assert.equal(
            await browser.getAttribute(PO.distrPopup(), 'tabindex'),
            '-1',
            `Элемент с селектором ${PO.distrPopup()} должен иметь атрибут tabindex=-1`,
        );

        assert.equal(
            await browser.getAttribute(PO.distrPopup.closeButton(), 'aria-hidden'),
            'true',
            `Элемент с селектором ${PO.distrPopup.closeButton()} должен быть скрыт от a11y`,
        );
        assert.equal(
            await browser.getAttribute(PO.distrPopup.closeButton(), 'tabindex'),
            '-1',
            `Элемент с селектором ${PO.distrPopup.closeButton()} должен иметь атрибут tabindex=-1`,
        );

        assert.equal(
            await browser.getAttribute(PO.distrPopup.installButton(), 'aria-hidden'),
            'true',
            `Элемент с селектором ${PO.distrPopup.installButton()} должен быть скрыт от a11y`,
        );
        assert.equal(
            await browser.getAttribute(PO.distrPopup.installButton(), 'tabindex'),
            '-1',
            `Элемент с селектором ${PO.distrPopup.installButton()} должен иметь атрибут tabindex=-1`,
        );
    });
});
