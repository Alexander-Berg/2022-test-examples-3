describe('Daria.vComposeLabels', function() {

    beforeEach(function() {
        ns.Model.get('compose-message').setData({});
        this.view = ns.View.create('compose-labels');
        this.labels = ns.Model.get('labels');
        this.data = {
            name: "Test",
            count: 1,
            color: "ff3f30",
            user: true,
            lid: "test-lid"
        };
        this.sinon.stub(this.labels, 'getLabelById').returns(this.data);
    });

    describe('#getDataForLabel', function() {

        it('Должен вернуть информацию о метке по id', function() {
            expect(this.view.getDataForLabel("test-lid")).to.be.equal(this.data);
        });
    });

    describe('#getDataForLabels', function() {

        beforeEach(function() {
            this.lids = ["test-lid"];
            ns.Model.get('compose-message').set('.lids', this.lids);
        });

        it('Должен вернуть массив', function() {
            expect(this.view.getDataForLabels()).to.be.an('array');
        });

        it('Должен вернуть столько же элементов, сколько id', function() {
            _.times(4, function() {
                this.lids.push("test-lid");
            }, this);

            expect(this.view.getDataForLabels().length).to.be.equal(5);
        });

        it('Должен вернуть инфорцию по меткам', function() {
            expect(this.view.getDataForLabels()[0]).to.be.equal(this.data);
        });
    });
});
