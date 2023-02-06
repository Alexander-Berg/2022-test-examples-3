describe('b-strategy2-settings_name_cpi', function() {
    var vm,
        currency = 'RUB',
        minAutobudget = u.currencies.getConst(currency, 'MIN_AUTOBUDGET'),
        maxAutobudget = u.currencies.getConst(currency, 'MAX_AUTOBUDGET'),
        minMaxClickBid = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_AVG_PRICE'),
        maxMaxClickBid = u.currencies.getConst(currency, 'MAX_PRICE'),
        minCPI = u.currencies.getConst(currency, 'MIN_AUTOBUDGET_AVG_CPA');

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_name_cpi', {
            maxClickBid: minMaxClickBid + (maxMaxClickBid - minMaxClickBid) / 2,
            weekBid: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            maxClickBidEnabled: true,
            weekBidEnabled: true,
            cpi: minCPI + 1,
            currency: currency,
            title: 'title',
            where: 'search',
            goalId: '4',
            rmpCounters: {
                allow_autobudget_avg_cpi: 1
            }
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

        it('Недельный бюджет должен быть больше средней цены установки приложения', function() {
            vm.update({
                cpi: minAutobudget + 1,
                weekBid: minAutobudget
            });
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('weekBid');
            expect(validate.errors[0].rule).to.equal('min');
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

        it('Не указано значение средней цены установки приложения', function() {
            vm.update({
                cpi: '',
                weekBidEnabled: false,
                weekBid: undefined
            });
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('cpi');
            expect(validate.errors[0].rule).to.equal('required');
        });

        it('Средняя цена установки приложения должна быть больше или равна ' + minCPI , function() {
            vm.set('cpi', minCPI - 1);
            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errorFields[0]).to.equal('cpi');
            expect(validate.errors[0].rule).to.equal('lte');
        });

        it('Невозможно подключить среднюю цену установки приложения', function() {
            vm.update({
                rmpCounters: {
                    allow_autobudget_avg_cpi: 0
                }
            });

            validate = vm.validate();

            expect(!!validate.valid).to.be.false;
            expect(validate.errors[0].text.value).to.equal('Невозможно подключить среднюю цену установки приложения');
        });

    });

    it('Корректно формируются серверные настройки стратегии', function() {
        expect(vm.get('options')).to.deep.equal({
            avg_cpi: minCPI + 1,
            sum: minAutobudget + (maxAutobudget - minAutobudget) / 2,
            bid: minMaxClickBid + (maxMaxClickBid - minMaxClickBid) / 2,
            goal_id: '4',
            pay_for_conversion: 0
        });
    });

});
