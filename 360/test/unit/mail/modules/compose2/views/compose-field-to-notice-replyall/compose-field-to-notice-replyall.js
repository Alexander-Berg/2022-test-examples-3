describe('Daria.vComposeFieldToNoticeReplyall', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-field-to-notice-replyall');
        this.mComposeState = this.view.getModel('compose-state');
        this.mComposeMessage = this.view.getModel('compose-message');
    });

    describe('#onReplyAllLinkClick', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'changeRecipientsToOpposite');
        });

        it('должен вызвать метод изменения получателей', function() {
            this.view.onReplyAllLinkClick($.Event('click'));
            expect(this.mComposeMessage.changeRecipientsToOpposite).to.have.callCount(1);
        });

        it('должен установить признак скрытия плашки переключения режима', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isReplyAllNoticeVisible', false);
        });

        it('должен установить признак показа поля CC, если есть данные', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.cc').returns('test');
            this.sinon.stub(this.mComposeState, 'isCcVisible').returns(false);

            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isCcVisible', true, {
                'force': false
            });
        });

        it('должен установить признак показа поля CC, если есть данные, с форсом, если поле показано', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.cc').returns('test');
            this.sinon.stub(this.mComposeState, 'isCcVisible').returns(true);

            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isCcVisible', true, {
                'force': true
            });
        });

        it('должен снять признак показа поля CC, если нет данных', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.cc').returns('');
            this.sinon.stub(this.mComposeState, 'isCcVisible').returns(false);

            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isCcVisible', false, {
                'force': false
            });
        });

        it('должен снять признак показа поля CC, если нет данных, с форсом, если поле показано', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.cc').returns('');
            this.sinon.stub(this.mComposeState, 'isCcVisible').returns(true);

            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isCcVisible', false, {
                'force': true
            });
        });

        it('должен установить признак показа поля BCC, если есть данные', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.bcc').returns('test');
            this.sinon.stub(this.mComposeState, 'isBccVisible').returns(false);

            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isBccVisible', true, {
                'force': false
            });
        });

        it('должен установить признак показа поля BCC, если есть данные, с форсом, если поле показано', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.bcc').returns('test');
            this.sinon.stub(this.mComposeState, 'isBccVisible').returns(true);

            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isBccVisible', true, {
                'force': true
            });
        });

        it('должен снять признак показа поля BCC, если нет данных', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.bcc').returns('');
            this.sinon.stub(this.mComposeState, 'isBccVisible').returns(false);

            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isBccVisible', false, {
                'force': false
            });
        });

        it('должен снять признак показа поля BCC, если нет данных, с форсом, если поле показано', function() {
            var stub = this.sinon.stub(this.mComposeState, 'set');
            this.sinon.stub(this.mComposeMessage, 'get').withArgs('.bcc').returns('');
            this.sinon.stub(this.mComposeState, 'isBccVisible').returns(true);

            this.view.onReplyAllLinkClick($.Event('click'));
            expect(stub).to.be.calledWithExactly('.isBccVisible', false, {
                'force': true
            });
        });
    });
});

