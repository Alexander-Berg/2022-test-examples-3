describe('Daria.vMopsConfirmation', function() {
    beforeEach(function() {
        this.vMopsConfirmation = ns.View.create('mops-confirmation');
        this.mMopsConfirmation = ns.Model.get('mops-confirmation');
        this.mMessagesChecked = ns.Model.get('messages-checked');

        this.sinon.stubGetModel(this.vMopsConfirmation, [
            this.mMopsConfirmation,
            this.mMessagesChecked
        ]);
    });

    describe('#clearMops', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Statusline, 'hide');
            this.sinon.stub(this.mMopsConfirmation, 'setMops');
            this.sinon.stub(this.vMopsConfirmation, 'unbindEvents');
        });

        it('должен скрыть статуслайн', function() {
            this.vMopsConfirmation.clearMops();

            expect(Daria.Statusline.hide).to.have.calledWith('confirm_mops');
        });

        it('должен затереть данные в модели', function() {
            this.vMopsConfirmation.clearMops();

            expect(this.mMopsConfirmation.setMops).to.have.calledWith({});
        });

        it('должен единожды вызвать "unbindEvents"', function() {
            this.vMopsConfirmation.clearMops();

            expect(this.vMopsConfirmation.unbindEvents).to.have.callCount(1);
        });
    });

    describe('#onCancel', function() {
        it('должен единожды вызвать "clearMops"', function() {
            this.sinon.stub(this.vMopsConfirmation, 'clearMops');
            this.vMopsConfirmation.onCancel();

            expect(this.vMopsConfirmation.clearMops).to.have.callCount(1);
        });
    });

    describe('#onConfirm', function() {
        beforeEach(function() {
            this.sinon.stub(this.vMopsConfirmation, 'clearMops');
            this.sinon.stub(this.mMopsConfirmation, 'getMops').returns({});
        });

        it('должен единожды вызвать "clearMops"', function() {
            this.vMopsConfirmation.onConfirm();

            expect(this.vMopsConfirmation.clearMops).to.have.callCount(1);
        });

        it('должен единожды вызвать callback из модельки', function() {
            this.mMopsConfirmation.getMops.returns({
                callback: function() {
                    console.log('callback');
                }
            });

            var mops = this.mMopsConfirmation.getMops();
            this.sinon.stub(mops, 'callback');
            this.vMopsConfirmation.onConfirm();

            expect(mops.callback).to.have.callCount(1);
        });
    });

    describe('#onMopsConfirmationChanged', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Statusline, 'hide');
            this.sinon.stub(Daria.Statusline, 'show');
            this.sinon.stub(this.vMopsConfirmation, 'bindEvents');
            this.sinon.stub(this.mMopsConfirmation, 'getMops').returns({
                action: 'remove'
            });
        });

        it('должен скрыть статуслайн', function() {
            this.vMopsConfirmation.onMopsConfirmationChanged();

            expect(Daria.Statusline.hide).to.have.calledWith('confirm_mops');
        });

        it('не должен показывать статуслайн если нет action', function() {
            this.mMopsConfirmation.getMops.returns({});
            this.vMopsConfirmation.onMopsConfirmationChanged();

            expect(Daria.Statusline.show).to.have.callCount(0);
        });

        it('должен показывать статуслайн если есть action', function() {
            this.vMopsConfirmation.onMopsConfirmationChanged();

            expect(Daria.Statusline.show).to.have.callCount(1);
        });

        it('не должен вызывать "bindEvents" если нет action', function() {
            this.mMopsConfirmation.getMops.returns({});
            this.vMopsConfirmation.onMopsConfirmationChanged();

            expect(this.vMopsConfirmation.bindEvents).to.have.callCount(0);
        });

        it('должен вызывать "bindEvents" если есть action', function() {
            this.vMopsConfirmation.onMopsConfirmationChanged();

            expect(this.vMopsConfirmation.bindEvents).to.have.callCount(1);
        });
    });

    describe('#onMessagesCheckedChanged', function() {
        beforeEach(function() {
            this.sinon.stub(this.vMopsConfirmation, 'clearMops');
            this.sinon.stub(this.mMessagesChecked, 'getCount').returns(0);
            this.sinon.stub(this.mMopsConfirmation, 'getMops').returns({
                action: 'remove'
            });
        });

        it('должен вызвать "clearMops" если есть action и нет выделенных писем', function() {
            this.vMopsConfirmation.onMessagesCheckedChanged();

            expect(this.vMopsConfirmation.clearMops).to.have.callCount(1);
        });

        it('не должен вызвать "clearMops" если нет action', function() {
            this.mMopsConfirmation.getMops.returns({});
            this.vMopsConfirmation.onMessagesCheckedChanged();

            expect(this.vMopsConfirmation.clearMops).to.have.callCount(0);
        });

        it('не должен вызвать "clearMops" если есть выделенные письма', function() {
            this.mMessagesChecked.getCount.returns(1);
            this.vMopsConfirmation.onMessagesCheckedChanged();

            expect(this.vMopsConfirmation.clearMops).to.have.callCount(0);
        });
    });
});
