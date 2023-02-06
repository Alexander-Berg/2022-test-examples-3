require('models/resource/resource');
require('models/operation/operation-rename');

const helperOperation = require('helpers/operation');

// TODO: Операция не покрыта полностью, покрыт только один случай повторного
// запуска операции. Удалите этот комментарий при покрытии операции.
describe('Переименования', () => {
    beforeEach(function() {
        this.resourceData = {
            id: '/files/test.txt',
            type: 'file'
        };
        this.resource = ns.Model.get('resource', { id: this.resourceData.id }).setData(this.resourceData);

        const operation = this.operation = helperOperation.initialize('rename', {
            idSrc: this.resourceData.id
        });

        const history = this.historyStatus = [];

        this.operation.on('ns-model-changed.status', () => {
            history.push(operation.get('.status'));
        });
    });

    it('после создания должна закэшировать srcType', function() {
        expect(this.operation.get('.srcType')).to.be.eql(this.resourceData.type);
    });

    describe('при повторном переименовании', () => {
        beforeEach(function() {
            this.existingOp = helperOperation.create('rename', {
                idSrc: '/files/test.txt'
            });
        });

        testOperation({
            desc: 'должна остановиться и уступить место существующей операции',
            status: 'duplicate',
            statusTestDone: 'duplicate',
            callback: function() {
                expect(this.historyStatus).to.eql([
                    'created',
                    'duplicate'
                ]);
            }
        });

        afterEach(function() {
            delete this.existingOp;
        });
    });

    describe('при переименовании в то же самое имя', () => {
        beforeEach(function() {
            sinon.stub(this.resource, 'rename', () => {
                return Vow.fulfill('test.txt');
            });
        });

        testOperation({
            desc: 'должна отмениться',
            status: 'canceled',
            statusTestDone: 'canceled',
            callback: function() {
                expect(this.historyStatus).to.eql([
                    'created',
                    'canceled'
                ]);
            }
        });
    });

    afterEach(function() {
        delete this.operation;
        delete this.resource;
        delete this.historyStatus;
    });
});
