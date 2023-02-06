describe('b-strategy2-settings_name_avg-click', function() {

    var vm,
        currency = 'RUB',
        minAutobudget = u.currencies.getConst(currency, 'MIN_AUTOBUDGET'),
        maxAutobudget = u.currencies.getConst(currency, 'MAX_AUTOBUDGET'),
        minClickBid = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_AVG_PRICE'),
        maxClickBid = u.currencies.getConst(currency, 'MAX_AUTOBUDGET_BID');

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_name_avg-click', {
            name: 'autobudget_avg_click',
            avgClickBid: minClickBid + (maxClickBid - minClickBid) / 2,
            weekBid: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            weekBidEnabled: true,
            maxClickBidEnabled: true,
            maxClickBid: maxClickBid,
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

        it('Недельный бюджет не может быть меньше ' + minAutobudget, function() {
            vm.set('weekBid', minAutobudget - 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('weekBid');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Недельный бюджет не может быть больше ' + maxAutobudget, function() {
            vm.set('weekBid', maxAutobudget + 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('weekBid');
            expect(validate.errors[0].rule).to.equal('gte');
        });

        it('Недельный бюджет должен быть больше средней цены клика', function() {
            vm.set('weekBid', minAutobudget);
            vm.set('avgClickBid', minAutobudget + 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('weekBid');
            expect(validate.errors[0].rule).to.equal('min');
        });

        it('Средняя цена за клик должна быть не менее ' + minClickBid, function() {
            vm.set('avgClickBid', minClickBid/2);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('avgClickBid');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Средняя цена за клик не может превышать ' + maxClickBid, function() {
            vm.set('avgClickBid', maxClickBid + 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('avgClickBid');
            expect(validate.errors[0].rule).to.equal('gte');
        });

        it('Не указано значение средней цены клика', function() {
            vm.set('avgClickBid', '');
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('avgClickBid');
            expect(validate.errors[0].rule).to.equal('required');
        });
    });

    it('Корректно формируются серверные настройки стратегии', function() {
        expect(vm.get('options')).to.deep.equal({
            avg_bid: minClickBid + (maxClickBid - minClickBid) / 2,
            sum: minAutobudget + (maxAutobudget - minAutobudget) / 2
        });
    });

});
