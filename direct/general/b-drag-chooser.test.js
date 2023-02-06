describe('b-drag-chooser', function() {
    var wrap = $('<div/>'),
        bemJson,
        bemBlock;

    /**
     * Создание инстанса блока
     * @param {Object} [extraParams] - доп. параметры которые будут переданы ctx
     * @param {String[]} [extraParams.value] - выбранные пункты
     * @param {String[]} [extraParams.positions] - порядок пунктов
     */
    function createBlock(extraParams) {
        extraParams = extraParams || {};
        bemJson = $.extend({
            block: 'b-drag-chooser',
            items: [
                {
                    text: 'first item',
                    value: 'first'
                },
                {
                    text: 'second item',
                    value: 'second'
                },
                {
                    text: 'third item',
                    value: 'third'
                },
                {
                    text: 'fourth item',
                    value: 'fourth'
                },
                {
                    text: 'fifth item',
                    value: 'fifth'
                }
            ]
        }, extraParams);

        bemBlock = BEM.DOM.init(u.getDOMTree(bemJson).appendTo(wrap)).bem('b-drag-chooser');
    }

    /**
     * Берем "пункт" для перестаскивания по индексу
     * @param index
     * @returns {*|jQuery|HTMLElement}
     */
    function getItemByIndex(index) {
        return $(bemBlock.findElem('item')[index]);
    }

    afterEach(function() {
        BEM.DOM.destruct(bemBlock.domElem);
    });

    describe('Проверяем получение value и positions которые были заданны во входном bemjson', function() {
        var initialValue = ['first', 'second'],
            initialPositions = ['second', 'fourth', 'fifth', 'first', 'third'];

        beforeEach(function() {
            createBlock({ value: initialValue, positions: initialPositions });
        });

        it('getValue должен вернуть выбранные пункты', function() {
            // в value мы задаем ['first', 'second']
            // но их расположение определяется параметром positions
            expect(bemBlock.getValue()).to.be.eql(['second', 'first']);
        });

        it('getPositions должен вернуть массив с порядком расположения пунктов', function() {
            expect(bemBlock.getPositions()).to.be.eql(initialPositions);
        });
    });

    describe('Проверяем получение value которое было заданно на клиенте', function() {
        beforeEach(function() {
            createBlock();
        });

        it('getValue должен вернуть выбранные пункты', function() {
            // Чекаем 1 и 3 чекбоксы
            bemBlock.findBlocksInside('checkbox')[0].setMod('checked', 'yes');
            bemBlock.findBlocksInside('checkbox')[2].setMod('checked', 'yes');

            // Сравниваем полученный результат с заведомоизвестным
            expect(bemBlock.getValue()).to.be.eql(['first', 'third']);
        });
    });

    describe('Проверяем получение positions после drag and drop', function() {
        beforeEach(function() {
            createBlock();
        });

        it('пятый пункт должен быть на месте третьего', function() {
            var fifth = getItemByIndex(4),  // получаем элемент для перетаскивания 5го пункта
                second = getItemByIndex(1); // получаем элемент для перетаскивания 2го пункта

            // Имитируем перетаскивание, меняя расположение пунктов
            fifth.insertAfter(second);

            // Проверяем какой порядок вернет getPositions()
            expect(bemBlock.getPositions()).to.be.eql(['first', 'second', 'fifth', 'third', 'fourth']);
        });
    });

});
