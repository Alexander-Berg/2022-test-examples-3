describe('b-strategy2-settings_name_max-clicks', function() {
    var vm,
        currency = 'RUB',
        block,
        clock,
        modelData = {
            name: 'maximum_clicks',
            currency: currency,
            where: 'search',
            title: 'Ручное управление ставками',
            isDifferentPlaces: false,
            platform: 'all',
            innerName: 'default',
            options: {},
            metrika: {
                campaign_goals: [
                    {
                        goal_id: 1,
                        counter_status: 'Active',
                        goal_status: 'Active'
                    }
                ]
            },
            dayBudgetSettings: {
                currency: "RUB",
                isEnabled: true,
                isSet: false,
                maxDailyChangeCount: "3",
                showMode: "default",
                showOptionsHint: true,
                sum: 459,
                timesChangedToday: 1
            },
            savedName: 'default'
        }, ctx = {
            block: 'b-strategy2-settings',
            mods: { name: 'max-clicks' },
            modelData: modelData
        };


    beforeEach(function() {
        clock = sinon.useFakeTimers();
        block = u
            .getDOMTree(ctx)
            .appendTo(document.body)
            .bem('b-strategy2-settings');
        vm = block.model;
        block.setMod('platform', 'all');
    });

    afterEach(function() {
        delete vm;
        block.destruct();
        clock.restore();
        $('.b-strategy2-settings').detach();
    });

    describe('Взаимодействие контролов на странице', function() {
        describe('Расположение галочки дневного бюджета', function() {
            it ('Галочка дневного бюджета отчерчена', function() {
                expect(block.getMod(block.elem('day-budget-settings'), 'bordered')).to.equal('top')
            });
        });

        it('При раздельном управлении показывается предупреждение', function() {
            vm.set('isDifferentPlaces', true);
            expect(block.elem('strategy-hints').text()).to.equal(iget2(
                'b-strategy2-settings',
                'pri-sohranenii-strategii-ceny',
                'При сохранении стратегии цены в сетях будут проставлены в зависимости от максимальной цены на поиске. Изменить цены в сетях можно со страницы кампании.'
            ));
        });

        it('Без раздельного управления НЕ показывается предупреждение', function() {
            vm.set('isDifferentPlaces', false);
            expect(block.elem('strategy-hints').text()).to.be.empty;
        });
    });

    describe('Корректно формируются серверные настройки стратегии', function() {
        describe('При снятой галочке "Раздельное управления"', function() {
            beforeEach(function() {
                vm.set('isDifferentPlaces', false);
                vm.set('limitShowPos', false);
            });
            it ('Все галочки сняты', function() {
                var options = block.getOptions('all');

                expect(options).to.deep.equal({ name: 'default', options: {} });
            });
        });

        describe('При поставленной галочке "Раздельное управления"', function() {
            beforeEach(function() {
                vm.set('isDifferentPlaces', true);
                vm.set('limitShowPos', false);
            });

            it ('Все галочки сняты', function() {
                var options = block.getOptions('all');

                expect(options).to.deep.equal({ name: 'different_places', options: {
                    net: { name: 'maximum_coverage' },
                    search: { name: 'default' }
                } });
            });
        });
    });

    describe('Для сетевой стратегии', function() {
        beforeEach(function() {
            modelData = {
                currency: currency,
                where: 'search',
                title: 'Ручное управление ставками',
                isDifferentPlaces: false,
                platform: 'net',
                innerName: 'maximum_coverage',
                options: {},
                dayBudgetSettings: {
                    currency: "RUB",
                    isEnabled: true,
                    isSet: false,
                    maxDailyChangeCount: "3",
                    showMode: "default",
                    showOptionsHint: true,
                    sum: 459,
                    timesChangedToday: 1
                },
                savedName: 'maximum_coverage',
                metrika: {
                    campaign_goals: [
                        {
                            goal_id: 1,
                            counter_status: 'Active',
                            goal_status: 'Active'
                        }
                    ]
                }
            };
            ctx = {
                block: 'b-strategy2-settings',
                mods: { name: 'max-clicks' },
                modelData: modelData
            };
            block = u
                .getDOMTree(ctx)
                .appendTo(document.body)
                .bem('b-strategy2-settings');
            block.setMod('platform', 'net');
            vm = block.model;
        });

        it('Корректно формируются настройки стратегии', function() {
            var options = block.getOptions('net');

            expect(options).to.deep.equal({ name: 'maximum_coverage', options: {} });
        });

        it ('предупреждение НЕ отображается', function() {
            expect(block.elem('strategy-hints').text()).to.be.empty;
        });

        describe('Расположение галочки дневного бюджета', function() {

            it ('Галочка дневного бюджета отчерчена', function() {
                expect(block.getMod(block.elem('day-budget-settings'), 'bordered')).to.equal('top')
            });
        });
    });

    describe('Для поисковой стратегии', function() {
        beforeEach(function() {
            modelData = {
                currency: currency,
                where: 'search',
                title: 'Ручное управление ставками',
                isDifferentPlaces: false,
                platform: 'search',
                innerName: 'default',
                options: {},
                dayBudgetSettings: {
                    currency: "RUB",
                    isEnabled: true,
                    isSet: false,
                    maxDailyChangeCount: "3",
                    showMode: "default",
                    showOptionsHint: true,
                    sum: 459,
                    timesChangedToday: 1
                },
                savedName: 'default',
                metrika: {
                    campaign_goals: [
                        {
                            goal_id: 1,
                            counter_status: 'Active',
                            goal_status: 'Active'
                        }
                    ]
                }
            };
            ctx = {
                block: 'b-strategy2-settings',
                mods: { name: 'max-clicks' },
                modelData: modelData
            };
            block = u
                .getDOMTree(ctx)
                .appendTo(document.body)
                .bem('b-strategy2-settings');
            block.setMod('platform', 'search');
            vm = block.model;
        });

        it ('предупреждение НЕ отображается', function() {
            expect(block.elem('strategy-hints').text()).to.be.empty;
        });

        describe('Расположение галочки дневного бюджета', function() {
            it ('Галочка дневного бюджета отчерчена', function() {
                expect(block.getMod(block.elem('day-budget-settings'), 'bordered')).to.equal('top')
            });
        });
    })
});
