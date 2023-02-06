describe('b-strategy2-settings_name_week-bundle', function() {

    var vm,
        currency = 'RUB',
        minClicksBundle = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_CLICKS_BUNDLE'),
        maxClicksBundle = u.currencies.getConst(currency, 'MAX_AUTOBUDGET_CLICKS_BUNDLE'),
        minClickBid = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_AVG_PRICE'),
        maxClickBid = u.currencies.getConst(currency, 'MAX_AUTOBUDGET_BID');

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_name_week-bundle', {
            clickBid: minClickBid + (maxClickBid - minClickBid) / 2,
            clicksLimit: minClicksBundle + (maxClicksBundle - minClicksBundle) / 2,
            clickBidEnabled: true,
            clickBidType: 'max',
            currency: currency,
            title: 'title',
            where: 'search'
        });
    });

    afterEach(function() {
        vm.destruct();
    });

    describe('Проверка валидации', function() {
        var validate;

        afterEach(function() {
            validate = undefined;
        });

        it('Настройки стратегии валидные', function() {
            expect(vm.validate().valid).to.be.true;
        });

        it('Не указано количество кликов на неделю', function() {
            vm.set('clicksLimit', '');
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('clicksLimit');
            expect(validate.errors[0].rule).to.equal('required');
        });

        it('Количество кликов на неделю должно быть не менее ' + minClicksBundle, function() {
            vm.set('clicksLimit', minClicksBundle - 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('clicksLimit');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Указано слишком большое количество кликов', function() {
            vm.set('clicksLimit', maxClicksBundle + 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('clicksLimit');
            expect(validate.errors[0].rule).to.equal('maxClicks');
        });

        it('Указана ставка меньше минимальной цены клика', function() {
            vm.set('clickBid', minClickBid - 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('clickBid');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Указана ставка больше ' + maxClickBid, function() {
            vm.set('clickBid', maxClickBid + 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('clickBid');
            expect(validate.errors[0].rule).to.equal('maxBid');
        });

        it('Не указана цена клика', function() {
            vm.set('clickBid', '');
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('clickBid');
            expect(validate.errors[0].rule).to.equal('required');
        });

    });

    it('Корректно формируются серверные настройки стратегии', function() {
        expect(vm.get('options')).to.deep.equal({
            limit_clicks: minClicksBundle + (maxClicksBundle - minClicksBundle) / 2,
            bid: minClickBid + (maxClickBid - minClickBid) / 2,
            avg_bid: ''
        });
    });

});
