const _ = require('lodash');

require('models/resource/resource');
require('models/user-current/user-current');
require('models/users/user-folder');
require('models/users/users-folder');

describe('Модель списка пользователей папки', () => {
    describe('для обычной папки', () => {
        beforeEach(function() {
            this.resource = ns.Model.get('resource', { id: '/files/notes' }).setData({
                id: '/files/notes',
                type: 'dir'
            });

            this.users = ns.Model.get('usersFolder', { idFolder: '/files/notes' }).setDataDefault();

            ns.Model.get('userCurrent').setData({
                name: { last: 'Alfa' },
                uid: 1,
                login: 'alfa',
                sids: ['59']
            });
        });

        afterEach(function() {
            delete this.users;
            delete this.resource;
        });

        it('должна создать список, состоящий из текущего пользователя', function() {
            expect(this.users.models).to.have.length(0);
        });

        it('должна самоуничтожиться при удалении связанного ресурса', function() {
            ns.Model.destroy(this.resource);

            expect(ns.Model.getValid('usersFolder', { idFolder: '/files/notes' })).to.not.be.ok();
        });

        it('должна быть валидна', function() {
            expect(this.users.isValid()).to.equal(true);
        });
    });

    describe('для общей папки', () => {
        beforeEach(function() {
            this.resource = ns.Model.get('resource', { id: '/files/notes' }).setData({
                id: '/files/notes',
                type: 'dir',
                meta: { group: { gid: '1' } }
            });

            this.users = ns.Model.get('usersFolder', { idFolder: '/files/notes', gidFolder: '1' });
        });

        afterEach(function() {
            delete this.users;
            delete this.resource;
        });

        it('должна быть невалидна при инстанциировании', function() {
            expect(this.users.isValid()).to.equal(false);
        });

        it('должна самоуничтожиться при удалении связанного ресурса', function() {
            ns.Model.destroy(this.resource);

            expect(ns.Model.getValid('usersFolder', { idFolder: '/files/notes', gidFolder: '1' })).to.not.be.ok();
        });

        it('должна самоуничтожиться при сбросе признака шаренности связанного ресурса', function() {
            this.resource.setData({
                id: '/files/notes',
                type: 'dir'
            });

            expect(ns.Model.getValid('usersFolder', { idFolder: '/files/notes', gidFolder: '1' })).to.not.be.ok();
        });

        describe('при вставке данных', () => {
            beforeEach(function() {
                this.users.setData({
                    users: [
                        {
                            status: 'approved',
                            name: 'Alfa',
                            userid: 'alfa'
                        },
                        {
                            status: 'proposed',
                            name: 'Bravo',
                            userid: 'bravo'
                        },
                        {
                            status: 'approved',
                            name: 'Wiskey',
                            userid: 'wiskey'
                        }
                    ]
                });
            });

            it('должна сортировать список по статусу принятия приглашения', function() {
                expect(_.first(this.users.models).get('.status')).to.equal('proposed');
            });

            it('должна проставлять каждому пользователю идентификатор ресурса', function() {
                expect(_.first(this.users.models).get('.idFolder')).to.equal('/files/notes');
            });

            it('должна создать правильный индекс элементов', function() {
                expect(this.users.get('.index')).to.eql(['bravo', 'alfa', 'wiskey']);
            });

            describe('обновление индексов', () => {
                it('при добавлении элемента', function() {
                    const user = ns.Model.get('userFolder', {
                        idFolder: this.users.params.idFolder,
                        userid: 'charley'
                    });
                    user.setData({
                        status: 'approved',
                        name: 'Charley',
                        userid: 'charley',
                        id: 'charley'
                    });
                    this.users.insertItem(user);
                    expect(this.users.get('.index')).to.eql(['charley', 'bravo', 'alfa', 'wiskey']);
                });

                it('при удалении элемента', function() {
                    this.users.removeItem('bravo');
                    expect(this.users.get('.index')).to.eql(['alfa', 'wiskey']);
                });
            });
        });
    });
});
