specs({
    feature: 'SportCompetitionTable',
}, () => {
    const selector = '.sport-competition-table';

    hermione.only.notIn('safari13');
    it('Внешний вид группового этапа турнирной таблицы', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/football-group-phase-phone.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });

    hermione.only.notIn('safari13');
    it('Внешний вид турнирной таблицы (Футбол)', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/football-phone.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });

    hermione.only.notIn('safari13');
    it('Внешний вид турнирной таблицы (Баскетбол)', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/basketball-phone.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });

    hermione.only.notIn('safari13');
    it('Внешний вид турнирной таблицы (Волейбол)', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/volleyball-phone.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });

    hermione.only.notIn('safari13');
    it('Внешний вид турнирной таблицы (Хоккей)', function() {
        return this.browser
            .url('/turbo?stub=sportcompetitiontable/hockey-phone.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector);
    });
});
