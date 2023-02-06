specs({
    feature: 'EmcBadgeList',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычные бейджи', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/default.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });

    hermione.only.notIn('safari13');
    it('Бейджи с выравниванием по центру', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/center.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });

    hermione.only.notIn('safari13');
    it('Бейджи с выравниванием по правому краю', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/right.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });

    hermione.only.notIn('safari13');
    it('Список из 5 бейджей', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/five-items.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });

    hermione.only.notIn('safari13');
    it('Бейджи без заголовка', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/wout-title.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });

    hermione.only.notIn('safari13');
    it('Бейджи без заголовка и подписей', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/wout-title-and-descriptions.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });

    hermione.only.notIn('safari13');
    it('Бейджи c emc-rich-text', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/emc-rich-text.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });

    hermione.only.notIn('safari13');
    it('Обычные бейджи (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/default-columns.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });

    hermione.only.notIn('safari13');
    it('Бейджи с выравниванием по центру (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcbadgelist/center-columns.json')
            .yaWaitForVisible(PO.emcBadgeList(), 'Бейджи не появились')
            .assertView('emcbadgelist', PO.emcBadgeList());
    });
});
