hermione.only.in(['chrome-desktop', 'firefox']);

specs({
    feature: 'Сетка карточек',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        const selector = '.mg-page-layout';

        return this.browser
            .url('/turbo?stub=mgpagelayout/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .setViewportSize({ width: 1920, height: 1972 })
            .assertView('layout-1920x1080', selector)
            .setViewportSize({ width: 1600, height: 1972 })
            .assertView('layout-1600x900', selector)
            .setViewportSize({ width: 1366, height: 1972 })
            .assertView('layout-1366x768', selector)
            .setViewportSize({ width: 1280, height: 1972 })
            .assertView('layout-1280x720', selector)
            .setViewportSize({ width: 1279, height: 1972 })
            .assertView('layout-1279x720', selector);
    });
});
