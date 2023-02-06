describe('filters', function() {
    beforeEach(function() {
        this.mFilters = ns.Model.get('filters');

        this.data = {};
    });

    describe('#getMasterRequest', function() {
        it('должен вернуть модель с параметром master', function() {
            expect(this.mFilters.getMasterRequest().params.master).to.be.ok;
        });
    });
});

