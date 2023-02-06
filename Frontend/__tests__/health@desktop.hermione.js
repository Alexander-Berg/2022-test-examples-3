specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    it('Список статей', function() {
        const selector = '.layout';

        return this.browser
            .url('?stub=health%2Farticles.json&brand=health&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaWaitForVisible(selector, 'Страница не загрузилась')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .setViewportSize({ width: 1920, height: 1972 })
            .assertView('layout-1920x1080', selector)
            .setViewportSize({ width: 1600, height: 1972 })
            .assertView('layout-1600x900', selector)
            .setViewportSize({ width: 1366, height: 1972 })
            .assertView('layout-1366x768', selector)
            .setViewportSize({ width: 1260, height: 1972 })
            .assertView('layout-1260x720', selector)
            .setViewportSize({ width: 1200, height: 1972 })
            .assertView('layout-1200x720', selector);
    });

    it('Страница "Не найдено"', function() {
        const selector = '.layout';

        return this.browser
            .url('/turbo?stub=health/not-found.json&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaWaitForVisible(selector, 'Страница не загрузилась')
            .setViewportSize({ width: 1920, height: 1080 })
            .assertView('not-found-1920x1080', selector)
            .setViewportSize({ width: 1600, height: 900 })
            .assertView('not-found-1600x900', selector)
            .setViewportSize({ width: 1366, height: 768 })
            .assertView('not-found-1366x768', selector)
            .setViewportSize({ width: 1260, height: 720 })
            .assertView('not-found-1260x720', selector)
            .setViewportSize({ width: 1200, height: 720 })
            .assertView('not-found-1200x720', selector);
    });
});
