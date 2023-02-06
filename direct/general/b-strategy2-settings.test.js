describe('b-strategy2-settings', function() {

    var currency = 'RUB',
        name = 'autobudget_roi',
        modelData = {
            title: u.strategy.getTitleByName(name),
            currency: currency,
            metrika: {
                campaign_goals: [{
                    goal_id: 1,
                    counter_status: 'Active',
                    goal_status: 'Active'
                }]
            },
            bid: 100,
            sum: 330,
            roi_coef: 10,
            profitability: 40,
            reserve_return: 30,
            goal_id: 1,
            meaningfulGoals: []
        },
        ctx = {
            block: 'b-strategy2-settings',
            mods: { name: 'roi' },
            modelData: modelData
        },
        block,
        sandbox,
        vm,
        clock;

    BEM.MODEL.decl({ model: 'm-campaign' }, {});

    u.stubCurrencies();
    u['b-strategy2-settings'] = {
        getMetrikaWarningsText: function() {},
        getCommonMoreText: function() {},
        getHelpLinkUrl: function() {},
        isMetrikaDisabled: function() {
            var fn = u.getMetrikaWarning;

            return fn.isSinonProxy && fn() === 'no-metrika';
        }
    };

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
        BEM.MODEL.create({ name: 'm-campaign', id: 1 });
        clock = sinon.useFakeTimers();
        block = u
            .getDOMTree(ctx)
            .appendTo(document.body)
            .bem('b-strategy2-settings');
        vm = block.model;
    });

    afterEach(function() {
        block.destruct();
        clock.restore();
        sandbox.restore();
        vm = undefined;
    });

    it('Возвращаются верные настройки стратегии', function() {
        expect(block.getOptions()).to.deep.equal({
            roi_coef: 10,
            profitability: 40,
            reserve_return: 30,
            bid: 100,
            sum: 330,
            goal_id: 1
        });
    });

    it('Выбор цели блокируется', function() {
        var goalElem = block.elem('goal-select'),
            select = block.findBlockInside(goalElem, 'select');

        expect(select.getMod('disabled')).to.not.equal('yes');

        block.setMod(goalElem, 'disabled', 'yes');

        expect(select.getMod('disabled')).to.equal('yes');
    });

    describe('Проверка отображения ошибок', function() {

        it('Отображаются ошибки валидации', function() {
            vm.set('weekBid', 200);
            expect(block.validate()).to.not.ok;
            expect(block.elem('error').length).to.not.empty;
            expect(block.elem('error').text()).to.not.empty;
        });

        it('Для корректно заполненной формы ошибок валидации нет', function() {
            expect(block.validate()).to.be.ok;
            expect(block.elem('error').length).to.be.empty;
            expect(block.elem('error').text()).to.be.empty;
        });

        it('Очищаются ошибки валидации', function() {
            vm.set('weekBid', 200);
            block.validate();
            block.clearErrorMessages();
            expect(block.elem('error').length).to.be.empty;
            expect(block.elem('error').text()).to.be.empty;
        });

        it('Очищаются ошибки валидации при изменении модели', function() {
            vm.set('weekBid', 200);
            block.validate();
            sinon.stub(block, 'clearErrorMessages').returns(block);
            vm.set('weekBid', 300);
            clock.tick(500);
            expect(block.clearErrorMessages.called).to.be.true;
            block.clearErrorMessages.restore();
        });

        it('Ошибки валидации сортируются', function() {
            var errors;

            vm.set('weekBid', 200);
            vm.set('roi', -10);
            block.validate();
            errors = block.elem('errors').children();

            expect($(errors[0]).text().indexOf('Рентабельность инвестиций')).to.not.equal(-1);
            expect($(errors[1]).text().indexOf('Недельный бюджет')).to.not.equal(-1);
        });
    });
});
