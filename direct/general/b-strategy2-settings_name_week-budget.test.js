describe('b-strategy2-settings_name_week-budget', function() {

    var vm,
        currency = 'RUB',
        minAutobudget = u.currencies.getConst(currency, 'MIN_AUTOBUDGET'),
        maxAutobudget = u.currencies.getConst(currency, 'MAX_AUTOBUDGET'),
        minMaxClickBid = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_BID'),
        maxMaxClickBid = u.currencies.getConst(currency, 'MAX_AUTOBUDGET_BID'),
        goalId = '1';

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_name_week-budget', {
            maxClickBid: minMaxClickBid + (maxMaxClickBid - minMaxClickBid) / 2,
            weekBid: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            maxClickBidEnabled: true,
            goalId: goalId,
            currency: currency,
            title: 'title',
            metrika: {
                campaign_goals: [{
                    goal_id: goalId,
                    counter_status: 'Active',
                    goal_status: 'Active'
                }]
            },
            mode: 'cpa',
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

        it('Указан слишком большой недельный бюджет', function() {
            vm.set('weekBid', maxAutobudget + 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('weekBid');
            expect(validate.errors[0].rule).to.equal('gte');
        });

        it('Максимальная цена клика должна быть больше или равна ' + minMaxClickBid, function() {
            vm.set('maxClickBid', minMaxClickBid - 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('maxClickBid');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Указана ставка больше ' + maxMaxClickBid, function() {
            vm.set('maxClickBid', maxMaxClickBid + 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('maxClickBid');
            expect(validate.errors[0].rule).to.equal('gte');
        });

        it('Недельный бюджет должен быть больше максимальной цены клика', function() {
            vm.update({
                maxMaxClickBid: minAutobudget + 1,
                weekBid: minAutobudget
            });
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('weekBid');
            expect(validate.errors[0].rule).to.equal('anotherLte');
        });

    });

    it('Корректно формируются серверные настройки стратегии', function() {
        expect(vm.get('options')).to.deep.equal({
            sum: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            goal_id: goalId,
            bid: minMaxClickBid + (maxMaxClickBid - minMaxClickBid) / 2
        });
    });

});
