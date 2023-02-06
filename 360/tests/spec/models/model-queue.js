import ModelQueue from 'models/model-queue';

describe('Модель очереди', () => {
    before(function() {
        ModelQueue.define('my-queue', {
            methods: {
                handlePortion: function() {
                    return this.promiseHandle;
                }.bind(this)
            }
        });

        ModelQueue.define('my-queue-double', {
            limit: 2
        });
    });

    beforeEach(function() {
        this.promiseHandle = new Vow.Promise();
        this.queue = ns.Model.get('my-queue');

        this.callbackAdded = sinon.spy();
        this.callbackProcessed = sinon.spy();
        this.callbackProcessing = sinon.spy();
        this.callbackDrained = sinon.spy();

        sinon.spy(this.queue, 'doIteration');

        this.queue.on('added', this.callbackAdded);
        this.queue.on('processed', this.callbackProcessed);
        this.queue.on('processing', this.callbackProcessing);
        this.queue.on('drained', this.callbackDrained);

        this.queueDouble = ns.Model.get('my-queue-double');

        this.items = [
            { foo: 1 },
            { foo: 2 },
            { foo: 3 },
            { foo: 4 },
            { foo: 5 },
            { foo: 6 },
            { foo: 7 }
        ];
    });

    afterEach(function() {
        this.queue.doIteration.restore();

        delete this.promiseHandle;
        delete this.queue;
        delete this.queueDouble;
        delete this.items;

        delete this.callbackAdded;
        delete this.callbackProcessed;
        delete this.callbackProcessing;
        delete this.callbackDrained;
    });

    it('`limit` должен быть равен 10 при инициализации', function() {
        expect(this.queue.limit).to.be(10);
    });

    it('`limit` должен быть равен 2 при инициализации', function() {
        expect(this.queueDouble.limit).to.be(2);
    });

    it('при инициализации очередь не должна быть занята', function() {
        expect(this.queue.busy).not.to.be.ok();
    });

    it('при добавлении одного элемента собтие `added` должно броситься один раз', function() {
        this.queue.push(this.items[0]);
        expect(this.callbackAdded.calledOnce).to.be.ok();
    });

    it('при добавлении нескольких элементов собтие `added` должно броситься несколько раз', function() {
        this.queue.push(this.items[0]);
        this.queue.push(this.items[1]);
        this.queue.push(this.items[2]);
        this.queue.push(this.items[3]);
        expect(this.callbackAdded.callCount).to.be(4);
    });

    it('метод `push` должен вернуть промис', function() {
        expect(
            this.queue.push(this.items[0])
        ).to.be.a(Vow.Promise);
    });

    it('метод `push` должен дернуть `doIteration`', function() {
        this.queue.push(this.items[0]);
        expect(this.queue.doIteration.callCount).to.be(1);
    });

    it('при добавлении элемента в очередь должно произойти собитие `processing`', function() {
        this.queue.push(this.items[0]);
        this.promiseHandle.fulfill();
        expect(this.callbackProcessing.calledOnce).to.be.ok();
    });

    it('после завершения обработки порции должно произойти событие `processed`', function(done) {
        this.queue.push(this.items[0]);
        this.promiseHandle.then(function() {
            expect(this.callbackProcessed.calledOnce).to.be.ok();
            done();
        }, this);
        this.promiseHandle.fulfill();
    });

    it('после завершения обработки всех элеметов очереди должно произойти событие `drained`', function(done) {
        this.queue.push(this.items[0]);
        this.promiseHandle.then(function() {
            expect(this.callbackDrained.calledOnce).to.be.ok();
            done();
        }, this);
        this.promiseHandle.fulfill();
    });

    describe('Обработка очереди из 4 элементов, с лимитом порции в 2 элемента', () => {
        before(function() {
            ModelQueue.define('zorro', {
                limit: 2,
                methods: {
                    handlePortion: function(items) {
                        this.itemsProcessing = items;
                        this.nextPortion = new Vow.Promise();
                        return this.nextPortion;
                    }.bind(this)
                }
            });
        });

        beforeEach(function() {
            this.nextPortion = new Vow.Promise();

            this.zorro = ns.Model.get('zorro');

            this.zorro.push(this.items[0]);
            this.zorro.push(this.items[1]);
            this.zorro.push(this.items[2]);
            this.zorro.push(this.items[3]);
        });

        afterEach(function() {
            delete this.zorro;
            delete this.nextPortion;
        });

        it('первый пакет должен состоять из одного элемента', function() {
            expect(this.itemsProcessing).to.have.length(1);
        });

        it('второй пакет должен состоять из двух элементов', function(done) {
            this.nextPortion.then(function() {
                expect(this.itemsProcessing).to.have.length(2);
                done();
            }, this);
            this.nextPortion.fulfill();
        });

        it('третий пакет должен состоять из одного элемента', function() {
            this.nextPortion.fulfill();
            this.nextPortion.fulfill();
            expect(this.itemsProcessing).to.have.length(1);
        });
    });

    describe('Метод `pushPriority`', () => {
        beforeEach(function() {
            sinon.stub(this.queueDouble, 'doIteration', ns.nop);
        });

        afterEach(function() {
            this.queueDouble.doIteration.restore();
        });

        it('должен вставить элемент в начало очереди, если ранее не было приоритетных элементов', function() {
            this.queueDouble.push(this.items[0]);
            this.queueDouble.pushPriority(this.items[1]);
            expect(this.queueDouble.items[0].item).to.be.eql(this.items[1]);
        });

        it('должен вставить элемент после последнего приоритетного элемента', function() {
            this.queueDouble.push(this.items[0]);
            this.queueDouble.push(this.items[3]);
            this.queueDouble.pushPriority(this.items[1]);
            this.queueDouble.pushPriority(this.items[2]);
            expect(this.queueDouble.items[1].item).to.be.eql(this.items[2]);
        });
    });
});
