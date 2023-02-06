specs({
    feature: 'SportAdvert',
}, () => {
    hermione.only.notIn('safari13');
    it('реклама должна быть видима', function() {
        return this.browser
            .url('/turbo?stub=sportadvert/default.json&exp_flags=adv-disabled=0&hermione_advert=stub&exp_flags=force-react-advert=1&exp_flags=adv-plainjs=none')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.sportAdvert(), 'Блок не появился')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .assertView('plain', PO.blocks.sportAdvert());
    });
});
