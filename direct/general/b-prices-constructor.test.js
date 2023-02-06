describe('b-prices-constructor', function() {
    var block,
        constructorModel,
        minPay = 30,
        currencyName = 'у.е.',
        currency = 'YND_FIXED',
        strategy = {},
        formType = 'common',
        savedData = {},
        strategiesHash = {
            'default': {
                name: '',
                search: { name: 'default' },
                net: { name: 'default' }
            },
            'diff-both': {
                name: 'different_places',
                search: { name: 'default' },
                net: { name: 'maximum_coverage' }
            },
            'diff-stop': {
                name: 'different_places',
                search: { name: 'stop' },
                net: { name: 'maximum_coverage' }
            }
        };

    function createBlock(data) {
        var tree = $(BEMHTML.apply({
            block: 'b-prices-constructor',
            modelParams: {
                name: 'm-prices-constructor',
                id: '1',
                parentName: 'm-campaign',
                parentId: '1'
            },
            strategy: strategiesHash[data.strategy || 'default'],
            savedData: data.savedData || {},
            formType: data.formType || formType,
            currencyName: data.currencyName || currencyName,
            currency: data.currency || currency,
            minPay: minPay,
            showBothPlatforms: data.showBothPlatforms || false,
            viewMode: data.viewMode || 'online'
        }));

        $('body').append(tree);

        block = BEM.DOM.init(tree).bem('b-prices-constructor');

        constructorModel = block.model;

        return block;
    }

    function clear() {
        block && BEM.DOM.destruct(block.domElem);

        constructorModel && constructorModel.destruct();
        minPay = 30;
        currencyName = 'у.е.';
        strategy = {};
        savedData = {};
    }

    afterEach(clear);


    describe('#initial обычная стратегия', function() {
        var defaultData = {
            platform: 'search',
            position_ctr_correction: '100',
            context_scope: '100',
            proc_search: 30,
            proc_context: 30,
            is_search_disabled: false,
            is_context_disabled: true,
            search_toggle: true,
            context_toggle: false,
            price_search: minPay,
            price_context: minPay
        };


        describe('Инициализация с пустыми данными, проверяем значения по умолчанию. ', function() {
            ['simple', 'wizard'].forEach(function(type) {
                describe('Тип конструктора - ' + type, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'default',
                            formType: 'common',
                            type: type
                        });
                    });

                    Object.keys(defaultData).forEach(function(field) {
                        var value = defaultData[field];

                        it(field + ' = ' + value, function() {
                            expect(block.model.get(field)).to.be.equal(value);
                        });
                    });
                })
            });
        });

        describe('#bugs Пытаемся установить context_scope = fixed в конструктор, где недоступна фиксированная цена', function() {
            it('context_scope установилось 100', function() {
                createBlock({
                    strategy: 'default',
                    formType: 'common',
                    type: 'wizard',
                    viewMode: 'offline',
                    savedData: { context_scope: 'fixed' }
                });

                expect(block.model.get('context_scope')).to.be.equal('100');
            })
        });

        describe('#bugs Конструктор с недоступным выбором платформы', function() {
            describe('Нет предустановленных данных', function() {
                beforeEach(function() {
                    createBlock({
                        strategy: 'default',
                        formType: 'common',
                        type: 'wizard',
                        savedData: {}
                    });
                });

                var values = {
                    platform: 'search',
                    is_search_disabled: false,
                    is_context_disabled: true
                };

                Object.keys(values).forEach(function(key) {
                    var val = values[key];

                    it(key + ' = ' + val, function() {
                        expect(block.model.get(key)).to.be.equal(val);
                    });
                });
            });

            describe('Сохранены некорректные значения', function() {
                beforeEach(function() {
                    createBlock({
                        strategy: 'default',
                        formType: 'common',
                        type: 'wizard',
                        savedData: {
                            platform: 'context',
                            is_search_disabled: true,
                            is_context_disabled: false
                        }
                    });
                });

                var values = {
                    platform: 'search',
                    is_search_disabled: false,
                    is_context_disabled: true
                };

                Object.keys(values).forEach(function(key) {
                    var val = values[key];

                    it(key + ' = ' + val, function() {
                        expect(block.model.get(key)).to.be.equal(val);
                    });
                });
            });

        });
    });


    describe('#initial - отдельное размещение, поиск не отключен, показ обеих платформ. ', function() {
        var defaultData = {
            position_ctr_correction: '100',
            context_scope: '100',
            proc_search: 30,
            proc_context: 30,
            price_search: minPay,
            price_context: minPay,
            search_toggle: true,
            context_toggle: true
        };

        describe('Инициализация с пустыми данными, проверяем значения по умолчанию. ', function() {
            ['simple', 'wizard'].forEach(function(type) {
                ['search', 'context'].forEach(function(formType) {
                    describe('Тип конструктора - ' + type, function() {
                        beforeEach(function() {
                            createBlock({
                                strategy: 'diff-both',
                                showBothPlatforms: true,
                                formType: formType,
                                type: type
                            });
                        });

                        Object.keys(defaultData).forEach(function(field) {
                            var value = defaultData[field];

                            it(field + ' = ' + value, function() {
                                expect(block.model.get(field)).to.be.equal(value);
                            });
                        });
                    })
                })
            });
        });

        describe('На входе - поиск отключен, контекст включен. ', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            type: 'wizard',
                            showBothPlatforms: true,
                            savedData: {
                                search_toggle: false,
                                context_toggle: true
                            }
                        });
                    });

                    it('На выходе - поиск задизейблен, контекст - нет', function() {
                        expect(block.model.get('is_search_disabled')).to.be.equal(true);
                        expect(block.model.get('is_context_disabled')).to.be.equal(false);
                    });
                })
            });
        });

        describe('На входе поиск включен, контекст отключен. ', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            showBothPlatforms: true,
                            type: 'wizard',
                            savedData: {
                                search_toggle: true,
                                context_toggle: false
                            }
                        });
                    });

                    it('На выходе поиск активен, контекст задизейблен', function() {
                        expect(block.model.get('is_search_disabled')).to.be.equal(false);
                        expect(block.model.get('is_context_disabled')).to.be.equal(true);
                    });
                })
            });
        });

        describe('На входе - поиск отключен, контекст отключен. ', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            showBothPlatforms: true,
                            type: 'wizard',
                            savedData: {
                                search_toggle: false,
                                context_toggle: false
                            }
                        });
                    });

                    it('На выходе поиск и контекст НЕ задизейблены и включены. ', function() {
                        expect(block.model.get('search_toggle')).to.be.equal(true);
                        expect(block.model.get('context_toggle')).to.be.equal(true);
                        expect(block.model.get('is_search_disabled')).to.be.equal(false);
                        expect(block.model.get('is_context_disabled')).to.be.equal(false);
                    });
                })
            });
        });

    });

    describe('#initial - отдельное размещение, поиск не отключен, показ только одной платформы, платформа по умолчанию поиск.', function() {
        var defaultData = {
            position_ctr_correction: '100',
            context_scope: '100',
            proc_search: 30,
            proc_context: 30,
            price_search: minPay,
            price_context: minPay,
            search_toggle: true,
            context_toggle: false
        };


        describe('Инициализация с пустыми данными, проверяем значения по умолчанию. ', function() {
            ['simple', 'wizard'].forEach(function(type) {
                ['search', 'context'].forEach(function(formType) {
                    describe('Тип конструктора - ' + type, function() {
                        beforeEach(function() {
                            createBlock({
                                strategy: 'diff-both',
                                formType: formType,
                                type: type
                            });
                        });

                        Object.keys(defaultData).forEach(function(field) {
                            var value = defaultData[field];

                            it(field + ' = ' + value, function() {
                                expect(block.model.get(field)).to.be.equal(value);
                            });
                        });
                    })
                })
            });
        });

        describe('На входе - поиск отключен, контекст включен. ', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            type: 'wizard',
                            savedData: {
                                search_toggle: false,
                                context_toggle: true
                            }
                        });
                    });

                    it('На выходе - поиск включен, контекст отключен', function() {
                        expect(block.model.get('search_toggle')).to.be.equal(true);
                        expect(block.model.get('context_toggle')).to.be.equal(false);
                    });
                })
            });
        });

        describe('На входе поиск включен, контекст отключен. ', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            type: 'wizard',
                            savedData: {
                                search_toggle: true,
                                context_toggle: false
                            }
                        });
                    });

                    it('На выходе - поиск включен, контекст отключен. ', function() {
                        expect(block.model.get('search_toggle')).to.be.equal(true);
                        expect(block.model.get('context_toggle')).to.be.equal(false);
                    });
                })
            });
        });

        describe('На входе - поиск отключен, контекст отключен. ', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            type: 'wizard',
                            savedData: {
                                search_toggle: false,
                                context_toggle: false
                            }
                        });
                    });

                    it('На выходе поиск включен, контекст отключен. ', function() {
                        expect(block.model.get('search_toggle')).to.be.equal(true);
                        expect(block.model.get('context_toggle')).to.be.equal(false);
                    });
                })
            });
        });

    });

    describe('#initial - отдельное размещение, поиск не отключен, показ только одной платформы, платформа - контекст. ', function() {
        var defaultData = {
            position_ctr_correction: '100',
            context_scope: '100',
            proc_search: 30,
            proc_context: 30,
            price_search: minPay,
            price_context: minPay,
            search_toggle: false,
            context_toggle: true
        };


        describe('инициализация с пустыми данными, проверяем значения по умолчанию. ', function() {
            ['simple', 'wizard'].forEach(function(type) {
                ['search', 'context'].forEach(function(formType) {
                    describe('Тип конструктора - ' + type, function() {
                        beforeEach(function() {
                            createBlock({
                                strategy: 'diff-both',
                                formType: formType,
                                type: type,
                                savedData: {
                                    platform: 'context'
                                }

                            });
                        });

                        Object.keys(defaultData).forEach(function(field) {
                            var value = defaultData[field];

                            it(field + ' = ' + value, function() {
                                expect(block.model.get(field)).to.be.equal(value);
                            });
                        });
                    })
                })
            });
        });

        describe('На входе - поиск отключен, контекст включен. ', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            type: 'wizard',
                            savedData: {
                                search_toggle: false,
                                platform: 'context',
                                context_toggle: true
                            }
                        });
                    });

                    it('На выходе - поиск отключен, контекст включен', function() {
                        expect(block.model.get('search_toggle')).to.be.equal(false);
                        expect(block.model.get('context_toggle')).to.be.equal(true);
                    });
                })
            });
        });

        describe('На входе поиск включен, контекст отключен', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            type: 'wizard',
                            savedData: {
                                search_toggle: true,
                                platform: 'context',
                                context_toggle: false
                            }
                        });
                    });

                    it('На выходе - контекст включен, поиск отключен', function() {
                        expect(block.model.get('search_toggle')).to.be.equal(false);
                        expect(block.model.get('context_toggle')).to.be.equal(true);
                    });
                })
            });
        });

        describe('На входе - поиск отключен, контекст отключен', function() {
            ['search', 'context'].forEach(function(formType) {
                describe('Тип конструктора - wizard, formType = ' + formType, function() {
                    beforeEach(function() {
                        createBlock({
                            strategy: 'diff-both',
                            formType: formType,
                            type: 'wizard',
                            savedData: {
                                search_toggle: false,
                                platform: 'context',
                                context_toggle: false
                            }
                        });
                    });

                    it('На выходе контекст включен, поиск отключен', function() {
                        expect(block.model.get('search_toggle')).to.be.equal(false);
                        expect(block.model.get('context_toggle')).to.be.equal(true);
                    });
                })
            });
        });

    });

    describe('#initial - отдельное размещение, поиск отключен, показ обеих платформ одновременно', function() {
        var defaultData = {
            position_ctr_correction: '100',
            context_scope: '100',
            proc_search: 30,
            proc_context: 30,
            search_toggle: false,
            context_toggle: true,
            price_search: minPay,
            price_context: minPay
        };

        describe('инициализация с пустыми данными, проверяем значения по умолчанию', function() {
            ['simple', 'wizard'].forEach(function(type) {
                ['context'].forEach(function(formType) {
                    describe('Тип конструктора - ' + type, function() {
                        beforeEach(function() {
                            createBlock({
                                strategy: 'diff-stop',
                                formType: formType,
                                type: type
                            });
                        });

                        Object.keys(defaultData).forEach(function(field) {
                            var value = defaultData[field];

                            it(field + ' = ' + value, function() {
                                expect(block.model.get(field)).to.be.equal(value);
                            });
                        });
                    })
                })
            });
        });
    });
});
