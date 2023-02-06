specs({
    feature: 'Детальный результат команд в матче',
}, () => {
    it('Текущий матч', function() {
        const selector = '.sport-result-detail-team-score_size-l';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/in_progress.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('in_progress', selector);
    });

    it('Перерыв', function() {
        const selector = '.sport-result-detail-team-score_size-l';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/halftime.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('halftime', selector);
    });

    it('Прошедший матч', function() {
        const selector = '.sport-result-detail-team-score_size-l';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/finished.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('finished', selector);
    });

    it('Будущий матч', function() {
        const selector = '.sport-result-detail-team-score_size-l';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/not_started.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('not_started', selector);
    });

    it('Национальные команды', function() {
        const selector = '.sport-result-detail-team-score_size-l';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/national.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('national', selector);
    });

    it('Буллиты', function() {
        const selector = '.sport-result-detail-team-score_size-l';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/shootout.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('shootout', selector);
    });

    it('Овертайм', function() {
        const selector = '.sport-result-detail-team-score_size-l';

        return this.browser
            .url('/turbo?stub=sportresultdetailteamscore/overtime.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('overtime', selector);
    });
});
