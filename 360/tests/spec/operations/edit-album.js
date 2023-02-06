require('models/album/album');
require('models/queues/queue-notifications');
require('models/operation/operation-edit-album');

const operation = require('helpers/operation');

describe('Операция изменения атрибутов альбома', () => {
    beforeEach(function() {
        this.notifAdding = sinon.spy();
        ns.Model.get('queueNotifications').on('added', this.notifAdding);

        this.queueAdding = sinon.spy();
        ns.Model.get('queueRequest').on('added', this.queueAdding);

        this.idAlbum = '/album/123';

        ns.Model.get('album', {
            idAlbum: this.idAlbum
        }).setData({
            id: this.idAlbum,
            title: 'Hello, world',
            cover: {
                id: '/album/123:123'
            },
            public: {
                public_key: '123',
                short_url: 'https://yadi.sk/a/123'
            }
        });
    });

    afterEach(function() {
        delete this.notifAdding;
        delete this.queueAdding;
        delete this.idAlbum;
    });

    describe('в случае обложки альбома', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    id: this.idAlbum,
                    title: 'Hello, world',
                    cover: {
                        id: '/foo/bar'
                    },
                    public: {
                        public_key: '123',
                        short_url: 'https://yadi.sk/a/123'
                    }
                }
            ]);
            this.operation = operation.initialize('editAlbum', {
                idAlbum: this.idAlbum,
                key: 'cover',
                value: '/foo/bar'
            });
        });
        afterEach(function() {
            delete this.operation;
        });

        testOperation({
            desc: 'после начала должна установить состояние изменения альбома',
            status: 'started',
            statusTestDone: 'started',
            callback: function() {
                const album = ns.Model.getValid('album', {
                    idAlbum: this.idAlbum
                });
                expect(album.get('.state.changing')).to.be(true);
            }
        });

        testOperation({
            desc: 'после окончания должна сбросить состояние изменения альбома',
            status: 'done',
            callback: function() {
                const album = ns.Model.getValid('album', {
                    idAlbum: this.idAlbum
                });
                expect(album.get('.state.changing')).to.be(false);
            }
        });

        testOperation({
            desc: 'должна изменить обложку альбома',
            status: 'done',
            callback: function() {
                const album = ns.Model.getValid('album', {
                    idAlbum: this.idAlbum
                });
                expect(album.get('.cover.id')).to.equal('/foo/bar');
            }
        });

        testOperation({
            desc: 'должна отправлять запрос на изменение атрибутов альбома',
            status: 'done',
            callback: function() {
                expect(this.queueAdding.calledOnce).to.be.ok();
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

    describe('в случае заголовка альбома', () => {
        describe('если заголовок содержит слеши', () => {
            beforeEach(function() {
                this.operation = operation.initialize('editAlbum', {
                    idAlbum: this.idAlbum,
                    key: 'title',
                    value: 'disk/foo'
                });
            });
            afterEach(function() {
                delete this.operation;
            });

            testOperation({
                desc: 'должна зафейлиться (failed) с причиной NAME_HAS_SLASH',
                status: 'failed',
                statusTestDone: 'failed',
                callback: function() {
                    expect(this.operation.get('.reason')).to.be('NAME_HAS_SLASH');
                }
            });

            testOperation({
                desc: 'не должна изменить тайтл альбома',
                status: 'failed',
                statusTestDone: 'failed',
                callback: function() {
                    const album = ns.Model.getValid('album', {
                        idAlbum: this.idAlbum
                    });
                    expect(album.get('.title')).to.be('Hello, world');
                }
            });

            testOperation({
                desc: 'не должна отправлять запрос на изменение атрибутов альбома',
                status: 'failed',
                statusTestDone: 'failed',
                callback: function() {
                    expect(this.queueAdding.calledOnce).not.to.be.ok();
                }
            });
        });

        describe('если заголовок не изменился', () => {
            beforeEach(function() {
                this.operation = operation.initialize('editAlbum', {
                    idAlbum: this.idAlbum,
                    key: 'title',
                    value: 'Hello, world'
                });
            });
            afterEach(function() {
                delete this.operation;
            });

            testOperation({
                desc: 'после начала должна установить состояние изменения альбома',
                status: 'started',
                statusTestDone: 'started',
                callback: function() {
                    expect(ns.Model.getValid('album', { idAlbum: this.idAlbum }).get('.state.changing')).to.be(true);
                }
            });

            testOperation({
                desc: 'должна отмениться (canceled) с причиной nochanges',
                status: 'canceled',
                statusTestDone: 'canceled',
                callback: function() {
                    expect(this.operation.get('.reason')).to.be('nochanges');
                }
            });

            testOperation({
                desc: 'должна сбросить состояние изменения альбома',
                status: 'canceled',
                statusTestDone: 'canceled',
                callback: function() {
                    const album = ns.Model.getValid('album', {
                        idAlbum: this.idAlbum
                    });
                    expect(album.get('.state.changing')).to.be(false);
                }
            });

            testOperation({
                desc: 'не должна отправлять запрос на изменение атрибутов альбома',
                status: 'canceled',
                statusTestDone: 'canceled',
                callback: function() {
                    expect(this.queueAdding.calledOnce).to.be(false);
                }
            });
        });

        describe('если заголовок валидный', () => {
            beforeEach(function() {
                addResponseModel([
                    {
                        id: this.idAlbum,
                        title: 'disk-foo',
                        cover: {
                            id: '234'
                        },
                        public: {
                            public_key: '123',
                            short_url: 'https://yadi.sk/a/123'
                        }
                    }
                ]);
                this.operation = operation.initialize('editAlbum', {
                    idAlbum: this.idAlbum,
                    key: 'title',
                    value: 'disk-foo'
                });
            });
            afterEach(function() {
                delete this.operation;
            });

            testOperation({
                desc: 'после начала должна установить состояние изменения альбома',
                status: 'started',
                callback: function() {
                    const album = ns.Model.getValid('album', {
                        idAlbum: this.idAlbum
                    });
                    expect(album.get('.state.changing')).to.be(true);
                }
            });

            testOperation({
                desc: 'должна отправлять запрос на изменение атрибутов альбома',
                status: 'done',
                callback: function() {
                    expect(this.queueAdding.calledOnce).to.be(true);
                }
            });

            testOperation({
                desc: 'должна изменить заголовок альбома',
                status: 'done',
                callback: function() {
                    const album = ns.Model.getValid('album', {
                        idAlbum: this.idAlbum
                    });
                    expect(album.get('.title')).to.be('disk-foo');
                }
            });

            testOperation({
                desc: 'после окончания должна сбросить состояние изменения альбома',
                status: 'done',
                callback: function() {
                    const album = ns.Model.getValid('album', {
                        idAlbum: this.idAlbum
                    });
                    expect(album.get('.state.changing')).to.be(false);
                }
            });

            testOperation({
                desc: 'должна создать нотификацию',
                status: 'done',
                callback: function() {
                    expect(this.notifAdding.calledOnce).to.be(true);
                }
            });
        });
    });
});
