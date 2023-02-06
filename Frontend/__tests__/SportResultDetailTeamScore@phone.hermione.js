specs({
    feature: 'Детальный результат команд в матче',
}, () => {
    hermione.only.notIn('safari13');
    it('Текущий матч', function() {
        const selector = '.sport-result-detail-team-score_size-s';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/in_progress.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('in_progress', selector);
    });

    hermione.only.notIn('safari13');
    it('Перерыв', function() {
        const selector = '.sport-result-detail-team-score_size-s';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/halftime.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('halftime', selector);
    });

    hermione.only.notIn('safari13');
    it('Прошедший матч', function() {
        const selector = '.sport-result-detail-team-score_size-s';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/finished.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('finished', selector);
    });

    hermione.only.notIn('safari13');
    it('Будущий матч', function() {
        const selector = '.sport-result-detail-team-score_size-s';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/not_started.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('not_started', selector);
    });

    hermione.only.notIn('safari13');
    it('Национальные команды', function() {
        const selector = '.sport-result-detail-team-score_size-s';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/national.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('national', selector);
    });

    hermione.only.notIn('safari13');
    it('Буллиты', function() {
        const selector = '.sport-result-detail-team-score_size-s';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/shootout.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('shootout', selector);
    });

    hermione.only.notIn('safari13');
    it('Овертайм', function() {
        const selector = '.sport-result-detail-team-score_size-s';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/overtime.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('overtime', selector);
    });
});
