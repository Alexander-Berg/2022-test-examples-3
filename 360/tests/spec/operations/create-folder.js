require('models/resource/resource');
require('models/operation/operation-create-folder');

const helperOperation = require('helpers/operation');

describe('Операция создания новой папки ->', () => {
    describe('восстановленная удалённая операция ->', () => {
        beforeEach(function() {
            this.operation = helperOperation.initialize('createFolder', {
                idDst: '/disk/foo'
            });
            this.doRequestCreate = sinon.stub(this.operation, 'doRequestCreate', () => {});
        });
        afterEach(function() {
            this.doRequestCreate.reset();
            const idDst = this.operation.get('.idDst');
            if (idDst) {
                const resource = ns.Model.getValid('resource', { id: idDst });
                resource && resource.destroy();
            }
        });
        testOperation({
            desc: 'после создания должна оказаться в статусе determined',
            status: 'determined',
            statusTestDone: 'determined',
            callback: no.nop
        });
    });
    describe('ошибка в запросе на создание папки ->', () => {
        beforeEach(function() {
            const idContext = '/disk/foo';

            ns.Model.get('resource', {
                id: idContext
            }).setData({
                id: idContext,
                name: 'foo',
                path: idContext,
                type: 'dir'
            });

            this.operation = helperOperation.initialize('createFolder', {
                idFolderDst: '/disk/foo'
            });
            this.doRenameStub = sinon.stub(this.operation, 'doRename', () => {
                this.operation.set('.nameFolderDst', 'hello, world!');
                this.operation.setStatus('validating');
            });
        });
        afterEach(function() {
            this.doRenameStub.reset();
        });
        describe('в случае HTTP_412', () => {
            beforeEach(() => {
                addResponseModel([
                    // ответ ручки do-resource-create-folder
                    {
                        error: { id: 'HTTP_412' }
                    }
                ]);
            });
            testOperation({
                desc: 'должна перейти в статус exists',
                status: 'exists',
                statusTestDone: 'exists',
                callback: no.nop
            });
        });
        describe('в случае HTTP_405', () => {
            beforeEach(() => {
                addResponseModel([
                    // ответ ручки do-resource-create-folder
                    {
                        error: { id: 'HTTP_405' }
                    }
                ]);
            });
            testOperation({
                desc: 'должна перейти в статус exists',
                status: 'exists',
                statusTestDone: 'exists',
                callback: no.nop
            });
        });
        describe('в случае неизвестной ошибки', () => {
            beforeEach(() => {
                addResponseModel([
                    // ответ ручки do-resource-create-folder
                    {
                        error: { id: 'HTTP_500' }
                    }
                ]);
            });
            testOperation({
                desc: 'должна перейти в статус failed',
                status: 'failed',
                statusTestDone: 'failed',
                callback: no.nop
            });
        });
    });
    describe('в листинге ->', () => {
        beforeEach(function() {
            this.idContext = '/disk/foo';

            ns.Model.get('resource', {
                id: this.idContext
            }).setData({
                id: this.idContext,
                name: 'foo',
                path: this.idContext,
                type: 'dir'
            });

            this.operation = helperOperation.initialize('createFolder', {
                idFolderDst: '/disk/foo'
            });
            this.doRequestCreate = sinon.stub(this.operation, 'doRequestCreate', no.nop);
        });
        afterEach(function() {
            this.doRequestCreate.reset();
        });
        describe('должна создать фейковую новую папку', () => {
            testOperation({
                desc: 'в указанной родительской папке',
                status: 'created',
                statusTestDone: 'created',
                callback: function() {
                    expect(
                        this.operation.get('.idSrc').indexOf(this.idContext + '/')
                    ).to.be.eql(0);
                }
            });
        });
        describe('после успешного задания имени для папки', () => {
            beforeEach(function() {
                this.doRenameStub = sinon.stub(this.operation, 'doRename', () => {
                    this.operation.set('.nameFolderDst', 'hello, world!');
                    this.operation.setStatus('validating');
                });
            });
            afterEach(function() {
                this.doRenameStub.reset();
            });
            testOperation({
                desc: 'должна перевести операция в статус determined, записать новое имя в поле nameFolderDst и спрятать фейковую папку',
                status: 'determined',
                statusTestDone: 'determined',
                callback: function() {
                    expect(
                        this.operation.get('.nameFolderDst')
                    ).to.be.eql('hello, world!');
                    expect(
                        this.operation.get('.src').get('.state.hidden')
                    ).to.be.ok();
                }
            });
        });
        describe('в случае задания невалидного имени для папки', () => {
            beforeEach(function() {
                this.doRenameStub = sinon.stub(this.operation, 'doRename', () => {
                    this.operation.set('.nameFolderDst', 'hello/world');
                    this.operation.setStatus('validating');
                });
            });
            afterEach(function() {
                this.doRenameStub.reset();
            });
            testOperation({
                desc: 'должна перейти в статус canceled и выставить флаг reason равным NAME_HAS_SLASH',
                status: 'failed',
                statusTestDone: 'failed',
                callback: function() {
                    expect(
                        this.operation.get('.reason')
                    ).to.be.eql('NAME_HAS_SLASH');
                }
            });
        });
    });
});
