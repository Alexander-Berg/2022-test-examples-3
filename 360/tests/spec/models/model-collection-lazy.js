require('models/queues/queue-request');

const ModelCollectionLazy = require('models/model-collection-lazy');

describe('Ленивая подгрузка:', () => {
    before(() => {
        ns.Model.define('test-item', {
            params: {
                id: null
            }
        });

        ModelCollectionLazy.define('test-tweets', {
            params: {
                username: null
            },
            split: {
                model_id: 'test-item',
                items: '.data',
                params: {
                    id: '.id'
                }
            },
            sizePortion: 5
        });

        ModelCollectionLazy.define('test-pages', {
            offsetProperty: 'skip',
            amountProperty: 'perpage',
            split: {
                model_id: 'test-item',
                items: '.data',
                params: {
                    id: '.id'
                }
            },
            sizePortion: 5
        });

        ModelCollectionLazy.define('test-wishes', {
            split: {
                model_id: 'test-item',
                items: '.data',
                params: {
                    id: '.id'
                }
            },
            methods: {
                offsetFilter: function(item) {
                    return !item.get('.state.handmade');
                }
            },
            sizePortion: 5
        });

        ModelCollectionLazy.define('test-params', {
            split: {
                model_id: 'test-item',
                items: '.data',
                params: {
                    id: '.id'
                }
            },
            sizePortion: 5,
            params: function() {
                return {
                    hello: 'you'
                };
            }
        });
    });

    beforeEach(function(done) {
        createFakeXHR();

        this.model = ns.Model.get('test-tweets', { username: 'Test' });
        this.fixture = [
            {
                data: [
                    {
                        id: '1',
                        text: 'Alfa'
                    },
                    {
                        id: '2',
                        text: 'Bravo'
                    },
                    {
                        id: '3',
                        text: 'Charlie'
                    },
                    {
                        id: '4',
                        text: 'Delta'
                    },
                    {
                        id: '5',
                        text: 'Echo'
                    }
                ]
            },
            {
                data: [
                    {
                        id: '6',
                        text: 'Foxtrot'
                    },
                    {
                        id: '7',
                        text: 'Golf'
                    },
                    {
                        id: '8',
                        text: 'Hotel'
                    },
                    {
                        id: '9',
                        text: 'India'
                    },
                    {
                        id: '10',
                        text: 'Juliett'
                    }
                ]
            },
            {
                data: [
                    {
                        id: '11',
                        text: 'Kilo'
                    },
                    {
                        id: '12',
                        text: 'Lima'
                    },
                    {
                        id: '13',
                        text: 'Mike'
                    },
                    {
                        id: '14',
                        text: 'November'
                    },
                    {
                        id: '15',
                        text: 'Oscar'
                    }
                ]
            }
        ];

        addResponseModel(this.fixture[0]);

        ns.Model.get('queueRequest').push(this.model).then(done.bind(null, null));
    });

    afterEach(() => {
        deleteFakeXHR();
    });

    describe('модель', () => {
        it('должна разделять входные данные на подмодели', function() {
            expect(this.model.models).to.have.length(5);
        });

        it('должна отправлять первый запрос с нулевым уровнем загрузки', () => {
            expect(getLastRequest().requestBody).to.contain('offset.0=0');
        });

        it('должна отправлять первый запрос с размером порции для подгрузки', () => {
            expect(getLastRequest().requestBody).to.contain('amount.0=5');
        });

        it('должна возвращать Vow.Promise при запросе порции', function(done) {
            addResponseModel(this.fixture[1]);

            const fetching = this.model.fetchNext();

            expect(fetching).to.be.a(Vow.Promise);

            fetching.then(() => {
                done();
            });
        });

        it('должна передавать количество имеющихся подмоделей при запросе', function(done) {
            const model = this.model;

            addResponseModel([this.fixture[1], this.fixture[2]]);

            model.fetchNext().then(() => {
                expect(getLastRequest().requestBody).to.contain('offset.0=5');

                model.fetchNext().then(() => {
                    expect(getLastRequest().requestBody).to.contain('offset.0=10');
                    done();
                });
            });
        });

        it('должна передавать id последнего элемента в коллекции при запросе', function(done) {
            const model = this.model;

            addResponseModel([this.fixture[1], this.fixture[2]]);

            model.fetchNext().then(() => {
                expect(getLastRequest().requestBody).to.contain('idItemLast.0=5');

                model.fetchNext().then(() => {
                    expect(getLastRequest().requestBody).to.contain('idItemLast.0=10');
                    done();
                });
            });
        });

        it('должна содержать верное количество подмоделей после загрузок порций', function(done) {
            const model = this.model;

            addResponseModel([this.fixture[1], this.fixture[2]]);

            model.fetchNext().then(() => {
                expect(model.models).to.have.length(10);

                model.fetchNext().then(() => {
                    expect(model.models).to.have.length(15);
                    done();
                });
            });
        });

        it('должна использовать в качестве названия свойства отступа переданное значение', function(done) {
            const model = ns.Model.get('test-pages', null).setData(this.fixture[0]);

            addResponseModel(this.fixture[1]);

            model.fetchNext().then(() => {
                expect(getLastRequest().requestBody).to.contain('skip.0=5');
                done();
            });
        });

        it('должна использовать в качестве названия свойства размера порции переданное значение', function(done) {
            const model = ns.Model.get('test-pages', null).setData(this.fixture[0]);

            addResponseModel(this.fixture[1]);

            model.fetchNext().then(() => {
                expect(getLastRequest().requestBody).to.contain('perpage.0=5');
                done();
            });
        });

        it('должна не учитывать подмодели созданные вручную при загрузке', function(done) {
            const model = ns.Model.get('test-wishes', null).setData(this.fixture[0]);

            // Модель, которая должна придти в следующей порции.
            const item = ns.Model.get('test-item', { id: '6' }).setData({
                id: '6',
                state: {
                    handmade: true
                }
            });

            addResponseModel(this.fixture[1]);
            model.insert(item);

            model.fetchNext().then(() => {
                expect(getLastRequest().requestBody).to.contain('offset.0=5');
                expect(item.get('.text')).to.be('Foxtrot');

                done();
            });
        });

        describe('должна выставлять флаг окончания подгрузки при получении порции размером меньше, чем `sizePortion`', () => {
            it('в случае обычных info.params', function(done) {
                const model = ns.Model.get('test-wishes', null).setData(this.fixture[0]);

                addResponseModel([this.fixture[1], { data: this.fixture[2].data.slice(0, 3) }]);

                model.fetchNext().then(() => {
                    model.fetchNext().then(() => {
                        expect(model.isComplete()).to.be(true);
                        done();
                    });
                });
            });

            it('в случае info.params заданных функцией', function(done) {
                const model = ns.Model.get('test-params', { hello: 'me' }).setData(this.fixture[0]);

                addResponseModel([{ data: this.fixture[1].data.slice(0, 3) }]);

                model.fetchNext().then(() => {
                    expect(model.isComplete()).to.be(true);
                    done();
                });
            });
        });

        it('должна выставлять флаг окончания подгрузки при получении пустой порции', function(done) {
            const model = ns.Model.get('test-wishes', null).setData(this.fixture[0]);

            addResponseModel([this.fixture[1], { data: [] }]);

            model.fetchNext().then(() => {
                model.fetchNext().then(() => {
                    expect(model.isComplete()).to.be(true);
                    done();
                });
            });
        });

        it('должна при двойном запросе возвращать промис от первого запроса', function(done) {
            const model = ns.Model.get('test-wishes', null).setData(this.fixture[0]);

            addResponseModel(this.fixture[1]);

            const firstFetch = model.fetchNext();
            const secondFetch = model.fetchNext();

            expect(secondFetch).to.equal(firstFetch);

            firstFetch.then(done.bind(null, null));
        });

        it('должна удалять порционные модели из кешей', function(done) {
            const model = this.model;

            addResponseModel(this.fixture[1]);

            model.fetchNext().then(() => {
                expect(ns.Model.getValid('test-tweets', { username: 'Test', offset: 5 })).to.not.be.ok();
                done();
            });
        });

        it('не должна удалять порционную модель, если это сама коллекция (модель создана, но к ней ещё не запрашивались данные)', function(done) {
            const model = ns.Model.get('test-tweets', { username: 'Some user' });
            addResponseModel(this.fixture[0]);

            model.fetchNext().then(() => {
                expect(ns.Model.getValid('test-tweets', { username: 'Some user' })).to.be.ok();
                done();
            });
        });

        it('не должна запрашивать порции будучи загруженной с пустым набором подмоделей', (done) => {
            const model = ns.Model.get('test-wishes', null).setData({ data: [] });

            model.fetchNext().then(done.bind(null, null));
        });

        it('не должна запрашивать порции будучи загруженной с количеством подмоделей меньшим, чем `sizePortion`', function(done) {
            const model = ns.Model.get('test-wishes', null).setData({ data: this.fixture[0].data.slice(0, 3) });

            model.fetchNext().then(done.bind(null, null));
        });

        it('должна уметь работать с функцией в params', function() {
            const model = ns.Model.get('test-params', { hello: 'you' });
            addResponseModel(this.fixture[1]);

            expect(model.params).to.have.property('hello', 'you');
            expect(model.params).to.have.property('offset', 0);
            expect(model.params).to.have.property('amount', 5);
            expect(model.params).to.have.property('idItemLast', null);
        });
    });

    describe('вид', () => {
        before(function() {
            const insertCallback = this.insertCallback = sinon.spy();
            const loadingStart = this.loadingStart = sinon.spy();
            const loadingStop = this.loadingStop = sinon.spy();
            const loadingComplete = this.loadingComplete = sinon.spy();

            ns.View.define('app');

            ns.View.define('test-view-tweet', {
                models: ['test-item']
            });

            ns.ViewCollection.define('test-view-tweets', {
                models: ['test-tweets'],
                events: {
                    'ns-view-htmlinit': 'onhtmlinit',
                    'ns-view-htmldestroy': 'onhtmldestroy'
                },
                split: {
                    intoViews: 'test-view-tweet',
                    byModel: 'test-tweets'
                },
                methods: {
                    onhtmlinit: function() {
                        this.models['test-tweets'].on('ns-model-insert', insertCallback);
                        this.models['test-tweets'].on('ns-model-loading-start', loadingStart);
                        this.models['test-tweets'].on('ns-model-loading-finish', loadingStop);
                        this.models['test-tweets'].on('ns-model-loading-complete', loadingComplete);
                    },
                    onhtmldestroy: function() {
                        this.models['test-tweets'].off('ns-model-insert', insertCallback);
                        this.models['test-tweets'].off('ns-model-loading-start', loadingStart);
                        this.models['test-tweets'].off('ns-model-loading-finish', loadingStop);
                        this.models['test-tweets'].off('ns-model-loading-complete', loadingComplete);
                    }
                }
            });

            ns.layout.define('test-app', {
                app: 'test-view-tweets'
            });
        });

        beforeEach(function(done) {
            this.insertCallback.reset();
            this.loadingStart.reset();
            this.loadingStop.reset();
            this.loadingComplete.reset();

            this.app = ns.View.create('app');

            addResponseModel([this.fixture[1], { data: [] }]);

            const layout = ns.layout.page('test-app');

            new ns.Update(this.app, layout, { username: 'Test' })
                .start()
                .then(() => {
                    this.firstNode = this.app.node.querySelector('.ns-view-test-view-tweets').cloneNode(true);

                    this.model.fetchNext().then(() => {
                        new ns.Update(this.app, layout, { username: 'Test' })
                            .start()
                            .then(() => {
                                this.secondNode = this.app.node.querySelector('.ns-view-test-view-tweets').cloneNode(true);

                                this.model.fetchNext().then(() => {
                                    new ns.Update(this.app, layout, { username: 'Test' })
                                        .start()
                                        .then(() => {
                                            this.thirdNode = this.app.node.querySelector('.ns-view-test-view-tweets').cloneNode(true);

                                            // Холостой запрос
                                            this.model.fetchNext().then(done.bind(null, null));
                                        });
                                });
                            });
                    });
                });
        });

        afterEach(function() {
            delete this.app;
            delete this.firstNode;
            delete this.secondNode;
            delete this.thirdNode;
        });

        after(function() {
            delete this.insertCallback;
            delete this.loadingStart;
            delete this.loadingStop;
            delete this.loadingComplete;
        });

        it('должен изначально отрисовать по ноде на каждую подмодель', function() {
            expect(this.firstNode.querySelectorAll('.ns-view-test-view-tweet')).to.have.length(5);
        });

        it('должен быть верно дорисован после подгрузки порции', function() {
            expect(this.secondNode.querySelectorAll('.ns-view-test-view-tweet')).to.have.length(10);
        });

        it('должен быть верно отрисован после окончания загрузки', function() {
            expect(this.thirdNode.querySelectorAll('.ns-view-test-view-tweet')).to.have.length(10);
        });

        it('должен ловить события вставки элементов в подмодель', function() {
            // Один раз, потому что во последующие разы вставка
            // не должна произойти.
            expect(this.insertCallback.callCount).to.equal(1);
        });

        it('должен ловить события `ns-model-loading-start`', function() {
            expect(this.loadingStart.callCount).to.equal(2);
        });

        it('должен ловить события `ns-model-loading-finish`', function() {
            expect(this.loadingStop.callCount).to.equal(2);
        });

        it('должен ловить события `ns-model-loading-complete`', function() {
            expect(this.loadingComplete.callCount).to.equal(1);
        });
    });
});
