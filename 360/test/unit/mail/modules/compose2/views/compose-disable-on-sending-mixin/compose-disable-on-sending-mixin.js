describe('Daria.vComposeDisableOnSendingMixin', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-disable-on-sending-mixin');
        this.view.$node = $('<div />');
        this.view.node = this.view.$node[0];

        this.mComposeFsm = ns.Model.get('compose-fsm');
        this.sinon.stub(this.view, 'getModel').withArgs('compose-fsm').returns(this.mComposeFsm);
    });

    describe('#disableOnSendingInit', function() {
        describe('Проверка зависимостей →', function() {
            beforeEach(function() {
                this.sinon.spy(this.view, 'disableOnSendingInit');
            });

            it('Должен бросить исключение, если модели mComposeFsm нет в зависимостях', function() {
                this.view.getModel.withArgs('compose-fsm').returns(undefined);

                try { this.view.disableOnSendingInit(); } catch(e) {}

                expect(this.view.disableOnSendingInit).to.throw();
            });
        });
    });

    describe('#disableOnSendingStart', function() {
        describe('Привязка событий →', function() {
            beforeEach(function() {
                this.sinon.stub(this.mComposeFsm, 'on');
                this.view.disableOnSendingStart();
            });

            it('Должен подписаться на событие изменение состояния mComposeFsm', function() {
                expect(this.mComposeFsm.on).to.be.calledWith('# *', this.view.onToggleControl);
            });

            it('Должен подписаться на событие перехода mComposeFsm', function() {
                expect(this.mComposeFsm.on).to.be.calledWith('* > *', this.view.onToggleControl);
            });
        });
    });

    describe('#disableOnSendingStop', function() {
        describe('Отвязка событий →', function() {
            beforeEach(function() {
                this.sinon.stub(this.mComposeFsm, 'off');
                this.view.disableOnSendingStop();
            });

            it('Должен отписаться от события изменение состояния mComposeFsm', function() {
                expect(this.mComposeFsm.off).to.be.calledWith('# *', this.view.onToggleControl);
            });

            it('Должен отписаться от события перехода mComposeFsm', function() {
                expect(this.mComposeFsm.off).to.be.calledWith('* > *', this.view.onToggleControl);
            });
        });
    });

    describe('#onToggleControl', function() {

        beforeEach(function() {
            this.view.disableOnSendingInit();
            this.view.disableOnSendingStart();

            this.stubToggleClass = this.sinon.stub(this.view.$node, 'toggleClass');

            this.sinon.stubMethods(this.mComposeFsm, ['inTransition']);
        });

        describe('Должен сделать контрол активным', function() {
            it('edit -> save', function() {
                this.mComposeFsm.setInitialState('edit');
                this.mComposeFsm.inTransition.returns(true);

                this.view.onToggleControl('edit > save', { from: 'edit', to: 'save' });

                expect(this.stubToggleClass).to.be.calledWith('is-disabled', false);
            });

            it('# save', function() {
                this.mComposeFsm.setInitialState('save');
                this.mComposeFsm.inTransition.returns(false);

                this.view.onToggleControl('# save', { from: 'edit', to: 'save' });

                expect(this.stubToggleClass).to.be.calledWith('is-disabled', false);
            });

            it('# edit', function() {
                this.mComposeFsm.setInitialState('edit');
                this.mComposeFsm.inTransition.returns(false);

                this.view.onToggleControl('# edit', { from: 'sending', to: 'edit' });

                expect(this.stubToggleClass).to.be.calledWith('is-disabled', false);
            });
        });

        describe('Должен сделать контрол неактивным', function() {
            it('edit > sending', function() {
                this.mComposeFsm.setInitialState('edit');
                this.mComposeFsm.inTransition.returns(true);

                this.view.onToggleControl('edit > sending', { from: 'edit', to: 'sending' });

                expect(this.stubToggleClass).to.be.calledWith('is-disabled', true);
            });

            it('# sending', function() {
                this.mComposeFsm.setInitialState('sending');
                this.mComposeFsm.inTransition.returns(false);

                this.view.onToggleControl('# sending', { from: 'edit', to: 'sending' });

                expect(this.stubToggleClass).to.be.calledWith('is-disabled', true);
            });

            it('sending > send', function() {
                this.mComposeFsm.set('.state', 'sending', { silent: true });
                this.mComposeFsm.inTransition.returns(true);

                this.view.onToggleControl('sending > send', { from: 'sending', to: 'send' });

                expect(this.stubToggleClass).to.be.calledWith('is-disabled', true);
            });

            it('# send', function() {
                this.mComposeFsm.set('.state', 'send', { silent: true });
                this.mComposeFsm.inTransition.returns(false);

                this.view.onToggleControl('# send', { from: 'sending', to: 'send' });

                expect(this.stubToggleClass).to.be.calledWith('is-disabled', true);
            });
        });
    });
});

