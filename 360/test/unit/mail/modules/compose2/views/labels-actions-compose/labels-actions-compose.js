describe('Daria.vLabelsActionsCompose', function() {
    beforeEach(function() {
        this.view = ns.View.create('labels-actions-compose');
        this.$node = $('<div>');
        this.view.$node = this.$node;
        this.view.node = this.$node[0];

        this.sinon.stubGetModel(this.view, ['compose-message']);
    });

    describe('#onShow', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'filterByComposeMessage');
        });

        it('Должен вызвать фильтрацию меток', function() {
            this.view.onShow();

            expect(this.view.filterByComposeMessage).to.have.callCount(1);
        });
    });

    describe('#filterByComposeMessage', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'getLabelsHash');
            this.sinon.stub(this.view, '_filterByLabelsHash');
        });

        it('Запускает фильтрацию с хешом меток, полученным из mComposeMessage', function() {
            this.mComposeMessage.getLabelsHash.returns({ 'test': 42 });

            this.view.filterByComposeMessage();

            expect(this.view._filterByLabelsHash).to.be.calledWith(
                { 'test': 42 },
                {
                    showReadLabel: false,
                    showUnreadLabel: false,
                    messagesCount: 1
                }
            );
        });
    });

    describe('#_onLabelClick', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.view, [
                '_getDataFromClickedLabel'
            ]);

            this.sinon.stubMethods(this.mComposeMessage, [
                'addLid',
                'removeLid'
            ]);

            this.sinon.stub(this.view, 'nbPopup').value({ close: this.closeStub = this.sinon.stub()});
        });

        it('Должен вызвать добавление метки на модели mComposeMessage', function() {
            this.view._getDataFromClickedLabel.returns({ action: 'label', lid: 1});

            this.view._onLabelClick();

            expect(this.mComposeMessage.addLid).to.be.calledWith(1);
        });

        it('Должен вызвать удаление метки из модели mComposeMessage', function() {
            this.view._getDataFromClickedLabel.returns({ action: 'unlabel', lid: 1});

            this.view._onLabelClick();

            expect(this.mComposeMessage.removeLid).to.be.calledWith(1);
        });

        it('Должен закрыть попап', function() {
            this.view._getDataFromClickedLabel.returns({ action: 'test', lid: 1});

            this.view._onLabelClick();

            expect(this.closeStub).to.have.callCount(1);
        });
    });
});
