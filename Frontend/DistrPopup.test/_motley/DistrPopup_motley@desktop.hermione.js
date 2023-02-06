'use strict';

const PO = require('../DistrPopup.page-object');

specs({
    feature: 'Popup на СЕРПе',
    type: 'Дизайн с цветовой темой',
}, function() {
    const themes = [
        { motley: 'yellow', foreverdata: 2043182642 },
        { motley: 'grey', foreverdata: 1762005792 },
        { motley: 'dark-grey', foreverdata: 1909898567 },
        { motley: 'green', foreverdata: 2793408136 },
        { motley: 'light-green', foreverdata: 1340880777 },
        { motley: 'dark-green', foreverdata: 963329666 },
        { motley: 'blue', foreverdata: 3066832902 },
        { motley: 'light-blue', foreverdata: 1662409618 },
        { motley: 'dark-blue', foreverdata: 92375932 },
        { motley: 'bordo', foreverdata: 1689760537 },
    ];

    themes.forEach(function(theme) {
        it(`Цветная тема (motley) - ${theme.motley}`, async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                srcskip: 'YABS_DISTR',
                text: 'foreverdata',
                foreverdata: theme.foreverdata,
                data_filter: 'distr-popup',
            }, PO.page());
            await browser.execute(function(selector) {
                $(selector).removeClass('distr-popup_animation_fade-show');
            }, PO.distrPopup());
            await browser.yaWaitForVisible(PO.distrPopup());
            await browser.assertView(`${theme.motley}`, PO.distrPopup(), {
                hideElements: [PO.main(), PO.serpNavigation()],
            });
        });
    });

    [
        { foreverdata: 2896867709, name: 'grey-old' },
        { foreverdata: 372400082, name: 'white-old' },
        { foreverdata: 4037738149, name: 'yellow-old' },
    ].forEach(theme => {
        it(`Цветная тема старая (motley) - ${theme.name}`, async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                srcskip: 'YABS_DISTR',
                text: 'foreverdata',
                foreverdata: theme.foreverdata,
                data_filter: 'distr-popup',
            }, PO.page());
            await browser.execute(function(selector) {
                $(selector).removeClass('distr-popup_animation_slide-show');
            }, PO.distrPopup());
            await browser.yaWaitForVisible(PO.distrPopup());
            await browser.assertView(theme.name, PO.distrPopup(), {
                hideElements: [PO.main(), PO.serpNavigation()],
            });
        });
    });

    it('Выключение попапа', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'foreverdata',
            foreverdata: 1762005792,
            data_filter: 'distr-popup',
            exp_flags: 'distr_popup_disable=1',
        }, PO.page());
        await browser.yaShouldNotBeVisible(PO.distrPopup());
    });
});
