specs({
    feature: 'advert-react',
}, () => {
    describe('Реклама-перетяжка в начале страницы', function() {
        hermione.only.notIn('safari13');
        it('Вертикальная ориентация', function() {
            return this.browser
                .url('?stub=advert%2Ftop.json&exp_flags=adv-disabled=0&hermione_advert=stub&exp_flags=force-react-advert=1&load-react-advert-script')
                .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
                .yaWaitForVisible(PO.advertItemInited(), 'Реклама не загрузилась')
                .yaAssertViewportView('plain');
        });

        hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
        hermione.only.notIn('safari13');
        it('Горизонтальная ориентация', function() {
            return this.browser
                .url('?stub=advert%2Ftop.json&exp_flags=adv-disabled=0&hermione_advert=stub&exp_flags=force-react-advert=1&load-react-advert-script')
                .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
                .yaWaitForVisible(PO.advertItemInited(), 'Реклама не загрузилась')
                .setOrientation('landscape')
                .yaAssertViewportView('plain-landscape');
        });
    });
});
