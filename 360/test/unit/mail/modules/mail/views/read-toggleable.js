describe('Daria.vReadToggleable', function() {
    beforeEach(function() {
        this.mMessage = ns.Model.get('message', {ids: 5});
        this.view = ns.View.create('read-toggleable', {ids: 5, type: 'messages'});
    });

    describe('#computeFsmModelState', function() {
        it('Должен вернуть "toRead" если письмо не прочитано', function() {
            this.sinon.stub(this.mMessage, 'isNew').returns(true);

            expect(this.view.computeFsmModelState()).to.be.eql('toRead');
        });

        it('Должен вернуть "toUnmute" если письмо прочитано и замьюченно', function() {
            this.sinon.stub(this.mMessage, 'isNew').returns(false);
            this.sinon.stub(this.mMessage, 'isMuted').returns(true);

            expect(this.view.computeFsmModelState()).to.be.eql('toUnmute');
        });

        it('Должен вернуть "toUnread" если письмо прочитано и не замьюченно', function() {
            this.sinon.stub(this.mMessage, 'isNew').returns(false);
            this.sinon.stub(this.mMessage, 'isMuted').returns(false);

            expect(this.view.computeFsmModelState()).to.be.eql('toUnread');
        });
    });

    describe('#toggleState', function() {
        beforeEach(function() {
            this.mReadMuteFsm = ns.Model.get('message-read-mute-fsm', {ids: 5, type: 'messages'});

            this.stubs = {
                mMessage: {
                    isNew: this.sinon.stub(this.mMessage, 'isNew'),
                    isMuted: this.sinon.stub(this.mMessage, 'isMuted')
                },
                mReadMuteFsm: {
                    getState: this.sinon.stub(this.mReadMuteFsm, 'getState')
                }
            };

            this.sinon.stub(this.mReadMuteFsm, 'setState');
            this.view.canMute = true;
        });

        // State toMute

        it('Должен установить состояние "toMute", если текущее состояние "toRead", письмо можно замьютить и мьют доступен для пользователя', function() {
            this.sinon.stub(this.mMessage, 'isMutable').returns(true);
            this.stubs.mReadMuteFsm.getState.returns('toRead');
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.calledWith('toMute');
        });

        it('Не должен устанавливать состояние "toMute", если мьют не доступен для пользователя', function() {
            this.view.canMute = false;
            this.sinon.stub(this.mMessage, 'isMutable').returns(true);
            this.stubs.mReadMuteFsm.getState.returns('toRead');
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.not.calledWith('toMute');
        });

        // State toUnmute

        it('Должен установить состояние "toUnmute", если текущее состояние "toMute" и нет непрочитанных писем', function() {
            this.stubs.mReadMuteFsm.getState.returns('toMute');
            this.stubs.mMessage.isNew.returns(false);
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.calledWith('toUnmute');
        });

        it('Должен установить состояние "toUnmute", если текущее состояние "toUnread" и тред замьючен', function() {
            this.stubs.mReadMuteFsm.getState.returns('toUnread');
            this.stubs.mMessage.isMuted.returns(true);
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.calledWith('toUnmute');
        });

        // State toUnread

        it('Должен установить состояние "toUnread", если текущее состояние "toRead" и письмо нельзя замьютить', function() {
            this.sinon.stub(this.mMessage, 'isMutable').returns(false);
            this.stubs.mReadMuteFsm.getState.returns('toRead');
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.calledWith('toUnread');
        });

        it('Должен установить состояние "toUnread", если текущее состояние "toUnmute" и нет непрочитанных писем', function() {
            this.stubs.mReadMuteFsm.getState.returns('toUnmute');
            this.stubs.mMessage.isNew.returns(false);
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.calledWith('toUnread');
        });

        // State toRead

        it('Должен установить состояние "toRead", если текущее состояние "toUnread" и тред не замьючен', function() {
            this.stubs.mReadMuteFsm.getState.returns('toUnread');
            this.stubs.mMessage.isMuted.returns(false);
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.calledWith('toRead');
        });

        it('Должен установить состояние "toRead", если текущее состояние "toMute" и есть непрочитанные письма', function() {
            this.stubs.mReadMuteFsm.getState.returns('toMute');
            this.stubs.mMessage.isNew.returns(true);
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.calledWith('toRead');
        });

        it('Должен установить состояние "toRead", если текущее состояние "toUnmute" и есть непрочитанные письма', function() {
            this.stubs.mReadMuteFsm.getState.returns('toUnmute');
            this.stubs.mMessage.isNew.returns(true);
            this.view.toggleState();

            expect(this.mReadMuteFsm.setState).to.be.calledWith('toRead');
        });
    });

    describe('#onMessageChange', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'setStartState');
        });

        it('Должен установить начальное состояние FSM, если модель сообщения полностью обновилась', function() {
            this.view.onMessageChange('ns-model-changed', '');

            expect(this.view.setStartState).to.have.callCount(1);
        });

        it('Не должен устанавливать начальное состояние FSM, если модель сообщение обновилась не полностью', function() {
            this.view.onMessageChange('ns-model-changed', '.lid');

            expect(this.view.setStartState).to.have.callCount(0);
        });

    });

    describe('#onMessageNewChange', function() {
        beforeEach(function() {
            this.mReadMuteFsm = ns.Model.get('message-read-mute-fsm', {ids: 5, type: 'messages'});
            this.sinon.stub(this.mReadMuteFsm, 'getState');
            this.sinon.stub(this.mMessage, 'isMuted');
            this.sinon.stub(this.mMessage, 'toggleMute');
            this.sinon.stub(this.mMessage, 'isThread');
            this.sinon.stub(this.mMessage, 'isNew');
            this.sinon.stub(this.view, 'toggleState');
        });

        it('Должен выключить замьюченность если тред замьючен', function() {
            // Это может произойти, если, например, тред замьючен, а мы делаем письмо внутри треда непрочитанным.
            this.mMessage.isMuted.returns(true);

            this.view.onMessageNewChange();

            expect(this.mMessage.toggleMute).to.have.callCount(1);
        });

        it('Не должен выключать замьюченность если тред не замьючен', function() {
            this.mMessage.isMuted.returns(false);

            this.view.onMessageNewChange();

            expect(this.mMessage.toggleMute).to.have.callCount(0);
        });

        it('Не должен запускать переключение состояния FSM, если тред замьючен', function() {
            this.mMessage.isMuted.returns(true);

            this.view.onMessageNewChange();

            expect(this.view.toggleState).to.have.callCount(0);
        });

        it('Не должен запускать переключение состояния FSM, если тред не прочитан и в нем есть непрочитанные письма', function() {
            this.mMessage.isMuted.returns(false);
            this.mMessage.isThread.returns(true);
            this.mMessage.isNew.returns(true);
            this.mReadMuteFsm.getState.returns('toRead');

            this.view.onMessageNewChange();

            expect(this.view.toggleState).to.have.callCount(0);
        });

        it('Должен запустить переключение состояния FSM, если это обычное письмо, а не тред', function() {
            this.mMessage.isMuted.returns(false);
            this.mMessage.isThread.returns(false);

            this.view.onMessageNewChange();

            expect(this.view.toggleState).to.have.callCount(1);
        });

        it('Должен запустить переключение состояния FSM, если тред не находится в состояние "toRead"', function() {
            this.mMessage.isMuted.returns(false);
            this.mMessage.isThread.returns(true);
            this.mReadMuteFsm.getState.returns('toUnread');

            this.view.onMessageNewChange();

            expect(this.view.toggleState).to.have.callCount(1);
        });

        it('Должен запустить переключение состояния FSM, если в треде нет непрочитанных писем', function() {
            this.mMessage.isMuted.returns(false);
            this.mMessage.isThread.returns(true);
            this.mReadMuteFsm.getState.returns('toRead');
            this.mMessage.isNew.returns(false);

            this.view.onMessageNewChange();

            expect(this.view.toggleState).to.have.callCount(1);
        });
    });

    describe('#onMessageLidChange', function() {
        beforeEach(function() {
            this.sinon.stub(this.mMessage, 'isThread');
            this.sinon.stub(this.mMessage, 'hasLabelInDiff');
            this.sinon.stub(this.view, 'toggleState');
        });

        it('Не должен запускать переключение состояния FSM, для обычного письма(не треда)', function() {
            this.mMessage.isThread.returns(false);

            this.view.onMessageLidChange();

            expect(this.view.toggleState).to.have.callCount(0);
        });

        it('Не должен запускать переключение состояния FSM, если изменения в метках не касаются мьюта', function() {
            this.mMessage.isThread.returns(true);
            this.mMessage.hasLabelInDiff.returns(false);

            this.view.onMessageLidChange();

            expect(this.view.toggleState).to.have.callCount(0);
        });

        it('Должен запустить переключение состояния FSM, если изменения в метках касаются мьюта', function() {
            this.mMessage.isThread.returns(true);
            this.mMessage.hasLabelInDiff.returns(true);

            this.view.onMessageLidChange();

            expect(this.view.toggleState).to.have.callCount(1);
        });
    });
});
