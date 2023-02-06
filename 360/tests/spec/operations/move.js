import 'models/resource/resource';
import 'models/queues/queue-notifications';
//import 'models/queues/queue-confirmations';
import 'models/operation/operation-move';

import operation from 'helpers/operation';
import helperResource from 'helpers/resource';

describe('Операция move', () => {
    beforeEach(function() {
        this.notifAdding = sinon.spy();

        this.notifications = ns.Model.get('queueNotifications').setDataDefault();
        this.notifications.on('added', this.notifAdding);

        //ns.Model.get('queueConfirmations').setDataDefault();

        this.dataSrc = {
            id: '/disk/path/to/test.jpg',
            name: 'test.jpg',
            type: 'file'
        };

        this.dataDst = {
            id: '/disk/path/test.jpg',
            name: 'test.jpg',
            type: 'file'
        };

        this.dataFolder = {
            id: '/disk/path',
            name: 'path',
            type: 'dir'
        };

        this.dataExists = {
            id: '/disk/path/test.jpg',
            name: 'test.jpg',
            type: 'file'
        };
    });

    afterEach(function() {
        delete this.notifAdding;
        delete this.notifications;

        delete this.dataSrc;
        delete this.dataDst;
        delete this.dataFolder;
        delete this.dataExists;
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
        const existingOp = operation.create('move', {
            idSrc: dataSrc.id
        });
        expect(existingOp.get('.srcType')).to.be.eql(dataSrc.type);
    });

    describe('будучи успешной', () => {
        beforeEach(function() {
            addResponseModel([{
                oid: 1,
                type: 'move'
            }, {
                status: 'WAITING',
                type: 'move'
            }, {
                status: 'EXECUTING',
                type: 'move'
            }, {
                status: 'DONE',
                params: {
                    source: '0001:' + this.dataSrc.id,
                    target: '0001:' + this.dataDst.id
                },
                resource: {
                    ctime: 1368183937,
                    mtime: 1368183937,
                    meta: {},
                    path: this.dataDst.id,
                    type: this.dataDst.type,
                    id: this.dataDst.id,
                    name: this.dataDst.name
                }
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.folder = ns.Model.get('resource', {
                id: this.dataFolder.id
            }).setData(this.dataFolder);
        });

        afterEach(function() {
            delete this.resource;
            delete this.folder;
        });

        describe('в папку которую предстоит выбрать', () => {
            beforeEach(function() {
                this.operation = operation.initialize('move', {
                    idSrc: this.dataSrc.id
                });

                this.historyStatus = [];
                this.operation.on('ns-model-changed.status', () => {
                    this.historyStatus.push(this.operation.get('.status'));
                });

                this.confirmAdding = sinon.spy((name, item) => {
                    item.promise.fulfill(this.folder.get('.id'));
                });

                //ns.Model.getValid('queueConfirmations').on('added', this.confirmAdding);
            });

            afterEach(function() {
                delete this.operation;
                delete this.historyStatus;
                delete this.confirmAdding;
            });

            testOperation({
                desc: 'после подтверждения должна запросить папку назначения один раз',
                status: 'approved',
                callback: function() {
                    expect(this.confirmAdding.calledOnce).to.be(true);
                }
            });

            testOperation({
                desc: 'после выбора папки должна запомнить ее',
                status: 'determined',
                callback: function() {
                    expect(
                        this.operation.get('.idFolderDst')
                    ).to.be(this.dataFolder.id);
                }
            });

            testOperation({
                desc: 'после завершения должна пройти определенные статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql([
                        'created',
                        'approved',
                        'determined',
                        'started',
                        'moving',
                        'done'
                    ]);
                }
            });
        });

        describe('в уже выбранную папку', () => {
            beforeEach(function() {
                this.operation = operation.initialize('move', {
                    idSrc: this.dataSrc.id,
                    idFolderDst: this.dataFolder.id
                });

                this.historyStatus = [];
                this.operation.on('ns-model-changed.status', () => {
                    this.historyStatus.push(this.operation.get('.status'));
                });
            });

            afterEach(function() {
                delete this.operation;
                delete this.historyStatus;
            });

            describe('после старта', () => {
                testOperation({
                    desc: 'должна создать частичный ресурс в целевой папке',
                    status: 'determined',
                    callback: function() {
                        expect(
                            ns.Model.getValid('resource', { id: this.dataDst.id }).get('.state.part')
                        ).to.be(true);
                    }
                });

                testOperation({
                    desc: 'должна создать невидимый ресурс в целевой папке',
                    status: 'determined',
                    callback: function() {
                        expect(
                            ns.Model.getValid('resource', { id: this.dataDst.id }).get('.state.hidden')
                        ).to.be.ok();
                    }
                });

                testOperation({
                    desc: 'должна создать ресурс в целевой папке с тем же именем, что и исходый',
                    status: 'determined',
                    callback: function() {
                        expect(
                            ns.Model.getValid('resource', { id: this.dataDst.id }).get('.name')
                        ).to.be.eql(this.dataSrc.name);
                    }
                });

                testOperation({
                    desc: 'должна создать ресурс в целевой папке с тем же типом, что и исходый',
                    status: 'determined',
                    callback: function() {
                        expect(
                            ns.Model.getValid('resource', { id: this.dataDst.id }).get('.type')
                        ).to.be.eql(this.dataSrc.type);
                    }
                });
            });

            describe('после завершения', () => {
                testOperation({
                    desc: 'должна создать настоящий ресурс в целевой папке',
                    status: 'done',
                    callback: function() {
                        expect(
                            ns.Model.getValid('resource', { id: this.dataDst.id }).get('.state.part')
                        ).not.to.be.ok();
                    }
                });

                testOperation({
                    desc: 'должна пройти определенные статусы',
                    status: 'done',
                    callback: function() {
                        expect(this.historyStatus).to.be.eql([
                            'created',
                            'approved',
                            'determined',
                            'started',
                            'moving',
                            'done'
                        ]);
                    }
                });

                testOperation({
                    desc: 'должна бросить нотификацию',
                    status: 'done',
                    callback: function() {
                        expect(
                            this.notifAdding.calledOnce
                        ).to.be.ok();
                    }
                });
            });
        });
    });

    describe('в выбранную папку, где уже присутсвует загруженный файл с тем же именем', () => {
        beforeEach(function() {
            addResponseModel([{
                oid: 1,
                type: 'move'
            }, {
                status: 'WAITING',
                type: 'move'
            }, {
                status: 'EXECUTING',
                type: 'move'
            }, {
                status: 'DONE',
                params: {
                    source: '0001:/disk/path/to/test.jpg',
                    target: '0001:/disk/path/test.jpg'
                },
                resource: {
                    ctime: 1368183937,
                    mtime: 1368183937,
                    meta: {},
                    path: this.dataDst.id,
                    type: this.dataDst.type,
                    id: this.dataDst.id,
                    name: this.dataDst.name
                }
            }, {
                ctime: 1368183937,
                mtime: 1368183937,
                meta: {},
                path: this.dataDst.id,
                type: this.dataDst.type,
                id: this.dataDst.id,
                name: this.dataDst.name
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.folder = ns.Model.get('resource', {
                id: this.dataFolder.id
            }).setData(this.dataFolder);

            this.resourceExists = ns.Model.get('resource', {
                id: this.dataExists.id
            }).setData(this.dataExists);

            this.resourceExistsData = this.resourceExists.getData();

            this.operation = operation.initialize('move', {
                idSrc: this.dataSrc.id,
                idFolderDst: this.dataFolder.id
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });

            this.confirmAdding = sinon.spy((name, item) => {
                item.promise.fulfill(this.folder);
            });

            //ns.Model.getValid('queueConfirmations').on('added', this.confirmAdding);
        });

        afterEach(function() {
            delete this.resourceExists;
            delete this.resource;
            delete this.folder;
            delete this.confirmAdding;
        });

        describe('до подтверждения замены', () => {
            testOperation({
                desc: 'должна спросить подтверждение на замену файла',
                status: 'exists',
                callback: function() {
                    expect(this.confirmAdding.calledOnce).to.be(true);
                }
            });

            testOperation({
                desc: 'не должна повлиять на существующий ресурс',
                status: 'determined',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataExists.id }).getData()
                    ).to.be(this.resourceExistsData);
                }

            });
        });

        describe('после завершения', () => {
            testOperation({
                desc: 'должна пройти соответсвующи статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql([
                        'created',
                        'approved',
                        'determined',
                        'exists',
                        'forced',
                        'moving',
                        'done']
                    );
                }
            });
        });
    });

    describe('в выбранную папку, где присутсвует незагруженный файл с тем же именем', () => {
        beforeEach(function() {
            addResponseModel([{
                error: {
                    id: 'HTTP_412',
                    message: 'Resource already exists'
                }
            }, {
                oid: 1,
                type: 'move'
            }, {
                status: 'WAITING',
                type: 'move'
            }, {
                status: 'EXECUTING',
                type: 'move'
            }, {
                status: 'DONE',
                params: {
                    source: '0001:' + this.dataSrc.id,
                    target: '0001:' + this.dataDst.id
                },
                resource: {
                    ctime: 1368183937,
                    mtime: 1368183937,
                    meta: {},
                    path: this.dataDst.id,
                    type: this.dataDst.type,
                    id: this.dataDst.id,
                    name: this.dataDst.name
                }
            }, {
                ctime: 1368183937,
                mtime: 1368183937,
                meta: {},
                path: this.dataDst.id,
                type: this.dataDst.type,
                id: this.dataDst.id,
                name: this.dataDst.name
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.folder = ns.Model.get('resource', {
                id: this.dataFolder.id
            }).setData(this.dataFolder);

            this.operation = operation.initialize('move', {
                idSrc: this.dataSrc.id,
                idFolderDst: this.dataFolder.id
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });

            this.confirmAdding = sinon.spy((name, item) => {
                item.promise.fulfill();
            });

            //ns.Model.getValid('queueConfirmations').on('added', this.confirmAdding);
        });

        afterEach(function() {
            delete this.resourceExists;
            delete this.resource;
            delete this.folder;
            delete this.confirmAdding;
            delete this.historyStatus;
        });

        testOperation({
            desc: 'должна спросить подтверждение на замену файла',
            status: 'exists',
            callback: function() {
                expect(this.confirmAdding.calledOnce).to.be(true);
            }
        });

        testOperation({
            desc: 'после заершения должна пройти соответсвующие статусы',
            status: 'done',
            callback: function() {
                expect(this.historyStatus).to.be.eql([
                    'created',
                    'approved',
                    'determined',
                    'started',
                    'exists',
                    'forced',
                    'moving',
                    'done'
                ]);
            }
        });
    });

    describe('в выбранную папку, где присутсвует файл с тем же именем, которая (операция) была отменена', () => {
        beforeEach(function() {
            //ns.Model.getValid('queueConfirmations').on('added', (name, item) => {
            //    item.promise.reject();
            //});

            ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            ns.Model.get('resource', {
                id: this.dataFolder.id
            }).setData(this.dataFolder);

            this.operation = operation.initialize('move', {
                idSrc: this.dataSrc.id,
                idFolderDst: this.dataFolder.id
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });
        });

        afterEach(function() {
            delete this.operation;
            delete this.historyStatus;
        });

        describe('в случае, когда файл не загружен', () => {
            beforeEach(() => {
                addResponseModel({
                    error: {
                        id: 'HTTP_412',
                        message: 'Resource already exists'
                    }
                });
            });

            testOperation({
                desc: 'после отмены должна пройти определенные статусы',
                status: 'canceled',
                callback: function() {
                    expect(this.historyStatus).to.be.eql([
                        'created',
                        'approved',
                        'determined',
                        'started',
                        'exists',
                        'canceled'
                    ]);
                },
                statusTestDone: 'canceled'
            });

            testOperation({
                desc: 'после отмены не должна создавать новый файл',
                status: 'canceled',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataDst.id })
                    ).not.to.be.ok();
                },
                statusTestDone: 'canceled'
            });
        });

        describe('в случае, когда файл загружен', () => {
            beforeEach(function() {
                this.dataResourceExists = ns.Model.get('resource', {
                    id: this.dataExists.id
                }).setData(this.dataExists).getData();
            });

            afterEach(function() {
                delete this.dataResourceExists;
            });

            testOperation({
                desc: 'после отмены должна пройти определенные статусы',
                status: 'canceled',
                callback: function() {
                    expect(this.historyStatus).to.be.eql([
                        'created',
                        'approved',
                        'determined',
                        'exists',
                        'canceled'
                    ]);
                },
                statusTestDone: 'canceled'
            });

            testOperation({
                desc: 'после отмены не должна повлиять на существующий ресурс',
                status: 'canceled',
                callback: function() {
                    expect(
                        ns.Model.getValid('resource', { id: this.dataExists.id }).getData()
                    ).to.be.eql(
                        this.dataResourceExists
                    );
                },
                statusTestDone: 'canceled'
            });
        });
    });

    describe('при перемещении папки в папку, для которой существует экземпляр модели `tree`', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'move'
                },
                {
                    status: 'DONE',
                    resource: {
                        type: 'dir',
                        id: '/files/bravo/alfa'
                    }
                },
                // Запрос для модели `tree`.
                {
                    type: 'dir',
                    id: '/files/bravo/alfa',
                    resource: []
                },
                // Запрос для модели `resource`.
                {
                    type: 'dir',
                    id: '/files/bravo/alfa'
                }
            ]);

            this.resource = ns.Model.get('resource', { id: '/files/alfa' }).setData({
                id: '/files/alfa',
                type: 'dir'
            });

            this.folder = ns.Model.get('resource', { id: '/files/bravo' }).setData({
                id: '/files/bravo',
                type: 'dir'
            });

            this.treeOld = ns.Model.get('tree', { id: '/files/alfa' }).setData({
                id: '/files/alfa',
                type: 'dir',
                resource: []
            });

            this.tree = ns.Model.get('tree', { id: '/files/bravo' }).setData({
                id: '/files/bravo',
                type: 'dir',
                resource: []
            });

            this.operation = operation.initialize('move', {
                idSrc: '/files/alfa',
                idFolderDst: '/files/bravo'
            });
        });

        testOperation({
            desc: 'должна создать модель `tree` для нового ресурса',
            status: 'done',
            callback: function() {
                expect(ns.Model.getValid('tree', { id: '/files/bravo/alfa' })).to.be.ok();
            }
        });

        testOperation({
            desc: 'должна добавить скопированную папку в подмодели для `tree`',
            status: 'done',
            callback: function() {
                expect(this.tree.models).to.contain(ns.Model.getValid('tree', { id: '/files/bravo/alfa' }));
            }
        });

        testOperation({
            desc: 'должна удалить экземпляр `tree` для старой папки',
            status: 'done',
            callback: function() {
                expect(ns.Model.getValid('tree', { id: '/files/alfa' })).to.not.be.ok();
            }
        });

        afterEach(function() {
            delete this.resource;
            delete this.folder;
            delete this.tree;
        });
    });

    describe('при перемещении файла в папку, для которой существует экземпляр модели `tree`', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'move'
                },
                {
                    status: 'DONE',
                    resource: {
                        type: 'file',
                        id: '/files/bravo/alfa.txt'
                    }
                },
                // Запрос для модели `resource`.
                {
                    type: 'file',
                    id: '/files/bravo/alfa.txt'
                }
            ]);

            this.resource = ns.Model.get('resource', { id: '/files/alfa.txt' }).setData({
                id: '/files/alfa.txt',
                type: 'file'
            });

            this.folder = ns.Model.get('resource', { id: '/files/bravo' }).setData({
                id: '/files/bravo',
                type: 'dir'
            });

            this.tree = ns.Model.get('tree', { id: '/files/bravo' }).setData({
                id: '/files/bravo',
                type: 'dir',
                resource: []
            });

            this.operation = operation.initialize('move', {
                idSrc: '/files/alfa.txt',
                idFolderDst: '/files/bravo'
            });
        });

        testOperation({
            desc: 'не должна создавать модель `tree` для нового ресурса',
            status: 'done',
            callback: function() {
                expect(ns.Model.getValid('tree', { id: '/files/bravo/alfa.txt' })).to.not.be.ok();
            }
        });

        testOperation({
            desc: 'не должна добавлять скопированный файл в подмодели для `tree`',
            status: 'done',
            callback: function() {
                expect(this.tree.models).to.have.length(0);
            }
        });

        afterEach(function() {
            delete this.resource;
            delete this.folder;
            delete this.tree;
        });
    });

    describe('при перемещении папки Фотокамера', () => {
        beforeEach(function() {
            this.resource = ns.Model.get('resource', { id: '/disk/Фотокамера' }).setData({
                id: '/disk/Фотокамера',
                type: 'folder'
            });

            this.operation = operation.initialize('move', {
                idSrc: '/disk/Фотокамера',
                idFolderDst: '/files/bravo'
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });

            sinon.stub(this.operation, 'confirm', () => {
                return Vow.reject();
            });
            sinon.stub(helperResource, 'isCamera', no.true);
        });

        afterEach(function() {
            this.operation.confirm.restore();
            helperResource.isCamera.restore();
            delete this.historyStatus;
            delete this.resource;
        });

        testOperation({
            desc: 'должна спросить подтверждение',
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

    describe('общей папки, с загруженными родителями', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'move'
                },
                {
                    status: 'DONE',
                    resource: {
                        type: 'dir',
                        id: '/root/destination/shared',
                        meta: {
                            group: {
                                is_root: 1,
                                gid: 555
                            }
                        }
                    }
                },
                {
                    type: 'dir',
                    id: '/root/destination/shared',
                    meta: {
                        group: {
                            is_root: 1,
                            gid: 555
                        }
                    }
                }
            ]);

            this.resourceSource = ns.Model.get('resource', { id: '/root/source' }).setData({
                id: '/root/source',
                name: 'source',
                type: 'dir',
                meta: { with_shared: 1 }
            });

            this.resourceDestination = ns.Model.get('resource', { id: '/root/destination' }).setData({
                id: '/root/destination',
                name: 'destination',
                type: 'dir'
            });

            this.resource = ns.Model.get('resource', { id: '/root/source/shared' }).setData({
                id: '/root/source/shared',
                name: 'shared',
                type: 'dir',
                meta: {
                    group: {
                        is_root: 1,
                        gid: 555
                    }
                }
            });

            this.operation = operation.initialize('move', {
                idSrc: this.resource.get('.id'),
                idFolderDst: this.resourceDestination.get('.id')
            });

            sinon.stub(this.resourceSource, 'fetch');
            sinon.stub(this.resourceDestination, 'fetch');
        });

        afterEach(function() {
            this.resourceSource.fetch.restore();
            this.resourceDestination.fetch.restore();
            delete this.operation;
            delete this.resource;
            delete this.resourceSource;
            delete this.resourceDestination;
        });

        testOperation({
            desc: 'должна перезапросить бывшего родителя',
            status: 'done',
            callback: function() {
                expect(this.resourceSource.fetch.calledOnce).to.be(true);
            }
        });

        testOperation({
            desc: 'должна перезапросить нового родителя',
            status: 'done',
            callback: function() {
                expect(this.resourceDestination.fetch.calledOnce).to.be(true);
            }
        });
    });

    describe('папки, содержащей ОП, с загруженными родителями', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'move'
                },
                {
                    status: 'DONE',
                    resource: {
                        type: 'dir',
                        id: '/root/destination/with shared',
                        meta: { with_shared: 1 }
                    }
                },
                {
                    type: 'dir',
                    id: '/root/destination/with shared',
                    meta: { with_shared: 1 }
                }
            ]);

            this.resourceSource = ns.Model.get('resource', { id: '/root/source' }).setData({
                id: '/root/source',
                name: 'source',
                type: 'dir',
                meta: { with_shared: 1 }
            });

            this.resourceDestination = ns.Model.get('resource', { id: '/root/destination' }).setData({
                id: '/root/destination',
                name: 'destination',
                type: 'dir'
            });

            this.resource = ns.Model.get('resource', { id: '/root/source/with shared' }).setData({
                id: '/root/source/with shared',
                name: 'with shared',
                type: 'dir',
                meta: { with_shared: 1 }
            });

            this.operation = operation.initialize('move', {
                idSrc: this.resource.get('.id'),
                idFolderDst: this.resourceDestination.get('.id')
            });

            sinon.stub(this.resourceSource, 'fetch');
            sinon.stub(this.resourceDestination, 'fetch');
        });

        afterEach(function() {
            delete this.operation;
            delete this.resource;
            delete this.resourceSource;
            delete this.resourceDestination;
        });

        testOperation({
            desc: 'должна перезапросить бывшего родителя',
            status: 'done',
            callback: function() {
                expect(this.resourceSource.fetch.calledOnce).to.be(true);
            }
        });

        testOperation({
            desc: 'должна перезапросить нового родителя',
            status: 'done',
            callback: function() {
                expect(this.resourceDestination.fetch.calledOnce).to.be(true);
            }
        });
    });
});
