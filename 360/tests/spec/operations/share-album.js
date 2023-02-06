require('models/album/album');
require('models/operation/operation-share-album');
require('models/queues/queue-notifications');

const helperOperation = require('helpers/operation');

describe('Сохранения редактирования фотографии', () => {
    beforeEach(function() {
        this.historyStatus = [];
        this.notifAdding = sinon.spy();

        this.notifications = ns.Model.get('queueNotifications');
        this.notifications.on('added', this.notifAdding);
    });

    afterEach(function() {
        delete this.historyStatus;
        delete this.notifications;
        delete this.notifAdding;
    });

    describe('будучи успешной', () => {
        beforeEach(function() {
            addResponseModel([
                { oid: 1 },
                { status: 'WAITING' },
                { status: 'EXECUTING' },
                { status: 'DONE' }
            ]);

            this.idAlbum = '123';

            ns.Model.get('album', {
                id: this.idAlbum
            }).setData({
                id: '345'
            });

            this.operation = helperOperation.initialize('shareAlbum', {
                idAlbum: this.idAlbum,
                provider: 'vkontakte'
            });

            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });
        });

        afterEach(function() {
            delete this.idAlbum;
            delete this.operation;
        });

        testOperation({
            desc: 'должна пройти определенные статусы',
            status: 'done',
            callback: function() {
                expect(this.historyStatus).to.be.eql([
                    'created',
                    'progressing',
                    'completed',
                    'done'
                ]);
            }
        });

        testOperation({
            desc: 'должна кинуть нотифайку',
            status: 'done',
            callback: function() {
                expect(this.notifAdding.calledOnce).to.be.ok();
            }
        });
    });

    describe('будучи неуспешной', () => {
        beforeEach(function() {
            addResponseModel([
                { oid: 1 },
                { status: 'WAITING' },
                { status: 'EXECUTING' },
                {
                    status: 'FAILED',
                    type: 'share',
                    album: {
                        id: '345'
                    }
                }
            ]);

            this.idAlbum = '123';

            ns.Model.get('album', {
                id: this.idAlbum
            }).setData({
                id: '345'
            });

            this.operation = helperOperation.initialize('shareAlbum', {
                idAlbum: this.idAlbum,
                provider: 'vkontakte'
            });

            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });
        });

        afterEach(function() {
            delete this.idAlbum;
            delete this.operation;
        });

        testOperation({
            desc: 'должна пройти определенные статусы',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(this.historyStatus).to.be.eql([
                    'created',
                    'progressing',
                    'failed'
                ]);
            }
        });

        testOperation({
            desc: 'должна кинуть нотифайку',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(this.notifAdding.calledOnce).to.be.ok();
            }
        });
    });
});
