specs({
    feature: 'Детальный результат матча',
}, () => {
    hermione.only.notIn('safari13');
    it('Текущий матч', function() {
        const selector = '.sport-result-detail';

        return this.browser
            .url('?stub=sportresultdetail/in_progress.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('in_progress', selector);
    });

    hermione.only.notIn('safari13');
    it('Перерыв', function() {
        const selector = '.sport-result-detail';

        return this.browser
            .url('?stub=sportresultdetail/halftime.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('halftime', selector);
    });

    hermione.only.notIn('safari13');
    it('Прошедший матч', function() {
        const selector = '.sport-result-detail';

        return this.browser
            .url('?stub=sportresultdetail/finished.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('finished', selector);
    });

    hermione.only.notIn('safari13');
    it('Будущий матч', function() {
        const selector = '.sport-result-detail';

        return this.browser
            .url('?stub=sportresultdetail/not_started.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('not_started', selector);
    });

    hermione.only.notIn('safari13');
    it('Национальные команды', function() {
        const selector = '.sport-result-detail';

        return this.browser
            .url('?stub=sportresultdetail/national.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('national', selector);
    });

    hermione.only.notIn('safari13');
    it('Буллиты', function() {
        const selector = '.sport-result-detail';

        return this.browser
            .url('?stub=sportresultdetail/shootout.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('shootout', selector);
    });

    hermione.only.notIn('safari13');
    it('Овертайм', function() {
        const selector = '.sport-result-detail';

        return this.browser
            .url('?stub=sportresultdetail/overtime.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('overtime', selector);
    });

    hermione.only.in(['chrome-desktop', 'firefox'], 'Комментарии к матчам есть только на десктопе');
    hermione.only.notIn('safari13');
    it('С футером', function() {
        const selector = '.card_theme_sport-result';

        return this.browser
            .url('?stub=sportresultdetail/with_footer.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('with-footer', selector);
    });
});
