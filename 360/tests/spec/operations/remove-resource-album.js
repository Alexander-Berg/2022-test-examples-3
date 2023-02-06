require('models/resource/resource');
require('models/queues/queue-notifications');
require('models/operation/operation-remove-resource-album');

const operation = require('helpers/operation');

describe('Операция удаление ресурса из альбома', () => {
    beforeEach(function() {
        this.notifAdding = sinon.spy();

        this.notifications = ns.Model.get('queueNotifications');
        this.notifications.on('added', this.notifAdding);

        this.idResource = '/album/123:456';

        ns.Model.get('resource', {
            id: this.idResource
        }).setData({
            name: 'hello, world',
            cover: {
                id: '123',
                object: {}
            }
        });

        this.operation = operation.initialize('removeResourceAlbum', {
            idSrc: this.idResource
        });

        this.historyStatus = [];
        this.operation.on('ns-model-changed.status', () => {
            this.historyStatus.push(this.operation.get('.status'));
        });
    });

    afterEach(function() {
        delete this.notifAdding;
        delete this.notifications;
        delete this.idAlbum;
        delete this.idResource;
        delete this.operation;
        delete this.historyStatus;
    });

    describe('после начала операции', () => {
        testOperation({
            desc: 'должна спрятать ресурс в листинге',
            status: 'started',
            statusTestDone: 'started',
            callback: function() {
                expect(ns.Model.getValid('resource', { id: this.idResource }).get('.state.hidden')).to.be(true);
            }
        });
    });

    describe('после успешного окончания операции', () => {
        beforeEach(() => {
            addResponseModel([
                {}
            ]);
        });

        testOperation({
            desc: 'должна пройти определенные статусы',
            status: 'done',
            statusTestDone: 'done',
            callback: function() {
                expect(this.historyStatus).to.be.eql(['created', 'started', 'done']);
            }
        });

        testOperation({
            desc: 'должна удалить модель ресурса',
            status: 'done',
            statusTestDone: 'done',
            callback: function() {
                expect(
                    ns.Model.getValid('resource', { id: this.idResource })
                ).not.to.be.ok();
            }
        });

        testOperation({
            desc: 'должна создать нотификацию',
            status: 'done',
            statusTestDone: 'done',
            callback: function() {
                expect(this.notifAdding.calledOnce).to.be.ok();
            }
        });
    });

    describe('после неуспешного окончания операции', () => {
        beforeEach(() => {
            addResponseModel([
                {
                    error: 'HTTP_500'
                }
            ]);
        });

        testOperation({
            desc: 'должна пройти определенные статусы',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(this.historyStatus).to.be.eql(['created', 'started', 'failed']);
            }
        });

        testOperation({
            desc: 'должна показать ресурс в листинге',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(
                    ns.Model.getValid('resource', { id: this.idResource }).get('.state.hidden')
                ).not.to.be.ok();
            }
        });

        testOperation({
            desc: 'должна создать нотификацию',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(this.notifAdding.calledOnce).to.be.ok();
            }
        });
    });
});
