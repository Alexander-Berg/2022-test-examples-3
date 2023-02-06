describe('b-strategy2-settings_name_cpa', function() {
    var vm,
        currency = 'RUB',
        minAutobudget = u.currencies.getConst(currency, 'MIN_AUTOBUDGET'),
        maxAutobudget = u.currencies.getConst(currency, 'MAX_AUTOBUDGET'),
        minMaxClickBid = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_AVG_PRICE'),
        maxMaxClickBid = u.currencies.getConst(currency, 'MAX_PRICE'),
        minCPA = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_AVG_CPA'),
        maxCPA = u.currencies.getConst(currency, 'AUTOBUDGET_AVG_CPA_WARNING'),
        goalId = '1';

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_name_cpa', {
            maxClickBid: minMaxClickBid + (maxMaxClickBid - minMaxClickBid) / 2,
            weekBid: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            maxClickBidEnabled: true,
            weekBidEnabled: true,
            cpa: minCPA + 1,
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

        it('Перед настройкой стратегии укажите счетчик Метрики и задайте ключевые цели кампании', function() {
            sinon.stub(u, 'getMetrikaWarning').returns('no-metrika');
            validate = vm.validate();
            u.getMetrikaWarning.restore();

            expect(!!validate.valid).to.be.false;
            expect(validate.errors[0].text.value).to.equal('Перед настройкой стратегии укажите счетчик Метрики и задайте ключевые цели кампании');
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

        it('Максимальная цена клика должна быть больше или равна ' + minMaxClickBid, function() {
            vm.set('maxClickBid', minMaxClickBid - 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('maxClickBid');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Максимальная цена клика должна быть меньше или равна ' + maxMaxClickBid, function() {
            vm.set('maxClickBid', maxMaxClickBid + 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('maxClickBid');
            expect(validate.errors[0].rule).to.equal('gte');
        });

        it('Не указано значение средней цены конверсии при неуказанном недельном бюджете', function() {
            vm.set('cpa', '');
            vm.set('weekBidEnabled', false);
            vm.set('weekBid', undefined);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('cpa');
            expect(validate.errors[0].rule).to.equal('required');
        });

        it('Не указано значение средней цены конверсии при указанном недельном бюджете', function() {
            vm.set('cpa', '');
            validate = vm.validate();

            expect(!!validate.valid).to.be.true;
        });

        it('Средняя цена конверсии должна быть больше или равна ' + minCPA , function() {
            vm.set('cpa', minCPA - 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('cpa');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Средняя цена конверсии не может быть больше ' + maxCPA , function() {
            vm.set('cpa', maxCPA + 1);

            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('cpa');
            expect(validate.errors[0].rule).to.equal('gte');
        });

        it('Недельный бюджет должен быть больше средней цены конверсии', function() {
            vm.update({
                cpa: maxCPA - 1,
                weekBid: maxCPA - 2
            });
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('cpa');
            expect(validate.errors[0].rule).to.equal('max');
        });

        it('Недельный бюджет должен быть больше максимальной ставки', function() {
            vm.update({
                maxClickBid: maxMaxClickBid - 1,
                weekBid: maxMaxClickBid - 2
            });
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('maxClickBid');
            expect(validate.errors[0].rule).to.equal('max');
        });

    });

    it('Корректно формируются серверные настройки стратегии', function() {
        expect(vm.get('options')).to.deep.equal({
            avg_cpa: minCPA + 1,
            sum: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            goal_id: goalId,
            bid: minMaxClickBid + (maxMaxClickBid - minMaxClickBid) / 2,
            pay_for_conversion: 0
        });
    });

});
