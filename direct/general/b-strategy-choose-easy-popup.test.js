describe.skip('b-strategy-choose-easy-popup', function() {
    var campModel,
        domTree,
        sandbox,
        block;

    function createCampModel(data) {
        data = data || {};

        campModel = BEM.MODEL.create({ name: 'm-easy-campaign', id: 'campaign' }, {
            currency: data.currency || 'YND_FIXED',
            isEdit: data.isEdit,
            cid: 'campaign',
            pseudo_currency_id: 'rub',
            strategy: 'distributed',
            banners_cnt: data.banners_cnt,
            rate: 30
        });
    }

    function createBlock(data) {
        domTree = u.getDOMTree({
            block: 'b-strategy-choose-easy-popup',
            js: {
                canChange: true
            },
            content:[
                {
                    elem: 'title-wrapper',
                    content: {
                        elem: 'title',
                        canChange: true,
                        strategyData: {
                            manual_autobudget_sum: 300
                        },
                        currencyName: 'руб.'
                    }
                },
                {
                    block: 'spin',
                    mix: [{ block: 'b-strategy-choose-easy-popup', elem: 'spin' }],
                    mods: { theme: 'gray-16' }
                },
                {
                    block: 'popup',
                    mods: { 'has-close': 'yes' },
                    js: {
                        directions: ['bottom']
                    },
                    mix: [{ block: 'b-strategy-choose-easy-popup', elem: 'popup' }],
                    content: [
                        { elem: 'tail' },
                        {
                            elem: 'content',
                            content: [
                                {
                                    block: 'b-strategy-choose-easy-popup',
                                    elem: 'popup-center',
                                    content: ''
                                },
                                {
                                    block: 'b-strategy-choose-easy-popup',
                                    elem: 'popup-buttons',
                                    content: [
                                        {
                                            block: 'button',
                                            mix: [{ block: 'b-strategy-choose-easy-popup', elem: 'accept-button' }],
                                            content: 'Сохранить'
                                        },
                                        {
                                            block: 'button',
                                            mix: [{ block: 'b-strategy-choose-easy-popup', elem: 'decline-button' }],
                                            content: 'Отмена'
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        });

        $('body').append(domTree);

        block = $(domTree).bem('b-strategy-choose-easy-popup');
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true, useFakeServer: true });
        sandbox.stub(BEM.blocks['i-easy-budget-forecast'], 'loadCampaignPhrases').callsFake(function() {
            return $.Deferred().promise();
        });

        createCampModel();
        createBlock();
    });

    afterEach(function() {
        BEM.MODEL.destruct(campModel);
        block.destruct();

        sandbox.restore();
    });

    function stubStrategyBlock(block) {
        sandbox.stub(block, '_strategyBlock', {
            isValid: function() { return true; },
            provideData: function() { return { manual_autobudget_sum: 4333 }; },
            fixStrategyData: function() {}
        });
    }


    ['accept-button', 'switcher-button'].forEach(function(elemName) {
        it('Клик по ' + elemName + ' должен вызывать установку модификатора loading', function() {
            stubStrategyBlock(block);

            block.elem(elemName).click();
            sandbox.clock.tick(5);

            expect(block).to.haveMod('loading', 'yes');
        });
    });

    ['accept-button', 'decline-button', 'switcher-button'].forEach(function(elemName) {
        it('При установленном модификаторе loading кнопка на ' + elemName + ' должна быть задисейблена', function() {
            block.setMod('loading', 'yes');
            sandbox.clock.tick(5);

            expect(block.findBlockOn(elemName, 'button').isDisabled()).to.be.true;
        });
    });

    ['cmd=ajaxSaveAutobudget', 'budget_strategy=distributed', 'cid=campaign'].forEach(function(string) {
        it('При клике на "Сохранить" должен уходить запрос на сервер c параметром ' + string, function() {
            stubStrategyBlock(block);

            block.elem('accept-button').click();
            sandbox.clock.tick(5);

            expect(sandbox.server.requests[0].requestBody).to.have.string(string);
        });
    });

    it('Если стратегия успешно сохранилась, то должен удалиться модификатор loading', function() {
        stubStrategyBlock(block);

        block.elem('accept-button').click();
        sandbox.clock.tick(5);

        sandbox.server.respondWith('POST', '/registered/main.pl', [200, { "Content-Type": "application/json" }, '{"success":1}']);
        sandbox.server.respond();

        expect(block).to.not.haveMod('loading');
    });

    it('Если стратегия успешно сохранилась, то на кнопке switcher-button выставляется текущее значение manual_autobudget_sum', function() {
        stubStrategyBlock(block);

        block.elem('accept-button').click();
        sandbox.clock.tick(5);
        sandbox.server.respondWith('POST', '/registered/main.pl', [200, { "Content-Type": "application/json" }, '{"success":1}']);
        sandbox.server.respond();
        sandbox.clock.tick(5);
        expect(block.findElem('manual-autobudget-sum').text()).to.equal('4 333');
    });

    it('Если при сохранении стратегии произошла ошибка, должно быть показано сообщение "При сохранении бюджета произошла ошибка"', function() {
        stubStrategyBlock(block);

        BEM.blocks['b-confirm'].alert = sandbox.spy(function() {});
        block.elem('accept-button').click();
        sandbox.clock.tick(5);

        sandbox.server.respondWith('POST', '/registered/main.pl', [404, {}, '']);
        sandbox.server.respond();
        expect(BEM.blocks['b-confirm'].alert.calledWith('При сохранении бюджета произошла ошибка')).to.be.true;
    });

    it('Если при сохранении стратегии произошла ошибка, должен удалиться модификатор loading', function() {
        stubStrategyBlock(block);

        BEM.blocks['b-confirm'].alert = sandbox.spy(function() {});
        block.elem('accept-button').click();
        sandbox.clock.tick(5);

        sandbox.server.respondWith('POST', '/registered/main.pl', [404, {}, '']);
        sandbox.server.respond();

        expect(block).to.not.haveMod('loading');
    });

});
