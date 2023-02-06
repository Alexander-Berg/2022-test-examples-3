describe('ns.ViewCollection', function() {
    describe('#forEachValidItem', function() {
        before(function() {
            ns.Model.define('m-collection', {
                isCollection: true
            });

            ns.ViewCollection.define('v-collection', {
                models: [ 'm-collection' ],
                split: {
                    byModel: 'm-collection',
                    intoViews: 'v-collection-item'
                },
                yateModule: 'common'
            });
        });

        beforeEach(function() {
            this.validView = { isValid: this.sinon.stub().returns(true) };
            this.invalidView = { isValid: this.sinon.stub().returns(false) };

            this.vCollection = ns.View.create('v-collection');
            this.cb = this.sinon.spy();
        });

        it('Должен вызвать колбек для каждого валидного вида', function() {
            var that = this;

            this.sinon.stub(this.vCollection, 'forEachItem').callsFake(function(cb) {
                [ that.validView, that.validView, that.validView ].forEach(function(view) {
                    cb(view);
                });
            });

            this.vCollection.forEachValidItem(this.cb);

            expect(this.cb).to.have.callCount(3);
        });

        it('Не должен вызывать колбек для невалидных видов', function() {
            var that = this;

            this.sinon.stub(this.vCollection, 'forEachItem').callsFake(function(cb) {
                [ that.invalidView, that.invalidView, that.validView ].forEach(function(view) {
                    cb(view);
                });
            });

            this.vCollection.forEachValidItem(this.cb);

            expect(this.cb).to.have.callCount(1);
            expect(this.cb).to.be.calledWith(this.validView);
        });
    });
});
