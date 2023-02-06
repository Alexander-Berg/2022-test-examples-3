specs({
    feature: 'EmcButtonList',
}, () => {
    hermione.only.notIn('safari13');
    it('Кнопки размера xs, с выравниванием по центру, с фоном секции', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/center-size-xs.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Кнопки размера s, с выравниванием по левому краю и с отступами секции m', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/left-size-s-paddings-m.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Кнопки размера m, с выравниванием по правому краю, и с фоном секции', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/right-size-m.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Кнопки размера l, с выравниванием по центру, и с фоном секции', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/center-size-l.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Кнопки размера xl, с выравниванием по левому краю, и с фоном секции', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/left-size-xl.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Список из 5 кнопок', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/five-items.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Кнопки без дисклеймера', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/wout-disclamer.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Кнопки c дисклеймером(emc-rich-text)', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/disclamer-emc-rich-text.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Текст кнопки c emc-rich-text', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/button-emc-rich-text.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Кнопки размера s, с выравниванием по левому краю и с отступами секции m (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/left-size-s-paddings-m-columns.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });

    hermione.only.notIn('safari13');
    it('Список из 5 кнопок (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcbuttonlist/five-items-columns.json')
            .yaWaitForVisible(PO.emcButtonList(), 'Кнопки не появились')
            .assertView('emcbuttonlist', PO.emcButtonList());
    });
});
