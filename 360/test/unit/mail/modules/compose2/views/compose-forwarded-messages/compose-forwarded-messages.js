describe('Daria.vComposeForwardedMessages', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-forwarded-messages');
        this.$node = $('<div><span class="js-message-checkbox"></span></div>');
        _.extend(this.view, {
            node: this.$node[0],
            $node: this.$node
        });
    });

    describe('#onHtmlInit', function() {
        beforeEach(function() {
            this.sinon.stub(nb, 'init');
            this.blockObj = {
                'on': this.sinon.stub()
            };
            this.sinon.stub(nb, 'block').returns(this.blockObj);
        });

        it('Инициализирует наноблоки', function() {
            this.view.onHtmlInit();

            expect(nb.init).to.be.calledWith(this.view.node);
        });

        it('Подписывается на события nb-changed чекбоксов', function() {
            this.view.onHtmlInit();

            expect(this.blockObj.on).to.be.calledWithExactly('nb-changed', this.view.onCheckboxChanged);
        });
    });

    describe('#onHtmlDestroy', function() {
        beforeEach(function() {
            this.sinon.stub(nb, 'destroy');
        });

        it('Уничтожает наноблоки', function() {
            this.view.onHtmlDestroy();

            expect(nb.destroy).to.be.calledWith(this.view.node);
        });
    });

    describe('#onCheckboxChanged', function() {
        beforeEach(function() {
            this.nbBlock = {
                isChecked: this.sinon.stub(),
                getValue: this.sinon.stub().returns('42')
            };
            this.mComposeForwardedMessages = ns.Model.get('compose-forwarded-messages');
            this.checkStub = this.sinon.stub(this.mComposeForwardedMessages, 'checkMessage');
            this.uncheckStub = this.sinon.stub(this.mComposeForwardedMessages, 'uncheckMessage');
            this.sinon.stub(this.view, 'getModel').withArgs('compose-forwarded-messages')
                .returns(this.mComposeForwardedMessages);
        });

        it('Добавляет mid в список выбранных в модели mComposeForwardedMessages', function() {
            this.nbBlock.isChecked.returns(true);

            this.view.onCheckboxChanged('nb-changed', this.nbBlock);

            expect(this.checkStub).to.be.calledWith('42');
        });

        it('Удаляет mid из списка выбранных в модели mComposeForwardedMessages', function() {
            this.nbBlock.isChecked.returns(false);

            this.view.onCheckboxChanged('nb-changed', this.nbBlock);

            expect(this.uncheckStub).to.be.calledWith('42');
        });
    });
});
