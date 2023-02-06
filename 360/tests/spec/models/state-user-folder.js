const operation = require('helpers/operation');

require('models/users/user-folder');
require('models/users/state-user-folder');
require('models/users/states-user-folder');

describe('Модель состояния пользователя папки', () => {
    beforeEach(function() {
        this.id = '/disk/path/to/folder';
        this.userid = 1;

        this.state = ns.Model.get('stateUserFolder', {
            id: this.id,
            userid: this.userid
        });

        this.onModelChange = sinon.spy();
        this.state.on('ns-model-changed', this.onModelChange);

        this.userFolder = ns.Model.get('userFolder', {
            id: this.id,
            userid: this.userid
        });
    });

    afterEach(function() {
        delete this.id;
        delete this.userid;
        delete this.state;
        delete this.userFolder;
        delete this.onModelChange;
    });

    it('Должна добавить себя в соотв. коллекцию состояний пользователей', function() {
        expect(
            ns.Model.get('statesUserFolder', { id: this.id }).models[0].key
        ).to.be(
            this.state.key
        );
    });

    it('При отсутсвии привязанной операции и вызове `unbindOperation` не должна тригеррить событие `ns-model-changed`', function() {
        this.state.unbindOperation();
        expect(this.onModelChange.called).not.to.be.ok();
    });

    it('При отсутсвии привязанной операции и вызове `resetOperation` модель пользователя папки не должна измениться', function() {
        this.state.resetOperation();
        expect(this.userFolder.status).to.be('none');
    });

    describe('При привязке на операцию (метод `bindOperation`)', () => {
        beforeEach(function() {
            this.operation = operation.initialize('stub', { name: 'operation' });
            this.operation2 = operation.initialize('stub', { name: 'operation2' });

            sinon.spy(this.state, 'onOperationStatusChanged');
            sinon.spy(this.state, 'unbindOperation');
            sinon.spy(this.state, 'resetOperation');

            this.state.bindOperation(this.operation);

            this.operation.isFinalized = function() {};
            this.operation.getType = function() {};
        });

        afterEach(function() {
            delete this.operation;
            delete this.operation2;
        });

        it('Должна создать у себя ссылку на операцию', function() {
            expect(this.state.operation).to.be(this.operation);
        });

        it('При повтроном бинде должна вызвать метод `unbindOperation`', function() {
            this.state.bindOperation(this.operation);
            expect(this.state.unbindOperation.calledOnce).to.be.ok();
        });

        it('При повторном бинде другой операции должна анбиндить старую операцию', function() {
            this.state.bindOperation(this.operation2);
            expect(this.state.operation).to.be(this.operation2);
        });

        it('Должна стригерить на себе событие `ns-model-changed`', function() {
            expect(this.onModelChange.calledOnce).to.be.ok();
        });

        it('При изменениия статуса операции должна вызвать свой метод `onOperationStatusChanged`', function() {
            this.operation.setStatus('done');
            expect(this.state.onOperationStatusChanged.calledOnce).to.be.ok();
        });

        it('При уничтожении операции должен анбиндить операцию', function() {
            ns.Model.destroy(this.operation);
            expect(this.state.unbindOperation.calledOnce).to.be.ok();
        });

        describe('При последующей отвязке отоперации', () => {
            beforeEach(function() {
                this.state.unbindOperation();
            });

            it('Должна создать у себя ссылку на операцию', function() {
                expect(this.state.operation).not.to.be.ok();
            });

            it('Должна стригерить на себе еще одно событие `ns-model-changed`', function() {
                expect(this.onModelChange.callCount).to.be(2);
            });

            it('При изменениия статуса операции не должна вызвать свой метод `onOperationStatusChanged`', function() {
                this.operation.setStatus('done');
                expect(this.state.onOperationStatusChanged.called).not.to.be.ok();
            });

            it('При уничтожении операции не должен повторно анбиндить операцию', function() {
                ns.Model.destroy(this.operation);
                expect(this.state.unbindOperation.calledOnce).to.be.ok();
            });
        });

        it('Метод `reset` должен вызвать метод `resetOperation`', function() {
            this.state.reset();
            expect(this.state.resetOperation.calledOnce).to.be.ok();
        });

        describe('Метод `resetOperation`', () => {
            it('Должен отвязать модель от операции, если она завершилась', function() {
                this.operation.isFinalized = function() {
                    return true;
                };
                this.state.resetOperation();
                expect(this.state.unbindOperation.calledOnce).to.be.ok();
            });

            it('Должен отвязать модель от операции, если она про настройку доступа', function() {
                this.operation.getType = function() {
                    return 'accessFolder';
                };
                this.state.resetOperation();
                expect(this.state.unbindOperation.calledOnce).to.be.ok();
            });

            it('Должен удалить пользователя папки, если операция приглашения сорвалась', function() {
                this.operation.getType = function() {
                    return 'inviteFolder';
                };
                this.operation.setStatus('failed');
                this.state.resetOperation();
                expect(
                    ns.Model.getValid('userFolder', {
                        id: this.id,
                        userid: this.userid
                    })
                ).not.to.be.ok();
            });
        });

        it('Метод `getData` должен вернуть ссылку на операцию', function() {
            expect(this.state.getData().operation).to.be(this.operation.getData());
        });
    });
});
