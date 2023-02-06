specs({
    feature: 'newsEmbedStatus',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=newsembedstatus/live.json')
            .yaWaitForVisible(PO.blocks.newsEmbedStatus(), 'Блок не появился')
            .assertView('plain', PO.blocks.newsEmbedStatus());
    });
});
