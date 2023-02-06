describe('filter-search', function() {
    beforeEach(function() {
        this.hFilterSearch = ns.Model.get('filter-search');

        this.hLabels = ns.Model.get('labels');
        this.hLabels.setData({
            label: [
                { lid: '1', social: false, name: 'somelabel' },
                { lid: '2', social: true, name: 'vtnrf0githubcom' },
                { lid: '3', social: true, name: 'vtnrf0facebook' }
            ]
        });

        this.data = {
            envelopes: [
                { mid: '1', labels: [ '1' ], dtype: { name: 's-company' } },
                { mid: '2', labels: [ '2', '3' ] },
                { mid: '3', labels: [ '3' ] }
            ]
        };
    });

    describe('#preprocessData', function() {
        it("Должен обрабатывать только те конверты, которые не обрабатывались ранее", function() {
            var spy = this.sinon.spy(this.hFilterSearch, 'preprocessData');
            var spy0 = this.sinon.spy(ns.Model.get('filter-search', { mids: '1' }), 'preprocessData');
            var spy1 = this.sinon.spy(ns.Model.get('filter-search', { mids: '2' }), 'preprocessData');
            var spy2 = this.sinon.spy(ns.Model.get('filter-search', { mids: '3' }), 'preprocessData');

            this.data.envelopes[0].processed = true;

            this.hFilterSearch.setData(this.data);

            expect(spy0.called).to.not.be.ok;
            expect(spy1.calledOnce).to.be.ok;
            expect(spy2.calledOnce).to.be.ok;
        });
    });
});

