specs({
    feature: 'LcJobsEntityList',
}, () => {
    hermione.only.notIn(['safari13']);
    it('Базовый вид', function() {
        return this.browser
            .url('turbo?stub=lcjobsentitylist/default.json')
            .yaWaitForVisible(PO.lcJobsEntityList(), 'Список не появился')
            .assertView('default', PO.lcJobsEntityList());
    });

    hermione.only.notIn(['safari13']);
    it('Города', function() {
        return this.browser
            .url('turbo?stub=lcjobsentitylist/locations.json')
            .yaWaitForVisible(PO.lcJobsEntityList(), 'Список не появился')
            .assertView('locations', PO.lcJobsEntityList());
    });

    hermione.only.notIn('safari13');
    it('Профессии', function() {
        return this.browser
            .url('turbo?stub=lcjobsentitylist/professions.json')
            .yaWaitForVisible(PO.lcJobsEntityList(), 'Список не появился')
            .assertView('professions', PO.lcJobsEntityList());
    });

    hermione.only.notIn('safari13');
    it('Сервисы', function() {
        return this.browser
            .url('turbo?stub=lcjobsentitylist/services.json')
            .yaWaitForVisible(PO.lcJobsEntityList(), 'Список не появился')
            .assertView('services', PO.lcJobsEntityList());
    });

    hermione.only.notIn(['safari13']);
    it('Пустой список', function() {
        return this.browser
            .url('turbo?stub=lcjobsentitylist/empty.json')
            .yaWaitForVisible(PO.lcJobsEntityList(), 'Список не появился')
            .assertView('empty', PO.lcJobsEntityList());
    });

    hermione.only.notIn(['safari13']);
    it('Ошибка загрузки', function() {
        return this.browser
            .url('turbo?stub=lcjobsentitylist/error.json')
            .yaWaitForVisible(PO.lcJobsEntityList(), 'Список не появился')
            .assertView('error', PO.lcJobsEntityList());
    });
});
