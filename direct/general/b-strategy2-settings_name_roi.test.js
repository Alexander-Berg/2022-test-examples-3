describe('b-strategy2-settings_name_roi', function() {

    var vm,
        currency = 'RUB',
        minAutobudget = u.currencies.getConst(currency, 'MIN_AUTOBUDGET'),
        maxAutobudget = u.currencies.getConst(currency, 'MAX_AUTOBUDGET'),
        minMaxClickBid = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_AVG_PRICE'),
        maxMaxClickBid = u.currencies.getConst(currency, 'MAX_PRICE'),
        goalId = '1';

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_name_roi', {
            maxClickBid: minMaxClickBid + (maxMaxClickBid - minMaxClickBid) / 2,
            weekBid: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            maxClickBidEnabled: true,
            profitabilityEnabled: true,
            weekBidEnabled: true,
            roi: 10,
            profitability: 40,
            reserveReturn: 30,
            goalId: '1',
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

        it('Процент себестоимости не должен быть меньше 0', function() {
            vm.set('profitability', -1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('profitability');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Процент себестоимости не должен быть больше 100', function() {
            vm.set('profitability', 101);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('profitability');
            expect(validate.errors[0].rule).to.equal('gte');
        });

        it('Не указано значение рентабельности инвестиций', function() {
            vm.set('roi', '');
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('roi');
            expect(validate.errors[0].rule).to.equal('required');
        });

        it('Рентабельность инвестиций должна быть больше -1', function() {
            vm.set('roi', -1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('roi');
            expect(validate.errors[0].rule).to.equal('min');
        });

    });

    it('Корректно формируются серверные настройки стратегии', function() {
        expect(vm.get('options')).to.deep.equal({
            roi_coef: 10,
            profitability: 40,
            reserve_return: 30,
            sum: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            goal_id: goalId,
            bid: minMaxClickBid + (maxMaxClickBid - minMaxClickBid) / 2
        });
    });

});
