specs({
    feature: 'LcJobsCards',
}, () => {
    const imagesToIgnore = ['.lc-jobs-card__image', '.lc-jobs-card__header-wrapper img'];

    it('default_cols_2', function() {
        return this.browser
            .url('turbo?stub=lcjobscards/default_cols_2.json')
            .yaWaitForVisible(PO.lcJobsCards(), 30000, 'Блок Jobs.Cards не появился')
            .moveToObject(PO.lcJobsCard())
            .assertView('default_cols_2', PO.lcJobsCards(), { ignoreElements: imagesToIgnore });
    });

    it('default_cols_3', function() {
        return this.browser
            .url('turbo?stub=lcjobscards/default_cols_3.json')
            .yaWaitForVisible(PO.lcJobsCards(), 30000, 'Блок Jobs.Cards не появился')
            .moveToObject(PO.lcJobsCard())
            .assertView('default_cols_3', PO.lcJobsCards(), { ignoreElements: imagesToIgnore });
    });

    it('default_cols_4', function() {
        return this.browser
            .url('turbo?stub=lcjobscards/default_cols_4.json')
            .yaWaitForVisible(PO.lcJobsCards(), 30000, 'Блок Jobs.Cards не появился')
            .moveToObject(PO.lcJobsCard())
            .assertView('default_cols_4', PO.lcJobsCards(), { ignoreElements: imagesToIgnore });
    });

    it('with_images_cols_2', function() {
        return this.browser
            .url('turbo?stub=lcjobscards/withImages_cols_2.json')
            .yaWaitForVisible(PO.lcJobsCards(), 30000, 'Блок Jobs.Cards не появился')
            .moveToObject(PO.lcJobsCard())
            .assertView('with_images_cols_2', PO.lcJobsCards(), { ignoreElements: imagesToIgnore });
    });

    it('with_images_cols_3', function() {
        return this.browser
            .url('turbo?stub=lcjobscards/withImages_cols_3.json')
            .yaWaitForVisible(PO.lcJobsCards(), 30000, 'Блок Jobs.Cards не появился')
            .moveToObject(PO.lcJobsCard())
            .assertView('with_images_cols_3', PO.lcJobsCards(), { ignoreElements: imagesToIgnore });
    });

    it('with_images_cols_4', function() {
        return this.browser
            .url('turbo?stub=lcjobscards/withImages_cols_4.json')
            .yaWaitForVisible(PO.lcJobsCards(), 30000, 'Блок Jobs.Cards не появился')
            .moveToObject(PO.lcJobsCard())
            .assertView('with_images_cols_4', PO.lcJobsCards(), { ignoreElements: imagesToIgnore });
    });
});
