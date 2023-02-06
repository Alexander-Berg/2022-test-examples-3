specs({
    feature: 'Детальный список результатов матчей',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид', function() {
        const selector = '.sport-result-detail-list';

        return this.browser
            .url('/turbo?stub=sportresultdetaillist/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('default', selector);
    });
});
