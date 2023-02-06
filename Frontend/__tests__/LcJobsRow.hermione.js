specs({
    feature: 'LcJobsRow',
}, () => {
    hermione.only.notIn('safari13');
    it('left_offset_0', function() {
        return this.browser
            .url('turbo?stub=lcjobsrow/left_offset_0.json')
            .assertView('default', PO.lcJobsContainer());
    });

    hermione.only.notIn('safari13');
    it('left_offset_1', function() {
        return this.browser
            .url('turbo?stub=lcjobsrow/left_offset_1.json')
            .assertView('default', PO.lcJobsContainer());
    });

    hermione.only.notIn('safari13');
    it('left_offset_2', function() {
        return this.browser
            .url('turbo?stub=lcjobsrow/left_offset_2.json')
            .assertView('default', PO.lcJobsContainer());
    });
});
