describe('Daria.vComposeFieldBaseFocus', function() {
    beforeEach(function() {
        this.vComposeFieldBaseFocus = ns.View.create('compose-field-base-focus');
        this.vComposeFieldBaseFocus.$node = $('<div />');
        this.vComposeFieldBaseFocus.node = this.vComposeFieldBaseFocus.$node[0];

        this.mComposeState = ns.Model.get('compose-state');
        this.mComposeState.setData({});

        this.sinon.stub(this.mComposeState, 'setFocusField');

        this.sinon.stub(this.vComposeFieldBaseFocus, 'getModel').withArgs('compose-state').returns(this.mComposeState);

        this.sinon.stub(ns.page.current, 'page').value('compose2');
    });

    describe('#onFocus', function() {

        it('должен вызвать метод установки фокуса', function() {
            var stub = this.sinon.stub(this.vComposeFieldBaseFocus, 'toggleFocus');

            this.vComposeFieldBaseFocus.onFocus();

            expect(stub).to.be.calledWith(true);
        });
    });

    describe('#onBlur', function() {

        it('должен вызвать метод снятия фокуса', function() {
            var stub = this.sinon.stub(this.vComposeFieldBaseFocus, 'toggleFocus');

            this.vComposeFieldBaseFocus.onBlur();

            expect(stub).to.be.calledWith(false);
        });
    });

    describe('#toggleFocus', function() {

        it('должен установить класс-признак наличия фокуса на ноду вида', function() {
            this.vComposeFieldBaseFocus.toggleFocus(true);
            expect(this.vComposeFieldBaseFocus.$node.hasClass(this.vComposeFieldBaseFocus.CLASS_FOCUSED)).to.be.ok;
        });

        it('должен снять класс-признак наличия фокуса на ноду вида', function() {
            this.vComposeFieldBaseFocus.toggleFocus(false);
            expect(this.vComposeFieldBaseFocus.$node.hasClass(this.vComposeFieldBaseFocus.CLASS_FOCUSED)).to.not.ok;
        });

        it('Должен установить имя фокусного поля название текущего поля в модель mComposeState, если фокус ставится в вид', function() {
            this.sinon.stub(Object.getPrototypeOf(this.vComposeFieldBaseFocus), 'FIELD_NAME').value('42');

            this.vComposeFieldBaseFocus.toggleFocus(true);

            expect(this.mComposeState.setFocusField).to.be.calledWithExactly('42', { silent: true });
        });

        it('Должен установить имя фокусного поля в null в модель mComposeState, если фокус снимается из вид', function() {
            this.sinon.stub(Object.getPrototypeOf(this.vComposeFieldBaseFocus), 'FIELD_NAME').value('42');

            this.vComposeFieldBaseFocus.toggleFocus(false);

            expect(this.mComposeState.setFocusField).to.be.calledWithExactly(null, { silent: true });
        });
    });

    describe('#focus', function() {

        it('должен выставить фокус на поле ввода', function() {
            this.vComposeFieldBaseFocus.$node.html('<input class="js-compose-field" />');
            this.sinon.stub(this.vComposeFieldBaseFocus.$node, 'find').returns({ 'focus': function() {} });
            var stub = this.sinon.stub(this.vComposeFieldBaseFocus.$node.find('.js-compose-field'), 'focus');

            this.vComposeFieldBaseFocus.focus();

            expect(stub).to.have.callCount(1);
        });
    });
});

