specs({
    feature: 'EmcButton',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычная кнопка', function() {
        return this.browser
            .url('/turbo?stub=emcbutton/default.json')
            .yaWaitForVisible(PO.emcButton(), 'Кнопка не появилась')
            .assertView('emcbutton', PO.emcButton());
    });

    hermione.only.notIn('safari13');
    it('Кнопка с белым контуром размера m', function() {
        return this.browser
            .url('/turbo?stub=emcbutton/border-white-m.json')
            .yaWaitForVisible(PO.emcButton(), 'Кнопка не появилась')
            .assertView('emcbutton', PO.emcButton());
    });

    hermione.only.notIn('safari13');
    it('Кнопка с черным контуром размера l', function() {
        return this.browser
            .url('/turbo?stub=emcbutton/border-black-l.json')
            .yaWaitForVisible(PO.emcButton(), 'Кнопка не появилась')
            .assertView('emcbutton', PO.emcButton());
    });

    hermione.only.notIn('safari13');
    it('Кнопка с глобальной темой размера xs', function() {
        return this.browser
            .url('/turbo?stub=emcbutton/base-xs.json')
            .yaWaitForVisible(PO.emcButton(), 'Кнопка не появилась')
            .assertView('emcbutton', PO.emcButton());
    });

    hermione.only.notIn('safari13');
    it('Жёлтая кнопка размера xl', function() {
        return this.browser
            .url('/turbo?stub=emcbutton/action-xl.json')
            .yaWaitForVisible(PO.emcButton(), 'Кнопка не появилась')
            .assertView('emcbutton', PO.emcButton());
    });
});
