specs({
    feature: 'Просмоторщик коллекций в Новостях (news-media-viewer)',
}, () => {
    hermione.only.in(['chrome-phone', 'iphone', 'searchapp'], 'Только touch');
    hermione.only.notIn('safari13');
    it('Внешний вид просмоторщика коллекций', function() {
        // Рабтать нужно с PO.blocks.newsMediaViewer.content()
        // (сам PO.blocks.newsMediaViewer не имеет размера, т.к.
        // у детей position:fixed)
        return this.browser
            .url('?stub=newscollection/default.json')
            .yaWaitForVisible(PO.blocks.newsCollection())
            .click(PO.blocks.newsCollection.imageWrapper())
            .yaShouldBeVisible(PO.blocks.newsMediaViewer.content(), 'Просмоторщик должен появиться')
            .assertView('basic', PO.blocks.newsMediaViewer.content(), { ignoreElements: '.viewer-image__image' });
    });
});
