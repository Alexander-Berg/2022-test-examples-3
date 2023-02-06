require('models/queues/queue-notifications');
require('models/operation/operation-publish');

const operation = require('helpers/operation');

describe('Publish', () => {
    beforeEach(function() {
        this.notifAdding = sinon.spy();

        this.notifications = ns.Model.get('queueNotifications');
        this.notifications.on('added', this.notifAdding);
    });

    afterEach(function() {
        delete this.notifAdding;
        delete this.notifications;
    });

    describe('Publish: do publish', () => {
        beforeEach(function() {
            this.dataSrc = {
                id: '/disk/path/to/test.jpg',
                name: 'test.jpg',
                type: 'file'
            };

            addResponseModel([
                {
                    url: 'https://disk-test.disk.yandex.ru/public/?hash=YNQWs5xRRcETiteBGgrlBbJ3sDMYnrAj9btz21p0Mc0%3D',
                    short_url: 'http://front.tst.clk.yandex.net/Rb_p',
                    hash: 'YNQWs5xRRcETiteBGgrlBbJ3sDMYnrAj9btz21p0Mc0=',
                    short_url_named: 'http://front.tst.clk.yandex.net/Rb_p?IMG_2050.JPG'
                }
            ]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.operation = operation.initialize('publish', {
                idSrc: this.dataSrc.id,
                reverse: false
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });
        });

        afterEach(function() {
            delete this.operation;
            delete this.dataSrc;
            delete this.dataDst;
            delete this.historyStatus;
            delete this.resource;
        });

        describe('after start', () => {
            testOperation({
                desc: 'should change .state.publishing in resource to \'true\'',
                status: 'started',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.state.publishing')).to.be(true);
                }
            });
        });

        describe('after done', () => {
            testOperation({
                desc: 'should pass through all statuses',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql(['created', 'started', 'done']);
                }
            });

            testOperation({
                desc: 'should change .state.publishing in resource to \'false\'',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.state.publishing')).to.be(false);
                }
            });

            testOperation({
                desc: 'should add meta.public information to resource',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.meta.public')).to.be(1);
                }
            });

            testOperation({
                desc: 'should add meta.short_url information to resource',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.meta')).have.property('short_url');
                }
            });

            testOperation({
                desc: 'should create notification',
                status: 'done',
                callback: function() {
                    expect(this.notifAdding.calledOnce).to.be.ok();
                }
            });
        });
    });
    describe('Publish: do private', () => {
        beforeEach(function() {
            this.dataSrc = {
                id: '/disk/path/to/test.jpg',
                name: 'test.jpg',
                type: 'file'
            };

            addResponseModel([
                {}
            ]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.operation = operation.initialize('publish', {
                idSrc: this.dataSrc.id,
                reverse: true
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });
        });

        afterEach(function() {
            delete this.operation;
            delete this.dataSrc;
            delete this.dataDst;
            delete this.historyStatus;
            delete this.resource;
        });

        describe('after start', () => {
            testOperation({
                desc: 'should change .state.publishing in resource to \'true\'',
                status: 'started',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.state.publishing')).to.be(true);
                }
            });
        });

        describe('after done', () => {
            testOperation({
                desc: 'should pass through all statuses',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql(['created', 'started', 'done']);
                }
            });

            testOperation({
                desc: 'should change .state.publishing in resource to \'false\'',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.state.publishing')).to.be(false);
                }
            });

            testOperation({
                desc: 'should remove meta.public information to resource',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.meta.public')).to.be(0);
                }
            });

            testOperation({
                desc: 'should remove meta.short_url information to resource',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.meta.short_url')).not.be.ok();
                }
            });

            testOperation({
                desc: 'should create notification',
                status: 'done',
                callback: function() {
                    expect(this.notifAdding.calledOnce).to.be.ok();
                }
            });
        });
    });
});
