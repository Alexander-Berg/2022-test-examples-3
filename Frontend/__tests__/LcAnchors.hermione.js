function test(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcanchors/${page}.json`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPage(PO.lcAnchors())
            .assertView(page, PO.lcAnchors());
    };
}

specs({
    feature: 'LcAnchors',
}, () => {
    hermione.only.notIn('safari13');
    it('Незакрепленная', test('notFixed'));
    hermione.only.notIn('safari13');
    it('Закрепленная', test('fixed'));
    hermione.only.notIn('safari13');
    it('Скролл до секции 3', function() {
        return this.browser
            .url('/turbo?stub=lcanchors/fixed.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .pause(1000)
            .execute(function() {
                var section = document.getElementById('section-3');

                window.scrollTo(0, section.offsetTop);
            })
            .pause(1000)
            .assertView('fixedScroll', PO.lcAnchors());
    });
    it('Исходное положение после скролла', function() {
        return this.browser
            .url('/turbo?stub=lcanchors/fixed.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .pause(1000)
            .execute(function() {
                var section = document.getElementById('section-3');

                window.scrollTo(0, section.offsetTop);
            })
            .pause(1000)
            .yaScrollPage(0)
            .assertView('fixedScrollToOriginal', PO.lcAnchors());
    });
});
