import 'models/tree/tree';
import 'models/queues/queue-notifications';
//import 'models/queues/queue-confirmations';
import 'models/operation/operation-copy';
import operation from 'helpers/operation';

describe('Копирования', () => {
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
        const existingOp = operation.create('copy', {
            idSrc: dataSrc.id
        });
        expect(existingOp.get('.srcType')).to.be.eql(dataSrc.type);
    });

    describe('при удачном копировании', () => {
        beforeEach(function() {
            addResponseModel([{
                oid: 1,
                type: 'copy'
            }, {
                status: 'WAITING',
                type: 'copy'
            }, {
                status: 'EXECUTING',
                type: 'copy'
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
        });

        afterEach(function() {
            delete this.resource;
            delete this.folder;
        });

        describe('в папку которую предстоит выбрать', () => {
            beforeEach(function() {
                this.operation = operation.initialize('copy', {
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
                desc: 'после запуска должна запросить папку назаначения один раз',
                status: 'created',
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
                        'determined',
                        'progressing',
                        'completed',
                        'done'
                    ]);
                }
            });
        });

        describe('в уже выбранную папку', () => {
            beforeEach(function() {
                this.operation = operation.initialize('copy', {
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
                            'determined',
                            'progressing',
                            'completed',
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
                type: 'copy'
            }, {
                status: 'WAITING',
                type: 'copy'
            }, {
                status: 'EXECUTING',
                type: 'copy'
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

            this.operation = operation.initialize('copy', {
                idSrc: this.dataSrc.id,
                idFolderDst: this.dataFolder.id
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });

            this.confirmAdding = sinon.spy((name, item) => {
                item.promise.fulfill({ name: this.dataSrc.id + '_2018' });
            });

            //ns.Model.getValid('queueConfirmations').on('added', this.confirmAdding);
        });

        afterEach(function() {
            delete this.resourceExists;
            delete this.resource;
            delete this.folder;
            delete this.confirmAdding;
        });

        describe('до смены имени замены', () => {
            testOperation({
                desc: 'должна спросить новое имя файла',
                status: 'exists',
                callback: function() {
                    expect(this.confirmAdding.calledOnce).to.be(true);
                }
            });

            testOperation({
                desc: 'не должна повлиять на существующий ресурс',
                status: 'exists',
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
                        'determined',
                        'exists',
                        'determined',
                        'progressing',
                        'completed',
                        'done'
                    ]);
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
                type: 'copy'
            }, {
                status: 'WAITING',
                type: 'copy'
            }, {
                status: 'EXECUTING',
                type: 'copy'
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

            this.operation = operation.initialize('copy', {
                idSrc: this.dataSrc.id,
                idFolderDst: this.dataFolder.id
            });

            this.historyStatus = [];
            this.operation.on('ns-model-changed.status', () => {
                this.historyStatus.push(this.operation.get('.status'));
            });

            this.confirmAdding = sinon.spy((name, item) => {
                item.promise.fulfill({ name: this.dataSrc.id + '_2018' });
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
            desc: 'должна спросить новое имя файла',
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
                    'determined',
                    'exists',
                    'determined',
                    'progressing',
                    'completed',
                    'done'
                ]);
            }
        });
    });

    describe('в выбранную папку, где присутсвует файл с тем же именеме, которая (операция) была отменена', () => {
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

            this.operation = operation.initialize('copy', {
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
                        'determined',
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

    describe('при копировании папки в папку, для которой существует экземпляр модели `tree`', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'copy'
                },
                {
                    status: 'DONE',
                    resource: {
                        type: 'dir',
                        id: '/files/bravo/alfa'
                    }
                },
                // Запрос для модели `resource`.
                {
                    type: 'dir',
                    id: '/files/bravo/alfa'
                },
                // Запрос для модели `tree`.
                {
                    type: 'dir',
                    id: '/files/bravo/alfa',
                    resource: []
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

            this.tree = ns.Model.get('tree', { id: '/files/bravo' }).setData({
                id: '/files/bravo',
                type: 'dir',
                resource: []
            });

            this.operation = operation.initialize('copy', {
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

        afterEach(function() {
            delete this.resource;
            delete this.folder;
            delete this.tree;
        });
    });

    describe('при копировании файла в папку, для которой существует экземпляр модели `tree`', () => {
        beforeEach(function() {
            addResponseModel([
                {
                    oid: 1,
                    type: 'copy'
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

            this.operation = operation.initialize('copy', {
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
});
