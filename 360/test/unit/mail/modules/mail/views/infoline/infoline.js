describe('Daria.vInfoLine', function() {
    beforeEach(function() {
        this.vInfoLine = ns.View.create('infoline', {
            current_folder: '1'
        });

        this.mMessagesChecked = ns.Model.get('messages-checked', {
            current_folder: '1'
        });

        this.sinon.stub(this.vInfoLine, 'getModel')
            .withArgs('messages-checked').returns(this.mMessagesChecked);

        this.vInfoLine.$node = $('<div />');
        this.vInfoLine._isVisible = false;
        this.vInfoLine.trigger('ns-view-htmlinit');
    });

    describe('#onDeselectAllClick', function() {
        beforeEach(function() {
            this.sinon.stub(this.mMessagesChecked, 'resetChecked');
        });

        it('Должен снять выделение со всех писем', function() {
            this.vInfoLine.onDeselectAllClick();
            expect(this.mMessagesChecked.resetChecked).to.have.callCount(1);
        });
    });

    describe('#toggleVisibility', function() {
        beforeEach(function() {
            this.canShowInfolineStub = this.sinon.stub(this.mMessagesChecked, 'canShowInfoline');
            this.sinon.stub($.fn, 'toggleClass');
        });

        it('Нужное количество писем не выбрано, вид должен остаться скрытым', function() {
            this.canShowInfolineStub.returns(false);
            this.vInfoLine.toggleVisibility();

            expect(this.vInfoLine._isVisible).to.be.equal(false);
        });

        it('Выбранно нужное количество писем вид, должен показаться', function() {
            this.canShowInfolineStub.returns(true);
            this.vInfoLine.toggleVisibility();

            expect(this.vInfoLine._isVisible).to.be.equal(true);
        });

        it('Если плашка показана, но письмо открыто, то плашка не должна показываться', function() {
            this.canShowInfolineStub.returns(true);
            this.vInfoLine._isMessageOpened = true;
            this.vInfoLine.toggleVisibility();

            expect(this.vInfoLine._isVisible).to.be.equal(false);
        });
        it('Если плашка показана, но письмо закрыто, то плашка показывается', function() {
            this.canShowInfolineStub.returns(true);
            this.vInfoLine._isMessageOpened = false;
            this.vInfoLine.toggleVisibility();

            expect(this.vInfoLine._isVisible).to.be.equal(true);
        });
    });

    describe('#_onMessagesCheckedChanged', function() {
        beforeEach(function() {
            this.sinon.stub(this.vInfoLine, 'toggleVisibility');
            this.sinon.stub(this.vInfoLine, 'onScroll');
            this.sinon.stub(this.vInfoLine, 'updateCounter');
        });

        it('Должен вызвать toggleVisibility', function() {
            this.vInfoLine._onMessagesCheckedChanged();
            expect(this.vInfoLine.toggleVisibility).to.have.callCount(1);
        });
    });
});
