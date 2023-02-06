require('models/album/album');
require('models/queues/queue-notifications');
require('models/operation/operation-remove-album');

const operation = require('helpers/operation');

describe('Операция удаления альбома', () => {
    beforeEach(function() {
        this.historyStatus = [];

        this.notifAdding = sinon.spy();

        this.notifications = ns.Model.get('queueNotifications');
        this.notifications.on('added', this.notifAdding);

        this.idAlbum = '/album/123';
    });
    afterEach(function() {
        this.notifications.destroy();
    });

    describe('в случае успешного удаления', () => {
        beforeEach(function() {
            ns.Model.get('album', {
                idAlbum: this.idAlbum
            }).setData({
                id: 'foo',
                cover: {
                    id: '123',
                    object: {}
                },
                public: {
                    public_key: '123',
                    short_url: 'https://yadi.sk/a/123'
                }
            });

            addResponseModel({});

            this.operation = operation.initialize('removeAlbum', {
                idAlbum: this.idAlbum
            });
        });

        testOperation({
            desc: 'должен перейти в статус \'done\' и добавить нотификацию',
            status: 'done',
            callback: function() {
                expect(this.notifAdding.calledOnce).to.be.ok();
            }
        });

        testOperation({
            desc: 'должна уничтожить модель',
            status: 'done',
            callback: function() {
                expect(ns.Model.getValid('album', { id: 'foo' })).not.to.be.ok();
            }
        });
    });

    describe('в случае неуспешного удаления', () => {
        beforeEach(function() {
            ns.Model.get('album', {
                idAlbum: this.idAlbum
            }).setData({
                id: 'foo',
                cover: {
                    id: '234',
                    object: {}
                },
                public: {
                    public_key: '123',
                    short_url: 'https://yadi.sk/a/123'
                }
            });

            addResponseModel({
                error: {
                    id: 'HTTP_404',
                    message: 'not found'
                }
            });

            this.operation = operation.initialize('removeAlbum', {
                idAlbum: this.idAlbum
            });
        });

        testOperation({
            desc: 'должен перейти в статус \'failed\' и добавить нотификацию',
            status: 'failed',
            statusTestDone: 'failed',
            callback: function() {
                expect(this.notifAdding.calledOnce).to.be.ok();
            }

        });
    });
});
