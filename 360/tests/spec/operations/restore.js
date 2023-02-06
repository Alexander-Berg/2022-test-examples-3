require('models/resource/resource');
require('models/queues/queue-notifications');
require('models/operation/operation-restore');
require('models/operations/operations-active');

const operation = require('helpers/operation');
const guid = require('helpers/guid');

describe('restore', () => {
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
            id: '/trash/test.txt',
            name: 'test.txt',
            type: 'file'
        };
        ns.Model.get('resource', {
            id: dataSrc.id
        }).setData(dataSrc);
        const existingOp = operation.create('restore', {
            idSrc: dataSrc.id
        });
        expect(existingOp.get('.srcType')).to.be.eql(dataSrc.type);
    });

    describe('будучи успешной', () => {
        beforeEach(function() {
            this.dataSrc = {
                id: '/trash/test.jpg',
                name: 'test.jpg',
                type: 'file'
            };

            this.dataDst = {
                id: '/disk/path/to/test.jpg',
                name: 'test.jpg',
                type: 'file',
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
                    id: this.dataDst.id,
                    name: this.dataDst.name
                },
                type: 'trash'
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.operation = operation.initialize('restore', {
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
                status: 'created',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.state.hidden')).to.be(true);
                }
            });
        });

        describe('после завершения', () => {
            testOperation({
                desc: 'должна пройти определенные статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql(['created', 'progressing', 'done']);
                }
            });

            testOperation({
                desc: 'должна удалить исходный ресурс из кешей',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', this.resource.key)).not.to.be.ok();
                }
            });

            testOperation({
                desc: 'должна создать восстановленный ресурс',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataDst.id }).get('.state.part')).not.to.be(true);
                }
            });

            testOperation({
                desc: 'должна создать нотификацию',
                status: 'done',
                callback: function() {
                    expect(this.notifAdding.calledOnce).to.be.ok();
                }
            });
        });
    });

    describe('ресурса в папку, где существует ресурс с тем же именем', () => {
        beforeEach(function() {
            this.dataSrc = {
                id: '/trash/test.jpg',
                name: 'test.jpg',
                type: 'file'
            };

            const now = $.now();

            this.dataDst = {
                id: '/disk/path/to/test.jpg_' + now,
                name: 'test.jpg_' + now,
                type: 'file'
            };

            this.resourceExisting = {
                id: '/disk/path/to/test.jpg',
                name: 'test.jpg',
                type: 'file'
            };

            ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            ns.Model.get('resource', {
                id: this.resourceExisting.id
            }).setData(this.resourceExisting);

            addResponseModel([{
                error: {
                    id: 'HTTP_412',
                    message: 'Resource already exists'
                }
            }, {
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
                    id: this.dataDst.id,
                    name: this.dataDst.name
                },
                type: 'trash'
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.operation = operation.initialize('operationRestore', {
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
                desc: 'операция должна создать новое имя, которое будет у файла после восстановления ',
                status: 'exists',
                callback: function() {
                    expect(this.operation.get('.nameNew')).to.be.ok();
                }
            });
        });

        describe('после завершения', () => {
            testOperation({
                desc: 'должна создать ресурс с другим именем',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataDst.id }).get('.id')).to.be(this.dataDst.id);
                }
            });

            testOperation({
                desc: 'должна пройти определенные статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql(['created', 'exists', 'progressing', 'done']);
                }
            });
        });
    });

    describe('ресурса, которое влечет за собой создание родительских папок', () => {
        beforeEach(function() {
            this.dataSrc = {
                id: '/trash/test.jpg',
                name: 'test.jpg',
                type: 'file'
            };

            this.dataDst = {
                id: '/disk/path/to',
                name: 'to',
                type: 'dir'
            };

            ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

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
                    id: this.dataDst.id,
                    name: this.dataDst.name
                },
                type: 'trash'
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.operation = operation.initialize('operationRestore', {
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
        });

        describe('после завершения', () => {
            testOperation({
                desc: 'должна создать созданную в процессе восстановления папку',
                status: 'done',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataDst.id }).get('.id')).to.be(this.dataDst.id);
                }
            });

            testOperation({
                desc: 'должна пройти определенные статусы',
                status: 'done',
                callback: function() {
                    expect(this.historyStatus).to.be.eql(['created', 'progressing', 'done']);
                }
            });
        });
    });

    describe('будучи не успешной', () => {
        beforeEach(function() {
            this.dataSrc = {
                id: '/trash/test.jpg',
                name: 'test.jpg',
                type: 'file'
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
                status: 'FAILED',
                error: {},
                type: 'trash'
            }]);

            this.resource = ns.Model.get('resource', {
                id: this.dataSrc.id
            }).setData(this.dataSrc);

            this.operation = operation.initialize('restore', {
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
            delete this.historyStatus;
            delete this.resource;
        });

        describe('после старта', () => {
            testOperation({
                desc: 'должна спрятать ресурс',
                status: 'created',
                statusTestDone: 'failed',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.state.hidden')).to.be(true);
                }
            });
        });

        describe('после завершения', () => {
            testOperation({
                desc: 'должна пройти определенные статусы',
                status: 'failed',
                statusTestDone: 'failed',
                callback: function() {
                    expect(this.historyStatus).to.be.eql(['created', 'progressing', 'failed']);
                }
            });

            testOperation({
                desc: 'должна показать ресурс',
                status: 'failed',
                statusTestDone: 'failed',
                callback: function() {
                    expect(ns.Model.getValid('resource', { id: this.dataSrc.id }).get('.state.hidden')).to.be(false);
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

    describe('будучи восстановленной', () => {
        it('должна создасться со статусом `progressing`', (done) => {
            const idSrc = '/trash/biggie';
            const activeOperations = ns.Model.get('operationsActive');

            addResponse({
                body: JSON.stringify({
                    models: [
                        {
                            data: [
                                {
                                    data: {
                                        path: idSrc
                                    },
                                    subtype: 'restore',
                                    type: 'trash'
                                }
                            ]
                        }
                    ]
                })
            });

            activeOperations.revive().then(() => {
                const operation = ns.Model.getValid('operationRestore', { id: guid.current() });

                expect(operation).to.be.ok();
                expect(operation.get('.status')).to.equal('progressing');
                expect(ns.Model.getValid('resource', { id: operation.get('.idSrc') }).get('.state.hidden')).to.be(true);

                done();
            });
        });
    });
});
