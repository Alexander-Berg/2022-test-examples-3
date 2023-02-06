describe('Daria.vLabelsActions', function() {
    beforeEach(function() {
        this.view = ns.View.create('labels-actions');
        this.sinon.stubGetModel(this.view, 'messages-checked');

        this.sinon.stub(this.view, '_getCheckedModel').returns(this.mMessagesChecked);
        this.sinon.stub(this.view, 'closePopup');
    });

    describe('#_onLabelClick', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.view, [
                '_getDataFromClickedLabel'
            ]);

            this.view._logMetrika = this.sinon.stub();

            this.sinon.stubMethods(this.mMessagesChecked, [
                'runAction'
            ]);
        });

        it('Должен запускать действие на модели mMessagesChecked', function() {
            this.view._getDataFromClickedLabel.returns({ action: 'test', lid: 42});

            this.view._onLabelClick();

            expect(this.mMessagesChecked.runAction).to.be.calledWith('test', { lid: 42 });
        });

        it('Должен залогировать действие с меткой', function() {
            this.view._getDataFromClickedLabel.returns({ action: 'test', lid: 42});

            this.view._onLabelClick();

            expect(this.view._logMetrika).to.be.calledWith(42, 'test');
        });

        it('Должен закрыть попап', function() {
            this.view._getDataFromClickedLabel.returns({ action: 'test', lid: 42});

            this.view._onLabelClick();

            expect(this.view.closePopup).to.have.callCount(1);
        });
    });

    describe('#filterByMessagesChecked', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.view, [
                'getUnreadMessagesCount',
                '_filterByLabelsHash'
            ]);

            this.sinon.stubMethods(this.mMessagesChecked, [
                'getLabels',
                'getData',
                'getCount'
            ]);

            this.mMessagesChecked.getData.returns([]);
        });

        it('Запускает фильтрацию с хешом меток, полученным из mComposeMessage', function() {
            this.mMessagesChecked.getLabels.returns({ 'test': 42 });

            this.view.filterByMessagesChecked();

            expect(this.view._filterByLabelsHash).to.be.calledWith(
                { 'test': 42 }
            );
        });

        describe('Параметры фильтрации →', function() {
            beforeEach(function() {
                this.mMessagesChecked.getData.returns(_.range(10));
                this.mMessagesChecked.getCount.returns(10);
            });

            it('Скрывает кнопку `Сделать прочитанным`, если все письма прочитаны', function() {
                this.view.getUnreadMessagesCount.returns(0);

                this.view.filterByMessagesChecked();

                expect(this.view._filterByLabelsHash)
                    .to.be.calledWith(sinon.match.any, sinon.match.has('showReadLabel', false));
            });

            it('Не скрывает кнопку `Сделать прочитанным`, если хотя бы одно письмо непрочитано', function() {
                this.view.getUnreadMessagesCount.returns(1);

                this.view.filterByMessagesChecked();

                expect(this.view._filterByLabelsHash)
                    .to.be.calledWith(sinon.match.any, sinon.match.has('showReadLabel', true));
            });

            it('Скрывает кнопку `Сделать непрочитанным`, если все письма непрочитанные', function() {
                this.view.getUnreadMessagesCount.returns(10);

                this.view.filterByMessagesChecked();

                expect(this.view._filterByLabelsHash)
                    .to.be.calledWith(sinon.match.any, sinon.match.has('showUnreadLabel', false));
            });

            it('Не скрывает кнопку `Сделать непрочитанным`, если не все письма непрочитанные', function() {
                this.view.getUnreadMessagesCount.returns(9);

                this.view.filterByMessagesChecked();

                expect(this.view._filterByLabelsHash)
                    .to.be.calledWith(sinon.match.any, sinon.match.has('showUnreadLabel', true));
            });

            it('Передает количество писем в mMessagesChecked', function() {
                this.view.filterByMessagesChecked();

                expect(this.view._filterByLabelsHash)
                    .to.be.calledWith(sinon.match.any, sinon.match.has('messagesCount', 10));
            });
        });
    });
});
