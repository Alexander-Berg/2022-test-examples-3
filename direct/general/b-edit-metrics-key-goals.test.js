describe('b-edit-metrics-key-goals', function() {
    var goals = {
            '1': {
                id: '1',
                domain: 'www.ya.ru',
                name: 'Ооооооооченььььььь ддддддлииииннннаааааааааааяяяяяяяя Цель 1',
                status: 'Active',
                isDefault: true
            },
            '2': {
                id: '2',
                domain: 'www.ya.ru',
                name: 'Цель 2',
                status: 'Active'
            },
            '3': {
                id: '3',
                domain: 'www.ya.ru',
                name: 'Цель 3',
                status: 'Active'
            },
            '4': {
                id: '4',
                domain: 'www.ya.ru',
                name: 'Цель 4',
                status: 'Active'
            },
            '5': {
                id: '5',
                domain: 'www.ya.ru',
                name: 'Цель 5',
                status: 'Active'
            },
            '6': {
                id: '6',
                domain: 'www.ya.ru',
                name: 'Цель 6',
                status: 'Active'
            },
            '7': {
                id: '7',
                domain: 'www.ya.ru',
                name: 'Цель 7',
                status: 'Active'
            },
            '8': {
                id: '8',
                domain: 'www.ya.ru',
                name: 'Цель 8',
                status: 'Active'
            },
            '9': {
                id: '9',
                domain: 'www.ya.ru',
                name: 'Цель 9',
                status: 'Active'
            },
            '10': {
                id: '10',
                domain: 'www.ya.ru',
                name: 'Цель 10',
                status: 'Active'
            },
            '11': {
                id: '11',
                domain: 'www.ya.ru',
                name: 'Цель 11',
                status: 'Active'
            },
            '12': {
                id: '12',
                domain: 'www.ya.ru',
                name: 'Цель 12',
                status: 'Active'
            }
        },
        createSandbox = function() {
            return sinon.sandbox.create({
                useFakeTimers: true
            });
        },
        createBlock = function(options) {
            return u.createBlock(
                u._.extend({ block: 'b-edit-metrics-key-goals' }, {
                    goals: goals,
                    value: [],
                    currency: 'RUB'
                }, options || {}),
                { inject: true }
            );
        },
        destructBlock = function(block) {
            block.destruct();
        };

    beforeEach(function() {
        u.stubCurrencies();
    });

    afterEach(function() {
        u.restoreCurrencies();
    });

    it('Цели не заданы -> отображен 1 элемент для выбора цели', function() {
        var block = createBlock();

        expect(block.elem('item').length).to.equal(1);

        destructBlock(block);
    });

    describe('Добавление цели', function() {
        it('Клик на кнопку добавить -> появляется новый элемент задания цели', function() {
            var block = createBlock(),
                itemsLength = block.findElem('item').length;

            block.elem('add').trigger('click');

            expect(block.findElem('item').length).to.equal(itemsLength + 1);

            destructBlock(block);
        });
    });

    describe('Удаление цели', function() {
        it('Клик на кнопку удалить -> удаляется элемент задания цели', function() {
            var block = createBlock({
                    value: [{ goalId: '1' }, { goalId: '2' }]
                }),
                itemsLength = block.findElem('item').length;

            block.findBlockOn('item','b-edit-metrics-key-goals-item').findBlockOn('delete', 'button').trigger('click');

            expect(block.findElem('item').length).to.equal(itemsLength - 1);

            destructBlock(block);
        });

        it('Всегда -> индексы корректно пересчитываются', function() {
            var block = createBlock({
                value: [{ goalId: '1' }, { goalId: '2' }, { goalId: '3' }]
            });

            block.elem('delete').eq(0).trigger('click');

            block.elem('item-index').each(function(index, indexElem) {
                expect($(indexElem).text()).to.equal((index + 1) + '.');
            });

            destructBlock(block);
        });
    });

    describe('Метод getValue', function() {
        it('Цели не заданы -> метод getValue возвращает корректное значение', function() {
            var block = createBlock();

            expect(block.getValue()).to.deep.equal([{ goalId: '', price: undefined }]);
        });

        it('Цели заданы -> метод getValue возвращает корректное значение', function() {
            var block = createBlock({
                value: [{ goalId: '1', price: 10 }, { goalId: '2' }, { goalId: '3' }]
            });

            expect(block.getValue()).to.deep.equal([
                { goalId: '1', price: 10 },
                { goalId: '2', price: undefined },
                { goalId: '3', price: undefined }
            ]);

            destructBlock(block);
        });
    });

    describe('Метод validate', function() {
        it('Цели не заданы -> возвращает false', function() {
            var block = createBlock();

            expect(block.validate()).to.be.false;

            destructBlock(block);
        });

        it('Все цели и ценности заданы -> возвращает true', function() {
            var block = createBlock({
                value: [{ goalId: '1', price: 120 }, { goalId: '2', price: 100 }, { goalId: '3', price: 12 }]
            });

            expect(block.validate()).to.be.true;

            destructBlock(block);
        });

        it('Цена конверсии не задана -> возвращает true', function() {
            var block = createBlock({
                value: [{ goalId: '1' }]
            });

            expect(block.validate()).to.be.true;

            destructBlock(block);
        });

        it('Цена конверсии задана и меньше минимального значения -> возвращает false', function() {
            var block = createBlock({
                    value: [{ goalId: '1', price: '0.1' }]
                }),
                sandbox = createSandbox();

            sandbox.stub(u.currencies, 'getConst').withArgs('RUB', 'MIN_AUTOBUDGET_BID').returns(0.3);

            expect(block.validate()).to.be.false;

            destructBlock(block);
            sandbox.restore();
        });

        it('Цена конверсии задана и больше максимального значения -> возвращает false', function() {
            var block = createBlock({
                    value: [{ goalId: '1', price: '2500' }]
                }),
                sandbox = createSandbox();

            sandbox.stub(u.currencies, 'getConst').withArgs('RUB', 'MAX_AUTOBUDGET_BID').returns(2000);

            expect(block.validate()).to.be.false;

            destructBlock(block);
            sandbox.restore();
        });

        it('Цена конверсии задана меньше максимального значения и больше минимального -> возвращает true', function() {
            var block = createBlock({
                    value: [{ goalId: '1', price: '1000' }]
                }),
                sandbox = createSandbox(),
                getConstStub = sandbox.stub(u.currencies, 'getConst');

            getConstStub.withArgs('RUB', 'MIN_AUTOBUDGET_BID').returns(0.3);
            getConstStub.withArgs('RUB', 'MAX_AUTOBUDGET').returns(2000);

            expect(block.validate()).to.be.true;

            destructBlock(block);
            sandbox.restore();
        });

        it('Цели заданы не полностью -> возвращает false', function() {
            var block = createBlock({
                value: [{ goalId: '1' }]
            });

            block.elem('add').trigger('click');

            expect(block.validate()).to.be.false;

            destructBlock(block);
        });
    });

    describe('События блока', function() {
        it('Добавление цели -> триггерит событие change', function() {
            var block = createBlock(),
                sandbox = createSandbox();

            sandbox.spy(block, 'trigger');

            block.elem('add').trigger('click');

            expect(block).to.triggerEvent('change');

            destructBlock(block);
            sandbox.restore();
        });

        it('Удаление цели -> триггерит событие change', function() {
            var block = createBlock({
                    value: [{ goalId: '1' }, { goalId: '2' }]
                }),
                sandbox = createSandbox();

            sandbox.spy(block, 'trigger');

            block.findBlockOn('item','b-edit-metrics-key-goals-item').findBlockOn('delete', 'button').trigger('click');

            expect(block).to.triggerEvent('change');

            destructBlock(block);

            sandbox.restore();
        });

        it('Изменение цели одного из элементов -> триггерит событие change', function() {
            var block = createBlock({
                    value: [{ goalId: '1' }, { goalId: '2' }]
                }),
                sandbox = createSandbox();

            sandbox.spy(block, 'trigger');

            block.findBlockInside('b-metrics-goal-selector').trigger('change', { value:  '5' });

            expect(block).to.triggerEvent('change');

            destructBlock(block);
            sandbox.restore();
        });

        it('Изменение цены конверсии одного из элементов -> триггерит событие change', function() {
            var block = createBlock({
                    value: [{ goalId: '1' }]
                }),
                sandbox = createSandbox();

            sandbox.spy(block, 'trigger');

            block.findBlockInside('input').trigger('change');

            expect(block).to.triggerEvent('change');

            destructBlock(block);
            sandbox.restore();
        });
    });
});
