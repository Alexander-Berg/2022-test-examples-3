describe('map-up', function() {
    describe('MapUp', function() {
        beforeEach(function() {
            this.address = 'whatever';
            this.blockParams = { 'address': this.address, 'ids': '1' };
            this.block = ns.View.create('map-up', this.blockParams);
        });

        describe('open', function() {
            beforeEach(function(done) {
                const addressLocation = {
                    lat: '60',
                    lon: '60'
                };
                this.popupNode = document.createElement('div');
                this.model = {
                    _data: { addressLocation },
                    getData: (function () {
                        return this.model._data;
                    }).bind(this)
                };
                this.popupApi = {
                    open: sinon.stub(),
                    setContent: sinon.stub(),
                    trigger: sinon.stub(),
                    on: sinon.stub(),
                    destroy: sinon.stub()
                };

                this.sinon.stub(Jane, 'tt').returns(this.popupNode);
                this.sinon.stub(this.block, 'destroy');

                this.sinon.stub(ns, 'Update').callsFake(function() {
                    return {
                        render: Vow.fulfill
                    };
                });

                this.sinon.stub(nb, 'block').callsFake(function() {
                    return this.popupApi;
                }.bind(this));
                this.sinon.stub(ns.Model, 'get').callsFake(function() {
                    return this.model;
                }.bind(this));

                this.block.open().then(function() {
                    done();
                });
            });

            it('should render the map-up template', function() {
                expect(Jane.tt.calledOnce).to.be.equal(true);
                expect(Jane.tt.calledWithExactly('message:map-up')).to.be.equal(true);
            });

            it('should initialize nb popup', function() {
                expect(nb.block.calledWithExactly(this.popupNode)).to.be.equal(true);
            });

            it('should set content to nb popup', function() {
                expect(this.popupApi.setContent).to.be.calledWithExactly(this.block.node);
            });

            it('should open nb popup', function() {
                expect(this.popupApi.open.calledOnce).to.be.equal(true);
            });
        });
    });
});
