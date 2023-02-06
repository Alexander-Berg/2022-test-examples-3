describe('Daria.vComposeFieldBaseHotkeys', function() {
    describe('#_onKeydown', function() {
        beforeEach(function() {
            this.view = ns.View.create('compose-field-base-hotkeys');
            this.view.$node = this.$node;
            this.sinon.stubGetModel(this.view, ['compose-fsm']);
            this.sinon.spy(this.mComposeFsm, 'setState');

            this.mSettings = ns.Model.get('settings');
        });
        describe('Не должен отправлять письмо ->', function() {
            it('на сочетание META+ENTER на PC', function() {
                this.mSettings.setData(getModelMockByName('settings', 'settings_enable_hotkeys1'));
                this.sinon.stub(Modernizr, 'mac').value(false);
                this.view._onKeydown({ keyCode: Jane.Common.keyCode.ENTER, metaKey: true });
                this.sinon.stub(ns.page, 'current').value({ page: 'message' });
                expect(this.mComposeFsm.setState).to.have.callCount(0);
            });

            it('на сочетание CTRL+ENTER на MAC', function() {
                this.mSettings.setData(getModelMockByName('settings', 'settings_enable_hotkeys1'));
                this.sinon.stub(Modernizr, 'mac').value(true);
                this.view._onKeydown({ keyCode: Jane.Common.keyCode.ENTER, ctrlKey: true });
                this.sinon.stub(ns.page, 'current').value({ page: 'message' });
                expect(this.mComposeFsm.setState).to.have.callCount(0);
            });

            it('на какую-либо отличную от CTRL+ENTER клавишу', function() {
                this.view._onKeydown({ keyCode: Jane.Common.keyCode.TAB });
                expect(this.mComposeFsm.setState).to.have.callCount(0);
            });

            it('на ENTER без функциональной клавиши', function() {
                this.view._onKeydown({ keyCode: Jane.Common.keyCode.ENTER });
                expect(this.mComposeFsm.setState).to.have.callCount(0);
            });
            it('если отключены хоткеи', function() {
                this.sinon.stub(Modernizr, 'mac').value(true);
                var event = {
                    keyCode: Jane.Common.keyCode.ENTER,
                    metaKey: true,
                    preventDefault: this.sinon.stub()
                };
                this.view._onKeydown(event);
                this.sinon.stub(ns.page, 'current').value({ page: 'message' });
                this.mSettings.setData(getModelMockByName('settings', 'settings_enable_hotkeys2'));

                expect(this.mComposeFsm.setState).to.have.callCount(0);
            });
        });

        describe('Должен отправлять письмо ->', function() {
            it('если зажали META+ENTER на маке и находимся не в большом композе и включены хоткеи', function() {
                this.mSettings.setData(getModelMockByName('settings', 'settings_enable_hotkeys1'));
                this.sinon.stub(Modernizr, 'mac').value(true);
                var event = {
                    keyCode: Jane.Common.keyCode.ENTER,
                    metaKey: true,
                    preventDefault: this.sinon.stub()
                };
                this.view._onKeydown(event);
                this.sinon.stub(ns.page, 'current').value({ page: 'message' });

                expect(this.mComposeFsm.setState).to.be.calledWith('sending');
            });

            it('вызывается event.preventDefault', function() {
                this.mSettings.setData(getModelMockByName('settings', 'settings_enable_hotkeys1'));
                this.sinon.stub(Modernizr, 'mac').value(true);
                var event = {
                    keyCode: Jane.Common.keyCode.ENTER,
                    metaKey: true,
                    preventDefault: this.sinon.spy()
                };
                this.view._onKeydown(event);
                this.sinon.stub(ns.page, 'current').value({ page: 'message' });

                expect(event.preventDefault).to.have.callCount(1);
            });
            it('если зажали CTRL+ENTER на PC и находимся не в большом композе и включены хоткеи', function() {
                this.mSettings.setData(getModelMockByName('settings', 'settings_enable_hotkeys1'));
                this.sinon.stub(Modernizr, 'mac').value(false);
                var event = {
                    keyCode: Jane.Common.keyCode.ENTER,
                    ctrlKey: true,
                    preventDefault: this.sinon.spy()
                };
                this.view._onKeydown(event);
                this.sinon.stub(ns.page, 'current').value({ page: 'message' });
                expect(this.mComposeFsm.setState).to.be.calledWith('sending');
            });
        });
    });
});
