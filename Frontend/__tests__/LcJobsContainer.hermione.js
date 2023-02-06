specs({
    feature: 'LcJobsContainer',
}, () => {
    hermione.only.notIn(['safari13']);
    it('default', function() {
        return this.browser
            .url('turbo?stub=lcjobscontainer/default.json')
            .assertView('default', PO.lcJobsContainer());
    });

    hermione.only.notIn(['safari13']);
    it('expanded', function() {
        return this.browser
            .url('turbo?stub=lcjobscontainer/expanded.json')
            .assertView('default', PO.lcJobsContainer());
    });
});
