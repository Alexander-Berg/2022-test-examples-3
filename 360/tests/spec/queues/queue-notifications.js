require('models/queues/queue-notifications');

const guid = require('helpers/guid');

describe('Очередь нотификаций', () => {
    beforeEach(function() {
        ns.Model.get('queueNotifications');

        this.queue = ns.Model.get('queueNotifications');
        this.callbackProcessing = sinon.spy();
        this.queue.on('processing', this.callbackProcessing);
    });

    afterEach(function() {
        delete this.queue;
        delete this.callbackProcessing;
    });

    describe('Метод `add`', () => {
        beforeEach(function() {
            guid.reset();

            this.dataNotif = {
                foo: 'bar'
            };

            this.queue.add(this.dataNotif);
        });

        afterEach(function() {
            delete this.dataNotif;
        });

        it('должен создать модель нотификации', function() {
            expect(ns.Model.get('notification', {
                notification: 1
            }).getData()).to.be.eql(this.dataNotif);
        });

        it('при добавлении элемента должно дергаться событие `processing`', function() {
            expect(this.callbackProcessing.calledOnce).to.be.ok();
        });
    });

    describe('Метод `getPortion`', () => {
        beforeEach(function() {
            this.items = [{ foo: 1 }, { foo: 2 }, { foo: 3 }, { foo: 4 }, { foo: 5 }, { foo: 6 }];
            sinon.spy(this.queue, 'getPortion');
            this.portions = this.queue.getPortion.returnValues;
        });

        afterEach(function() {
            delete this.portions;
            delete this.items;
        });

        it('При добавлении одного элемента должен вернуть один элемент', function() {
            this.queue.add(this.items[0]);
            expect(this.portions[0][0].item.getData()).to.be.eql(this.items[0]);
        });

        it('При добавлении элементов меньших лимита должен вернуть последний элемент', function() {
            this.queue.add(this.items[0]);
            this.queue.add(this.items[1]);
            this.queue.add(this.items[2]);
            expect(this.portions[2][0].item.getData()).to.be.eql(this.items[2]);
        });

        describe('При добавлении элементов количеством больше лимита', () => {
            beforeEach(function() {
                this.promises = [];
                this.promises.push(this.queue.add(this.items[0]));
                this.promises.push(this.queue.add(this.items[1]));
                this.promises.push(this.queue.add(this.items[2]));
                this.promises.push(this.queue.add(this.items[3]));
                this.promises.push(this.queue.add(this.items[4]));
                this.promises.push(this.queue.add(this.items[5]));
            });

            it('Должен сначала возвращать помещающиеся в лимит элементы, а потом пустой массив', function() {
                expect(this.portions[0][0].item.getData()).to.be.eql(this.items[0]);
                expect(this.portions[1][0].item.getData()).to.be.eql(this.items[1]);
                expect(this.portions[2][0].item.getData()).to.be.eql(this.items[2]);
                expect(this.portions[3][0].item.getData()).to.be.eql(this.items[3]);
                expect(this.portions[4][0]).not.to.be.ok();
                expect(this.portions[5][0]).not.to.be.ok();
            });

            it('При освобождении места в очереди должен отдать первый свободный элемент', function(done) {
                this.queue.itemsProcessing[2].promise.then(function() {
                    expect(this.portions[6][0].item.getData()).to.be.eql(this.items[4]);
                    done();
                }, this);
                this.queue.itemsProcessing[2].promise.fulfill();
            });

            it('При освобождении места в очереди первый свободный элемент должен попасть в массив активных', function(done) {
                this.queue.itemsProcessing[2].promise.then(function() {
                    expect(this.queue.itemsProcessing[3].item.getData()).to.be.eql(this.items[4]);
                    done();
                }, this);
                this.queue.itemsProcessing[2].promise.fulfill();
            });

            it('При освобождении места в очереди должен увеличить массив активных элементов до значения предельно возможного', function(done) {
                this.queue.itemsProcessing[2].promise.then(function() {
                    expect(this.queue.itemsProcessing).to.have.length(this.queue.limit);
                    done();
                }, this);
                this.queue.itemsProcessing[2].promise.fulfill();
            });

            it('Gри добавлении элементов событие `processing` должно дернуться столько раз, сколько элементов добавили', function() {
                expect(this.callbackProcessing.callCount).to.be(4);
            });

            describe('При показе всех нотификаций', () => {
                beforeEach(function(done) {
                    Vow.all(this.promises).then(() => {
                        done();
                    });

                    this.promises.forEach((promise) => {
                        promise.fulfill();
                    });
                });

                it('массив свободных элементов должен опустеть', function() {
                    expect(this.queue.items).to.have.length(0);
                });

                it('массив активных элементов должен опустеть', function() {
                    expect(this.queue.itemsProcessing).to.have.length(0);
                });

                it('событие `processing` должно дернуться столько раз, сколько нотификаций показали', function() {
                    expect(this.callbackProcessing.callCount).to.be(this.items.length);
                });
            });
        });
    });
});
