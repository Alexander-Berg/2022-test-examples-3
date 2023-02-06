describe('Daria.vDonePromoMobileApp', function() {
    beforeEach(function() {
        this.view = ns.View.create('done-promo-mobile-app', { promo: 'mobile-app-done' });
        this.mSettings = ns.Model.get('settings');
    });

    describe('#checkShowPromo', function() {
        beforeEach(function() {
            this.checkShowPromo = function() {
                return ns.View.info('done-promo-mobile-app').checkShowPromo();
            };

            this.parentView = ns.View.infoLite('promo-mobile-app-common');

            this.sinon.stub(this.mSettings, 'getSetting');
            this.sinon.stub(Daria, 'getAccountAgeInDays').returns(20);
            this.sinon.stub(Daria, 'is3pane').returns(false);

            this.mSettings.getSetting.withArgs('inline-wizard-mobile_close').returns(true);
            this.mSettings.getSetting.withArgs('promo-mobile_sms-send_date')
                .returns(Daria.now() - Jane.Date.MONTH * 4);
            this.mSettings.getSetting.withArgs('promo-mobile-done_show-date')
                .returns(Daria.now() - Jane.Date.MONTH);
            this.mSettings.getSetting.withArgs('inline-wizard-mobile_show-date')
                .returns(Daria.now() - Jane.Date.MONTH);
            this.mSettings.getSetting.withArgs('inline-wizard-mobile_cr-date')
                .returns(Daria.now() - Jane.Date.MONTH * 4);
        });

        it('Должен вернуть название view, если все условия выполняются', function() {
            expect(this.checkShowPromo()).to.be.eql('done-promo-mobile-app');
        });

        it('Должен не показываться, если после регистрации прошло не больше 10 дней', function() {
            Daria.getAccountAgeInDays.returns(9);
            expect(this.checkShowPromo()).to.be.eql(false);
        });

        it('Должен не показываться, если inline-wizard не закрыт', function() {
            this.mSettings.getSetting.withArgs('inline-wizard-mobile_close').returns(false);
            expect(this.checkShowPromo()).to.be.eql(false);
        });

        it('Должен не показываться, если после отправки смс прошло меньше 3 месяцев', function() {
            this.mSettings.getSetting.withArgs('promo-mobile_sms-send_date')
                .returns(Daria.now() - Jane.Date.MONTH * 2);
            expect(this.checkShowPromo()).to.be.eql(false);
        });

        it('Должен не показываться, если после последнего показа прошло меньше 14 дней', function() {
            this.mSettings.getSetting.withArgs('promo-mobile-done_show-date')
                .returns(Daria.now() - Jane.Date.WEEK);
            expect(this.checkShowPromo()).to.be.eql(false);
        });
    });
});
