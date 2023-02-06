describe('b-badges-multiselect', function() {

    var block,
        sandbox,
        inputBlock,
        items = [
            { id: '1', title: 'Супер кампания' },
            { id: '2', title: 'Холодильник 2000' },
            { id: '3', title: 'Реклама будущего' },
            { id: '4', title: 'У меня много кампаний' },
            { id: '5', title: 'Директ – лучшая кампания' }
        ],
        initialValue = [
            { id: '3', title: 'Реклама будущего' },
            { id: '4', title: 'У меня много кампаний' }
        ],
        createBlock = function(params) {
            var blockTree = u.getDOMTree(params ?
                $.extend(params, { block: 'b-badges-multiselect' }) :
            {
                block: 'b-badges-multiselect',
                items: items,
                value: initialValue
            });

            block = BEM.DOM
                .init(blockTree.appendTo('body'))
                .bem('b-badges-multiselect');
            inputBlock = block._input;
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        sandbox.spy(BEM.blocks, 'i-multiselect-static-provider');
    });

    afterEach(function() {
        BEM.DOM.destruct(block.domElem);
        sandbox.clock.tick(0);
        sandbox.restore();
    });

    describe('initialize', function() {

        it('Должны быть предвыбраны два бейджа', function() {
            var values = [];

            createBlock();

            block.findElem('badge').each(function(n, elem) {
                values.push(block.elemParams($(elem)).id);
            });

            expect((initialValue.every(function(item) {
                return values.some(function(v) {
                    return v === item.id;
                });
            }))).to.be.equal(true);
            expect(values.length).to.be.equal(2);
        });

        it('Должны быть добавлены несуществующие элементы', function() {
            var wrongValue = [
                    { id: '3', title: 'Реклама будущего' },
                    { id: '4', title: 'У меня много кампаний' },
                    { id: '4', title: 'У меня много кампаний 2' }
                ],
                values = [];

            createBlock({
                items: items,
                value: wrongValue
            });

            block.findElem('badge').each(function(n, elem) {
                values.push(block.elemParams($(elem)).id);
            });

            expect((wrongValue.every(function(item) {
                return values.some(function(v) {
                    return v === item.id;
                });
            }))).to.be.equal(true);
            expect(values.length).to.be.equal(3);
        });

        it('Должен быть создан инстанс дата провайдера i-multiselect-static-provider', function() {
            createBlock();

            var call = BEM.blocks['i-multiselect-static-provider'].getCall(0);

            expect(call).not.to.be.null;
            expect(call.args[0]).to.equal(undefined);
            expect(call.args[1].items).to.deep.equal(items);
            expect(call.args[1].disabledItems).to.deep.equal(initialValue);
        });

        it('Должен быть создан инстанс кастомного провайдера с правильными параметрами', function() {
            var params = { test: 'test' };

            BEM.blocks['i-my-test-provider'] = sinon.spy();

            createBlock({
                provider: {
                    name: 'i-my-test-provider',
                    params: params
                }
            });

            expect(BEM.blocks['i-my-test-provider'].calledWith(undefined, params)).to.be.equal(true);
            delete BEM.blocks['i-my-test-provider'];
        });

    });


    describe('get-set', function() {

        beforeEach(function() {
            createBlock();
        });


        it('Должно быть установлено корректный набор бейджей', function() {

            // проверить бейджи
            // val
            // дизейбл
            // инейбл
            expect(block).not.to.be.null;
        });

    });

    describe('При сбросе значений', function() {
        var clearElement;

        beforeEach(function() {
            createBlock({
                hint: 'hint',
                items: items,
                value: initialValue
            });
            clearElement = block.findElem('clear-link');
        });

        describe('Поле ввода __input пустое', function() {

            beforeEach(function() {
                inputBlock.val('');
                clearElement.trigger('pointerclick');
            });

            it('Должен выставлять модификатор _empty_yes', function() {
                expect(block).to.haveMod('empty', 'yes');
            });

            it('Должен выставлять элементу input__hint модификатор _visibility_visible', function() {
                expect(inputBlock).to.haveMod('hint', 'visibility', 'visible');
            });

        });

        describe('Поле ввода __input заполнено', function() {

            beforeEach(function() {
                inputBlock.val('test');
                clearElement.trigger('pointerclick');
            });

            it('Должен очищать __input', function() {
                expect(inputBlock.val()).to.be.empty;
            });

            it('Должен выставлять модификатор _empty', function() {
                expect(block).to.haveMod('empty', '');
            });

            it('Должен выставлять элементу input__hint модификатор _visibility_visible', function() {
                expect(inputBlock).to.haveMod('hint', 'visibility', 'visible');
            });

        });

    });

    describe('Вызов метода val() со списком значений', function() {

        beforeEach(function() {
            sandbox.stub(BEM.blocks['i-multiselect-static-provider'].prototype, 'enableItems');
            createBlock();
        });

        it('Должен очищать __input', function() {
            inputBlock.val('test');
            block.val(items);

            expect(inputBlock.val()).to.be.empty;
        });

        it('Должен устанавливать у __input модификатор _focused_yes', function() {
            block.val(items);

            expect(inputBlock).to.haveMod('focused', 'yes');
        });

    });

});
