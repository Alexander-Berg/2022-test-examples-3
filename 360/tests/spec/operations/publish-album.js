require('models/album/album');
require('models/queues/queue-notifications');
require('models/operation/operation-publish-album');

const operation = require('helpers/operation');

describe('Операция публикации альбома', () => {
    beforeEach(function() {
        this.notifAdding = sinon.spy();

        this.notifications = ns.Model.get('queueNotifications');
        this.notifications.on('added', this.notifAdding);

        this.idAlbum = '/album/abc';
    });

    afterEach(function() {
        delete this.notifAdding;
        delete this.notifications;
        delete this.idAlbum;
    });

    describe('Публикация', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    id: 'foo',
                    cover: {
                        id: 123,
                        object: {}
                    },
                    is_public: true,
                    public: {
                        public_key: 'abc',
                        short_url: 'http://yadi.sk/abc'
                    }
                }
            ]);

            this.album = ns.Model.get('album', {
                idAlbum: this.idAlbum
            }).setData({
                id: 'foo',
                cover: {
                    id: 123,
                    object: {}
                },
                is_public: false,
                public: {
                    public_key: 'abc',
                    short_url: 'http://yadi.sk/abc'
                }
            });

            this.operation = operation.initialize('publishAlbum', {
                idAlbum: this.idAlbum,
                reverse: false
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });
        });

        afterEach(function() {
            delete this.idAlbum;
            delete this.album;
            delete this.operation;
            delete this.historyStatus;
        });

        describe('после начала операции', () => {
            testOperation({
                desc: 'должно измениться поле .state.publishing альбома на \'true\'',
                status: 'started',
                callback: function() {
                    expect(ns.Model.getValid('album', { idAlbum: this.idAlbum }).get('.state.publishing')).to.be(true);
                }
            });
        });

        describe('после окончания операции', () => {
            testOperation({
                desc: 'должна пройти определенные статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql(['created', 'started', 'done']);
                }
            });

            testOperation({
                desc: 'должно измениться поле .state.publishing альбома на \'false\'',
                status: 'done',
                callback: function() {
                    expect(
                        ns.Model.getValid('album', { idAlbum: this.idAlbum })
                            .get('.state.publishing')
                    ).not.to.be.ok();
                }
            });

            testOperation({
                desc: 'должен создать нотификацию',
                status: 'done',
                callback: function() {
                    expect(this.notifAdding.calledOnce).to.be.ok();
                }
            });
        });
    });
    describe('Распубликация', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    id: 'foo',
                    cover: {
                        id: 123,
                        object: {}
                    },
                    is_public: false,
                    public: {
                        public_key: 'abc',
                        short_url: 'http://yadi.sk/abc'
                    }
                }
            ]);

            this.album = ns.Model.get('album', {
                idAlbum: this.idAlbum
            }).setData({
                id: 'foo',
                cover: {
                    id: 123,
                    object: {}
                },
                is_public: true,
                public: {
                    public_key: 'abc',
                    short_url: 'http://yadi.sk/abc'
                }
            });

            this.operation = operation.initialize('publishAlbum', {
                idAlbum: this.idAlbum,
                reverse: true
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });
        });

        afterEach(function() {
            delete this.idAlbum;
            delete this.album;
            delete this.operation;
            delete this.historyStatus;
        });

        describe('после начала операции', () => {
            testOperation({
                desc: 'должно измениться поле .state.publishing альбома на \'true\'',
                status: 'started',
                callback: function() {
                    expect(ns.Model.getValid('album', { idAlbum: this.idAlbum }).get('.state.publishing')).to.be(true);
                }
            });
        });

        describe('после окончания операции', () => {
            testOperation({
                desc: 'должна пройти определенные статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql(['created', 'started', 'done']);
                }
            });

            testOperation({
                desc: 'должно измениться поле .state.publishing альбома на \'false\'',
                status: 'done',
                callback: function() {
                    expect(
                        ns.Model.getValid('album', { idAlbum: this.idAlbum })
                            .get('.state.publishing')
                    ).not.to.be.ok();
                }
            });

            testOperation({
                desc: 'должен создать нотификацию',
                status: 'done',
                callback: function() {
                    expect(this.notifAdding.calledOnce).to.be.ok();
                }
            });
        });
    });
});
