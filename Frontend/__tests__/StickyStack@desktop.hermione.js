specs({
    feature: 'sticky-stack',
}, () => {
    it('Стики через заданый промежуток', function() {
        return this.browser
            .url('/turbo?stub=sticky-stack%2Ffixed_step.json')
            .yaAssertViewportView('fixed-start')
            .scroll('.paragraph:nth-child(2)')
            .yaAssertViewportView('fixed-2-paragraph')
            .scroll('.paragraph:nth-child(5)')
            .yaAssertViewportView('fixed-5-paragraph');
    });

    it('Стики распределенные по высоте', function() {
        return this.browser
            .url('/turbo?stub=sticky-stack%2Fauto.json')
            .yaAssertViewportView('fluid-start')
            .scroll('.paragraph:nth-child(2)')
            .yaAssertViewportView('fluid-2-paragraph')
            .scroll('.paragraph:nth-child(5)')
            .yaAssertViewportView('fluid-5-paragraph');
    });

    it('Один стик', function() {
        return this.browser
            .url('/turbo?stub=sticky-stack%2Fsingle.json')
            .yaAssertViewportView('single-start')
            .scroll('.paragraph:nth-child(2)')
            .yaAssertViewportView('single-2-paragraph')
            .scroll('.paragraph:nth-child(5)')
            .yaAssertViewportView('single-5-paragraph');
    });
});
