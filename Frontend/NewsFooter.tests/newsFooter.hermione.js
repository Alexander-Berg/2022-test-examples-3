specs({
    feature: 'Новостной футер',
}, () => {
    hermione.only.notIn('safari13');
    it('корректно отображается', function() {
        return this.browser
            .url('/turbo?stub=newsfooter/default.json')
            .yaWaitForVisible(PO.blocks.newsFooter(), 'Блок не появился')
            .assertView('plain', PO.blocks.newsFooter());
    });
});
