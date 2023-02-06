specs({
    feature: 'news-tray',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=newstray%2Fdefault.json')
            .yaWaitForVisible(PO.blocks.newsTray(), 'Шторка не загрузилась')
            .execute(function(selector) {
                document.querySelector(selector).setAttribute('style', 'position:fixed;width:100%;height:100%;');
            }, PO.page())
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('По нажатию на кнопку "Свернуть" шторка сворачивается', function() {
        return this.browser
            .url('/turbo?stub=newstray%2Fdefault.json')
            .yaWaitForVisible(PO.blocks.newsTray(), 'Шторка не загрузилась')
            .click(PO.blocks.newsTray.close())
            .getAttribute(PO.blocks.newsTray(), 'class')
            .then(value => assert.strictEqual(value.indexOf('news-tray_mode_closed') >= 0, true));
    });
});
