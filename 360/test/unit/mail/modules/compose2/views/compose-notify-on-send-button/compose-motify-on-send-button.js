describe('Daria.vComposeNotifyOnSendButton', function() {

    beforeEach(function() {
        this.sinon.stub(window, 'clearTimeout');

        this.view = ns.View.create('compose-notify-on-send-button');

        this.sinon.stub(this.view, 'initNbs');
        this.sinon.stub(this.view, 'destroyNbs');

        // Попап для попапа сообщения
        this.view.nbs = {
            checkbox: {
                isChecked: this.sinon.stub().returns(false),
                check: this.sinon.stub(),
                uncheck: this.sinon.stub()
            },
            popup: {
                open: this.sinon.stub(),
                close: this.sinon.stub(),
                isOpen: this.sinon.stub().returns(true)
            }
        };

        // Таймер скрытия попапа сообщения
        this.view._popupTimeout = 123;

        return this.view.update();
    });

    describe('#onChanged', function() {

        beforeEach(function() {
            this.sinon.stub(this.view, 'setCheckboxState');
            this.sinon.stub(this.view, 'showBubble');

            this.mComposeMessageGet = this.sinon.stub().withArgs('.notify_on_send');
            this.sinon.stub(this.view, 'getModel').withArgs('compose-message').returns({
                'get': this.mComposeMessageGet
            });
        });

        describe('mComposeMessage.notify_on_send выставлено в yes', function() {

            beforeEach(function() {
                this.mComposeMessageGet.returns('yes');
                this.view.onChangedNotifyOnSend();
            });

            it('должен показать попап о включении нотификации, если ', function() {
                expect(this.view.showBubble).to.have.callCount(1);
            });

            it('должен выбрать чекбокс', function() {
                expect(this.view.setCheckboxState).to.be.calledWithExactly('checkbox', true);
            });

        });


        describe('mComposeMessage.notify_on_send выставлено в null', function() {

            beforeEach(function() {
                this.mComposeMessageGet.returns(null);
                this.view.onChangedNotifyOnSend();
            });

            it('должен сбросить таймер скрытия попапа', function() {
                expect(window.clearTimeout).to.be.calledWithExactly(this.view._popupTimeout);
            });

            it('должен отменить чекбокс', function() {
                expect(this.view.setCheckboxState).to.be.calledWithExactly('checkbox', false);
            });

        });



    });

    describe('#onChecked', function() {

        it('должен выставить настройку нотификации о получении', function() {
            var mComposeMessage = this.view.getModel('compose-message');
            this.view.onChecked();

            expect(mComposeMessage.get('.notify_on_send')).to.be.equal('yes');
        });

    });

    describe('#onUnchecked', function() {

        it('должен сбросить настройку нотификации о получении', function() {
            var mComposeMessage = this.view.getModel('compose-message');
            this.view.onUnchecked();

            expect(mComposeMessage.get('.notify_on_send')).to.be.equal(null);
        });

    });

    describe('#onClosePopupClick', function() {

        it('должен закрыть попап о включении нотификации', function() {
            this.sinon.stub(this.view, 'closeBubble');
            this.view.onClosePopupClick();

            expect(this.view.closeBubble).to.have.callCount(1);
        });

    });

    describe('#closeBubble', function() {

        it('должен сбросить таймер скрытия попапа', function() {
            this.view.closeBubble();

            expect(window.clearTimeout).to.be.calledWithExactly(this.view._popupTimeout);
        });

        it('должен заркрыть попап о включении нотификации', function() {
            this.view.closeBubble();

            expect(this.view.nbs.popup.close).to.have.callCount(1);
        });

    });

    describe('#showBubble', function() {

        beforeEach(function() {
            this.clock = this.sinon.useFakeTimers();
            this.sinon.stub(this.view.super_, 'showBubble').callsFake(function() {
                return {
                    then: function(callback) {
                        callback();
                    }
                };
            });
            this.sinon.stub(this.view, 'closeBubble');
        });

        afterEach(function() {
           this.clock.restore();
        });

        it('должен показать попап о включении нотификации', function() {
            this.view.showBubble();

            expect(this.view.super_.showBubble).to.have.callCount(1);
        });

        it('должен закрыть попап по истечении 3-х секунд', function() {
            this.view.showBubble();
            this.clock.tick(Daria.timify({seconds: 3}));

            expect(this.view.closeBubble).to.have.callCount(1);
        });

    });

});
