describe('Daria.mAbookContacts', function() {
    beforeEach(function() {
        this.model = ns.Model.get('abook-contacts');
    });

    describe('#loadMore', function() {
        beforeEach(function() {
            this.portionModels = [['abook-contact', 'abook-contact']];
            var that = this;
            this.promiseAbookContactsObject = {
                then: function(fn, ctx) {
                    return fn.call(ctx, that.portionModels);
                }
            };
            this.sinon.stub(ns, 'request').withArgs('abook-contacts').returns(this.promiseAbookContactsObject);
            this.sinon.stub(this.model, '_addModels');
            this.model.setData({
                contact: [],
                pager: {
                    'items-count': 52
                }
            });
            this.sinon.stub(this.model, 'models').value([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
        });

        it('должен запросить модели, начиная с последней загруженной', function() {
            var loadMoreParams = {
                pagesize: 30,
                skip: 10
            };

            this.model.loadMore();

            expect(ns.request).to.be.calledWith('abook-contacts', loadMoreParams);
        });

        it('должен запросить все незагруженные модели, начиная с последней загруженной, если вызван с параметром loadAll == true', function() {
            var loadAllParams = {
                pagesize: 42,
                skip: 10
            };

            this.model.loadMore(true);

            expect(ns.request).to.be.calledWith('abook-contacts', loadAllParams);
        });

        it('должен добавить модели после запроса', function() {
            var loadMoreParams = {
                pagesize: 30,
                skip: 10
            };

            this.model.loadMore();

            expect(this.model._addModels).to.be.calledWith(this.portionModels);
        });
    });

    describe('#_addModels', function() {
        beforeEach(function() {
            this.portionModels = ns.Model.get('abook-contacts', {tid: 123});
            this.portionModels.setData({
                contact: [],
                pager: {
                    'items-count': 52
                }
            });
            this.sinon.stub(this.portionModels, 'models').value(['abook-contact', 'abook-contact']);

            this.model.setData({
                contact: [],
                pager: {
                    'items-count': 52
                }
            });
            this.sinon.stubMethods(this.model, ['insert', 'set']);
        });

        it('должен обновить пейджер', function() {
            var oldPager = {'items-per-page': 10};
            var newPager = {'items-per-page': 10, 'items-count': 20};
            this.model.set('.pager', oldPager);
            this.portionModels.set('.pager', newPager);

            this.model._addModels([this.portionModels]);

            expect(this.model.set).to.be.calledWith('.pager', newPager);
        });

        it('должен вставить модели в коллекцию', function() {
            this.model._addModels([this.portionModels]);

            expect(this.model.insert).to.be.calledWith(['abook-contact', 'abook-contact']);
        });

        it('должен выставить статус OK, если модель была пустая', function() {
            this.model.data = null;

            this.model._addModels([this.portionModels]);

            expect(this.model.status).to.be.eql(this.model.STATUS.OK);
        });


        it('должен выставить статус OK, если модель была невалидная', function() {
            this.sinon.stub(this.model, 'isValid').returns(false);

            this.model._addModels([this.portionModels]);

            expect(this.model.status).to.be.eql(this.model.STATUS.OK);
        });
    });

});
