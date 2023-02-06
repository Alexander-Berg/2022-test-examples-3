describe('Daria.vMessagesItemCheckboxWrap', function() {
    beforeEach(function() {
        this.view = ns.View.create('messages-item-checkbox-wrap', {ids: '12334'});
    });

    describe('#onCheckboxClick', function() {
        beforeEach(function() {
            this.clickEvent = {
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub()
            };
            this.shouldShowCheckboxInsideUserpicStub = this.sinon.stub(Daria.UISettings, 'shouldShowCheckboxInsideUserpic');
            this.isMessageCheckedStub = this.sinon.stub(this.view, 'isMessageChecked');
            this.sinon.stub(this.view, 'logClickMessageFromSearch');
        });

        it('должен предотвратить действие по умолчанию', function() {
            this.view.onCheckboxClick(this.clickEvent);

            expect(this.clickEvent.preventDefault).to.have.callCount(1);
        });

        it('должен прервать всплытие события', function() {
            this.view.onCheckboxClick(this.clickEvent);

            expect(this.clickEvent.stopPropagation).to.have.callCount(1);
        });

        it('не должен вызывать логгер поисковых кликов, если письмо не выделено', function() {
            this.isMessageCheckedStub.returns(false);

            this.view.onCheckboxClick(this.clickEvent);

            expect(this.view.logClickMessageFromSearch).to.have.callCount(0);
        });

        it('должен вызвать логгер поисковых кликов, если письмо выделено', function() {
            this.isMessageCheckedStub.returns(true);

            this.view.onCheckboxClick(this.clickEvent);

            expect(this.view.logClickMessageFromSearch).to.have.callCount(1);
        });

        it('логгер поисковых кликов должен быть вызван с параметром cbxavatar, если чекбокс внутри аватарки', function() {
            this.isMessageCheckedStub.returns(true);
            this.shouldShowCheckboxInsideUserpicStub.returns(true);

            this.view.onCheckboxClick(this.clickEvent);

            expect(this.view.logClickMessageFromSearch).to.be.calledWith('cbxavatar');
        });

        it('логгер поисковых кликов должен быть вызван с параметром checkbox, если чекбокс снаружи аватарки', function() {
            this.isMessageCheckedStub.returns(true);
            this.shouldShowCheckboxInsideUserpicStub.returns(false);

            this.view.onCheckboxClick(this.clickEvent);

            expect(this.view.logClickMessageFromSearch).to.be.calledWith('checkbox');
        });
    });
});
