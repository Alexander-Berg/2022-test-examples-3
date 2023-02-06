describe('Daria.vToolbarButtonForward', function() {
    beforeEach(function() {
        this.vToolbarButtonForward = ns.View.create('toolbar-button-forward');
        this.mMessagesChecked = ns.Model.get('messages-checked', {
            current_folder: '1'
        });
        this.sinon.stub(this.vToolbarButtonForward, 'getModel')
            .withArgs('messages-checked')
            .returns(this.mMessagesChecked);
    });

    describe('#enabled', function() {
        beforeEach(function() {
            this.sinon.stub(this.mMessagesChecked, 'getCount');
        });

        it('должен вернуть false если не выбрано ни одного письма', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.isEnabled = this.vToolbarButtonForward.enabled();

            expect(this.isEnabled).to.be.equal(false);
        });

        it(
            'должен вернуть false если выбрано больше 30 писем',
            function() {
                this.mMessagesChecked.getCount.returns(Daria.Constants.MAX_MESSAGES_TO_FORWARD + 1);
                this.isEnabled = this.vToolbarButtonForward.enabled();

                expect(this.isEnabled).to.be.equal(false);
            }
        );

        it(
            'должен вернуть true если выбрано от 1 до 29 письма',
            function() {
                this.mMessagesChecked.getCount.returns(Daria.Constants.MAX_MESSAGES_TO_FORWARD - 1);
                this.isEnabled = this.vToolbarButtonForward.enabled();

                expect(this.isEnabled).to.be.equal(true);
            }
        );
    });
});
