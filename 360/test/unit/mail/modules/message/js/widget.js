describe('Daria.MessageWidget', function() {
    describe('#_checkShowClassificationConfirmationRequest', function() {
        beforeEach(function() {
            const params = { ids: '42' };

            this.mMessage = ns.Model.get('message', params);
            this.mMessageWidgetState = ns.Model.get('message-widget-state', params);
            this.mMessageBody = ns.Model.get('message-body', params);
            this.mAccountInformation = ns.Model.get('account-information');
            [
                this.mMessage, this.mMessageWidgetState, this.mMessageBody, this.mAccountInformation
            ].forEach((model) => model.setData({}));

            this.sinon.stub(this.mAccountInformation, 'isMyEmail').returns(false);

            this.check = (expected) => {
                expect(Daria.MessageWidget._checkShowClassificationConfirmationRequest(params)).to.equal(expected);
            };
        });

        describe('должен показать плашку', function() {
            it(
                'если в гет-параметрах передан message-widget=confirm-classification и нет флага на бэкенде',
                function() {
                    this.sinon.stub(Daria.urlParams, 'message-widget').value('confirm-classification');
                    this.mMessageWidgetState.set('.show', { someWidget: false });
                    this.mMessageBody.set('.classification', false);
                    this.mMessage.set('.type', [ 4 ]);

                    this.check(true);
                }
            );

            it('если есть какой-нибудь тип кроме 8, флаг от бэкенда и не показываются другие плашки', function() {
                this.mMessageWidgetState.set('.show', { someWidget: false });
                this.mMessageBody.set('.classification', true);
                this.mMessage.set('.type', [ 4 ]);
                this.check(true);
            });
        });

        describe('не должен показать плашку', function() {
            it('если письмо пришло с одного из алиасов получателя', function() {
                this.sinon.stub(this.mMessage, 'getFromEmail').returns('test@test.ru');
                this.mAccountInformation.isMyEmail.returns(true);

                this.check(false);
            });

            it('если есть тип 8', function() {
                this.mMessageBody.set('.classification', true);
                this.mMessage.set('.type', [ 8 ]);
                this.check(false);
            });

            it('если нет флага с бэкенда и гет-параметра', function() {
                this.mMessageBody.set('.classification', false);
                this.mMessage.set('.type', [ 4 ]);
                this.check(false);
            });

            it('если нет типов', function() {
                this.mMessageBody.set('.classification', true);
                this.mMessage.set('.type', []);
                this.check(false);
            });

            it('если показываются другие плашки', function() {
                this.mMessageWidgetState.set('.show', { someWidget: true });
                this.mMessageBody.set('.classification', true);
                this.mMessage.set('.type', [ 4 ]);
                this.check(false);
            });
        });
    });
});

