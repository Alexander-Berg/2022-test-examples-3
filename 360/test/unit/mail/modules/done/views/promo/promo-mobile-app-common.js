describe('Daria.vPromoMobileAppCommon', function() {
    beforeEach(function() {
        this.view = ns.View.create('promo-mobile-app-common', { promo: 'mobile-app-common' });
        this.mSettings = ns.Model.get('settings');
        this.mPhoneMobileAppState = ns.Model.get('promo-mobile-app-state');

        var stubModels = this.sinon.stub(ns.Model, 'get');
        stubModels.withArgs('promo-mobile-app-state').returns(this.mPhoneMobileAppState);

        this.defaultValues = {
            promoMobileAppName: 'mail-promo-test',
            metrikaData: null,
            PICTURE_PREFIX: 'MailAppLink',
            PICTURE_PREFIX_RU: 'MailAppLink3',

            SHOW_CLOSE_BUTTON: true,
            SHOW_FORM_FIELDS: true,
            SHOW_IN_POPUP: false,

            PROMO_TITLE: 'promo_title',
            PROMO_INFO: 'promo_info',
            PROMO_DESCRIPTION: 'promo_description',
            SEND_BUTTON_TEXT: 'send_button_text',
            SENDING_BUTTON_TEXT: 'sending_text',

            CUSTOM_CLASSES: 'test-class',

            PROMO_TYPES: {
                NEW_PROMO: 'new-promo',
                FORGOT_PROMO: 'forgot-promo',
                UPDATE_PROMO: 'update-promo'
            }
        };

        _.extend(this.view, this.defaultValues);
    });

    it('Должен правильно вернуть все константы из геттеров', function() {
        expect(this.view.canShowCloseButton()).to.be.eql(this.defaultValues.SHOW_CLOSE_BUTTON);
        expect(this.view.inPopup()).to.be.eql(this.defaultValues.SHOW_IN_POPUP);
        expect(this.view.canShowFormFields()).to.be.eql(this.defaultValues.SHOW_FORM_FIELDS);
        expect(this.view.getSendButtonText()).to.be.eql(this.defaultValues.SEND_BUTTON_TEXT);
        expect(this.view.getSendingButtonText()).to.be.eql(this.defaultValues.SENDING_BUTTON_TEXT);
        expect(this.view.getCustomClasses()).to.be.eql(this.defaultValues.CUSTOM_CLASSES);
    });

    describe('#getPicturePrefixName', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'isRuPromo');
        });

        it('Должен вернуть стандартный префикс, если у юзера не русский язык', function() {
            this.view.isRuPromo.returns(false);
            expect(this.view.getPicturePrefixName()).to.be.eql(this.defaultValues.PICTURE_PREFIX);
        });

        it('Должен верунть префикс для ru, если у юзера русский язык', function() {
            this.view.isRuPromo.returns(true);
            expect(this.view.getPicturePrefixName()).to.be.eql(this.defaultValues.PICTURE_PREFIX_RU);
        });
    });

    describe('#applyPhoneNumberFormat', function() {
        it('Должен \'7abc997484615\' переформатировать в \'79997484615\'', function() {
            expect(this.view.applyPhoneNumberFormat('7abc9997484615')).to.be.eql('79997484615');
        });
    });

    describe('#sendMobileAppLink', function() {
        beforeEach(function() {
            this.sinon.stub(ns, 'forcedRequest');
            this.sinon.stub(this.view, 'metrikaMarkAction');
            this.sinon.stub(this.view, '_getPhoneNumber').returns('123123123123');
            this.view.nbPhoneNumber = {
                focus: function() {

                }
            };
        });

        it('Не отправляет ссылку, если нет номера телефона', function() {
            this.view._getPhoneNumber.returns(false);

            this.view.sendMobileAppLink();

            expect(ns.forcedRequest).to.has.callCount(0);
        });

        it('Не отправляет ссылку, если номер телефона не корректный', function() {
            this.sinon.stub(Jane.FormValidation, 'checkPhoneNumber').returns(false);

            this.view.sendMobileAppLink();

            expect(ns.forcedRequest).to.has.callCount(0);
        });

        it('Не отправляет ссылку, если больше нельзя пытаться отправить ссылку', function() {
            this.sinon.stub(this.view, 'getState').returns({
                canTrySendAppLink: function() {
                    return false;
                },
                isInvalidPhoneNumberState: function() {
                    return false;
                }
            });

            this.view.sendMobileAppLink();

            expect(ns.forcedRequest).to.has.callCount(0);
        });

        it('Отправляет ссылку, с нужными параметрами', function() {
            this.sinon.stub(this.view, 'promoMobileAppName').value('test-promo-mobile-app');

            ns.forcedRequest.callsFake(() => vow.reject());

            this.view.sendMobileAppLink();

            expect(ns.forcedRequest).to.have.been.calledWith([ 'get-link-app' ], {
                app: 'test-promo-mobile-app',
                phone_full: '123123123123'
            });
        });
    });
});
