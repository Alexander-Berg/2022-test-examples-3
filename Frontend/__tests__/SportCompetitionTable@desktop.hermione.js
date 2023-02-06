specs({
    feature: 'Страница турнира',
}, () => {
    const selector = '.sport-competition-table';

    it('Внешний вид группового этапа турнирной таблицы', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/football-group-phase-desktop.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .setViewportSize({ width: 1920, height: 1972 })
            .assertView('layout-1920', selector)
            .setViewportSize({ width: 1366, height: 1972 })
            .assertView('layout-1366', selector)
            .setViewportSize({ width: 1250, height: 1972 })
            .assertView('layout-1250', selector);
    });

    it('Внешний вид турнирной таблицы (Футбол)', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/football-desktop.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .setViewportSize({ width: 1920, height: 1972 })
            .assertView('layout-1920', selector)
            .setViewportSize({ width: 1366, height: 1972 })
            .assertView('layout-1366', selector)
            .setViewportSize({ width: 1250, height: 1972 })
            .assertView('layout-1250', selector);
    });

    it('Внешний вид турнирной таблицы (Баскетбол)', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/basketball-desktop.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .setViewportSize({ width: 1920, height: 1972 })
            .assertView('layout-1920', selector)
            .setViewportSize({ width: 1366, height: 1972 })
            .assertView('layout-1366', selector)
            .setViewportSize({ width: 1250, height: 1972 })
            .assertView('layout-1250', selector);
    });

    it('Внешний вид турнирной таблицы (Волейбол)', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/volleyball-desktop.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .setViewportSize({ width: 1920, height: 1972 })
            .assertView('layout-1920', selector)
            .setViewportSize({ width: 1366, height: 1972 })
            .assertView('layout-1366', selector)
            .setViewportSize({ width: 1250, height: 1972 })
            .assertView('layout-1250', selector);
    });

    it('Внешний вид турнирной таблицы (Хоккей)', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/hockey-desktop.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .setViewportSize({ width: 1920, height: 1972 })
            .assertView('layout-1920', selector)
            .setViewportSize({ width: 1366, height: 1972 })
            .assertView('layout-1366', selector)
            .setViewportSize({ width: 1250, height: 1972 })
            .assertView('layout-1250', selector);
    });
});
