const ViewFetch = require('views/view-fetch');

describe('ViewFetch', () => {
    before(() => {
        ns.View.define('appViewFetch');

        ns.View.define('view1', {
            models: ['model1']
        });

        ViewFetch.define('fetchModel1', {
            models: ['model1']
        });

        ns.Model.define('model1');

        ns.layout.define('appViewFetch', {
            appViewFetch: {
                'box@': function() {
                    // TODO: для наглядности тут должна быть проверка не наличия модели,
                    // а данных этой модели
                    const model = ns.Model.getValid('model1', {});
                    if (!model) {
                        return 'fetchModel1';
                    }

                    if (model.get('.attr')) {
                        return 'view2';
                    } else {
                        return 'view1';
                    }
                }
            }
        });
    });

    beforeEach(function() {
        this.viewApp = ns.View.create('appViewFetch');

        createFakeXHR();
        addResponseModel({
            attr: false
        });
    });

    afterEach(function() {
        deleteFakeXHR();
        delete this.viewApp;
    });

    describe('basic usage', () => {
        beforeEach(function(done) {
            const that = this;

            const layout = ns.layout.page('appViewFetch', {});

            sinon.stub(this.helperInit = require('helpers/init'), 'onAppReady', (f) => {
                return Vow.fulfill().then(f);
            });

            sinon.stub(ns.page, 'go', () => {
                const layout = ns.layout.page('appViewFetch', {});
                new ns.Update(that.viewApp, layout, {})
                    .start()
                    .then(() => {
                        done();
                    });
            });

            new ns.Update(this.viewApp, layout, {}).start();
        });

        afterEach(function() {
            ns.page.go.restore();
            this.helperInit.onAppReady.restore();
        });

        it('should call ns.page.go once', () => {
            expect(ns.page.go.callCount).to.be(1);
        });

        it('should render view1', function() {
            expect(this.viewApp.$node.find('.ns-view-view1').length).to.be(1);
        });

        it('should not have visible fetchModel1 view', function() {
            expect(this.viewApp.$node.find('.ns-view-fetchModel1.ns-view-visible').length).to.be(0);
        });
    });
});
