import 'models/queues/queue-notifications';
import 'models/operation/operation-delete';

import operation from 'helpers/operation';
import helperResource from 'helpers/resource';

describe('Удаления', () => {
    beforeEach(function() {
        this.notifAdding = sinon.spy();

        this.notifications = ns.Model.get('queueNotifications');
        this.notifications.on('added', this.notifAdding);
    });

    afterEach(function() {
        delete this.notifAdding;
        delete this.notifications;
    });

    it('после создания должна закэшировать srcType', () => {
        const dataSrc = {
            id: '/files/test.txt',
            name: 'test.txt',
            type: 'file'
        };
        ns.Model.get('resource', {
            id: dataSrc.id
        }).setData(dataSrc);
        const existingOp = operation.create('delete', {
            idSrc: dataSrc.id
        });
        expect(existingOp.get('.srcType')).to.be.eql(dataSrc.type);
    });

    describe('будучи успешной', () => {
        beforeEach(function() {
            this.dataSrc = {
                id: '/disk/path/to/test.jpg',
                name: 'test.jpg',
                type: 'dir'
            };

            this.dataDst = {
                id: '/trash/test.jpg',
                name: 'test.jpg',
                type: 'dir',
                originalId: '/disk/path/to/test.jpg'
            };

            addResponseModel([{
                oid: 1,
                type: 'trash'
            }, {
                status: 'WAITING',
                type: 'trash'
            }, {
                status: 'EXECUTING',
                type: 'trash'
            }, {
                status: 'DONE',
                resource: {
                    ctime: 1368183937,
                    mtime: 1368183937,
                    path: this.dataDst.id,
                    type: this.dataDst.type,
                    id: this.dataDst.id + '/',
                    name: this.dataDst.name,
                    originalId: this.dataDst.originalId
                },
                type: 'trash'
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.operation = operation.initialize('delete', {
                idSrc: this.dataSrc.id
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

        describe('после старта', () => {
            testOperation({
                desc: 'должна спрятать изначальный ресурс',
                status: 'started',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.state.hidden')
                    ).to.be(true);
                }
            });

            testOperation({
                desc: 'должна создать частичный ресурс в корзине',
                status: 'started',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataDst.id }).get('.state.part')
                    ).to.be(true);
                }
            });

            testOperation({
                desc: 'должна показать частичный ресурс в корзине',
                status: 'started',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataDst.id }).get('.state.hidden')
                    ).not.to.be(true);
                }
            });
        });

        describe('после завершения', () => {
            testOperation({
                desc: 'должна пройти определенные статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql([
                        'created',
                        'started',
                        'progressing',
                        'done'
                    ]);
                }
            });

            testOperation({
                desc: 'должна удалить исходный ресурс из кешей',
                status: 'done',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', this.resource.key)
                    ).not.to.be.ok();
                }
            });

            testOperation({
                desc: 'должна создать настоящий ресурс в корзине',
                status: 'done',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataDst.id }).get('.state.part')
                    ).not.to.be(true);
                }
            });

            testOperation({
                desc: 'должна создать нотификацию',
                status: 'done',
                callback: function() {
                    expect(
                        this.notifAdding.calledOnce
                    ).to.be.ok();
                }
            });
        });
    });

    describe('ресурса в корзину, где существует ресурс с тем же именем, который есть в кешах', () => {
        beforeEach(function() {
            this.dataSrc = {
                id: '/disk/path/to/test.jpg',
                name: 'test.jpg',
                type: 'dir'
            };

            this.dataDst = {
                id: '/trash/test.jpg_1',
                name: 'test.jpg',
                type: 'dir',
                originalId: '/disk/path/to/test.jpg'
            };

            this.resourceExisting = {
                id: '/trash/test.jpg',
                name: 'test.jpg',
                type: 'dir'
            };

            ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            ns.Model.get('resource', {
                id: this.resourceExisting.id
            }).setData(this.resourceExisting);

            addResponseModel([{
                oid: 1,
                type: 'trash'
            }, {
                status: 'WAITING',
                type: 'trash'
            }, {
                status: 'EXECUTING',
                type: 'trash'
            }, {
                status: 'DONE',
                resource: {
                    ctime: 1368183937,
                    mtime: 1368183937,
                    path: this.dataDst.id,
                    type: this.dataDst.type,
                    id: this.dataDst.id + '/',
                    name: this.dataDst.name,
                    originalId: this.dataDst.originalId
                },
                type: 'trash'
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.operation = operation.initialize('operationDelete', {
                idSrc: this.dataSrc.id
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
            delete this.resourceExisting;
            delete this.historyStatus;
        });

        describe('после старта', () => {
            testOperation({
                desc: 'должна создать ресурс с верным `id` в корзине',
                status: 'started',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataDst.id }).get('.id')
                    ).to.be(this.dataDst.id);
                }
            });

            testOperation({
                desc: 'должна создать ресурс с верным именем в корзине',
                status: 'started',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataDst.id }).get('.name')
                    ).to.be(this.dataDst.name);
                }
            });
        });

        describe('после завершения', () => {
            testOperation({
                desc: 'должна пройти определенные статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql([
                        'created',
                        'started',
                        'progressing',
                        'done'
                    ]);
                }
            });
        });
    });

    describe('папки', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'trash'
                },
                {
                    status: 'DONE',
                    resource: { ok: true }
                }
            ]);

            this.subresource = ns.Model.get('resource', { id: '/files/folder/test.txt' }).setData({
                id: '/files/folder/test.txt',
                name: 'test.txt'
            });

            this.subresource2 = ns.Model.get('resource', { id: '/files/folder/test' }).setData({
                id: '/files/folder/test',
                name: 'test'
            });

            this.subresources2 = ns.Model.get('resources', {
                idContext: '/files/folder/test',
                sort: 'asc',
                order: 'name'
            });

            this.subresource3 = ns.Model.get('resource', { id: '/files/folder/test/sub.jpg' }).setData({
                id: '/files/folder/test/sub.jpg',
                name: 'sub.jpg'
            });

            this.resource = ns.Model.get('resource', { id: '/files/folder' }).setData({
                id: '/files/folder',
                name: 'folder'
            });

            this.resources = ns.Model.get('resources', {
                idContext: '/files/folder',
                sort: 'asc',
                order: 'name'
            });

            this.resources.insert([this.subresource, this.subresource2]);

            this.subresources2.insert(this.subresource3);

            this.operation = operation.initialize('delete', {
                idSrc: '/files/folder'
            });
        });

        afterEach(function() {
            delete this.operation;
            delete this.resource;
            delete this.resources;
            delete this.subresource;
            delete this.subresource2;
            delete this.subresource3;
            delete this.subresources2;
        });

        testOperation({
            desc: 'должна обеспечить удаление сопряженной коллекции из кешей',
            status: 'done',
            callback: function() {
                expect(ns.Model.getValid('resources', this.resources.params)).to.not.be.ok();
            }
        });

        testOperation({
            desc: 'должна обеспечить удаление дочерней коллекции ресурсов из кешей',
            status: 'done',
            callback: function() {
                expect(ns.Model.getValid('resource', this.subresources2.params)).to.not.be.ok();
            }
        });

        testOperation({
            desc: 'должна обеспечить удаление дочерних ресурсов из кешей',
            status: 'done',
            callback: function() {
                expect(ns.Model.getValid('resource', this.subresource.params)).to.not.be.ok();
                expect(ns.Model.getValid('resource', this.subresource2.params)).to.not.be.ok();
            }
        });

        testOperation({
            desc: 'должна обеспечить удаление вложенных ресурсов из кешей',
            status: 'done',
            callback: function() {
                expect(ns.Model.getValid('resource', this.subresource3.params)).to.not.be.ok();
            }
        });
    });

    describe('папки Фотокамера', () => {
        beforeEach(function() {
            this.resource = ns.Model.get('resource', { id: '/disk/Фотокамера' }).setData({
                id: '/disk/Фотокамера',
                name: 'folder'
            });

            this.operation = operation.initialize('delete', {
                idSrc: '/disk/Фотокамера'
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });

            sinon.stub(helperResource, 'isCamera', no.true);
            sinon.stub(this.operation, 'confirm', () => {
                return Vow.reject();
            });
        });

        afterEach(function() {
            helperResource.isCamera.restore();
            this.operation.confirm.restore();
            delete this.operation;
            delete this.resource;
            delete this.historyStatus;
        });

        testOperation({
            desc: 'должна спросить подтверждения у пользователя',
            status: 'rejected',
            statusTestDone: 'rejected',
            callback: function() {
                expect(this.historyStatus).to.be.eql([
                    'created',
                    'rejected'
                ]);
            }
        });
    });

    describe('общей папки с уже загруженными родителями', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'trash'
                },
                {
                    status: 'DONE'
                }
            ]);

            this.resourceRoot = ns.Model.get('resource', { id: '/root' }).setData({
                id: '/root',
                name: 'root',
                type: 'dir',
                meta: { with_shared: 1 }
            });

            this.resourceParent = ns.Model.get('resource', { id: '/root/parent' }).setData({
                id: '/root/parent',
                name: 'parent',
                type: 'dir',
                meta: { with_shared: 1 }
            });

            this.resourceShared = ns.Model.get('resource', { id: '/root/parent/shared' }).setData({
                id: '/root/parent/shared',
                name: 'shared',
                type: 'dir',
                meta: {
                    group: {
                        is_root: 1,
                        gid: 555
                    }
                }
            });

            this.operation = operation.initialize('delete', {
                idSrc: this.resourceShared.get('.id')
            });

            sinon.stub(this.resourceRoot, 'fetch');
            sinon.stub(this.resourceParent, 'fetch');
        });

        afterEach(function() {
            delete this.operation;
            delete this.resourceRoot;
            delete this.resourceParent;
            delete this.resourceShared;
        });

        testOperation({
            desc: 'должна перезапросить непосредственного родителя',
            status: 'done',
            callback: function() {
                expect(this.resourceParent.fetch.calledOnce).to.be(true);
            }
        });

        testOperation({
            desc: 'не должна перезапрашивать корневой ресурс',
            status: 'done',
            callback: function() {
                expect(this.resourceRoot.fetch.called).to.be(false);
            }
        });
    });

    describe('папки, содержащей ОП, с уже загруженными родителями', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'trash'
                },
                {
                    status: 'DONE'
                }
            ]);

            this.resourceParent = ns.Model.get('resource', { id: '/root/parent' }).setData({
                id: '/root/parent',
                name: 'root',
                type: 'dir',
                meta: { with_shared: 1 }
            });

            this.resourceWithShared = ns.Model.get('resource', { id: '/root/parent/with shared' }).setData({
                id: '/root/parent/with shared',
                name: 'With Shared',
                type: 'dir',
                meta: { with_shared: 1 }
            });

            this.operation = operation.initialize('delete', {
                idSrc: this.resourceWithShared.get('.id')
            });

            sinon.stub(this.resourceParent, 'fetch');
        });

        afterEach(function() {
            delete this.operation;
            delete this.resourceParent;
            delete this.resourceWithShared;
        });

        testOperation({
            desc: 'должна перезапросить непосредственного родителя',
            status: 'done',
            callback: function() {
                expect(this.resourceParent.fetch.calledOnce).to.be(true);
            }
        });
    });
});
