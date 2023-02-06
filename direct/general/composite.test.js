describe('composite', function() {

    var sandbox,
        composite,
        /**
         * Создаёт и возвращает блок списка
         * @param {Object} params набор параметров для блока
         * @returns {BEM.DOM}
         */
        createBlock = function(params) {
            var template = {
                block: 'composite',
                id: params.id,
                mix: params.mix,
                layout: params.layout,
                itemView: params.itemView,
                idAttr: params.idAttr,
                collection: params.collection
            };

            return u.createBlock(template, {
                inject: true,
                hidden: false
            });
        };

    before(function() {

        BEMHTML.compile(function() {
            block('b-some')(
                elem('item').content()(function() {

                    if (this.ctx.flags) {
                        return this.ctx.flags.join(',') + this.ctx.itemData.name;
                    } else if (this.ctx.flagsObj) {
                        return Object.keys(this.ctx.flagsObj).join(',') + this.ctx.itemData.name;
                    } else {
                        return this.ctx.itemData.name
                    }
                }),

                elem('wrap').content()(function() {
                    return [
                        { block: 'composite', elem: 'items' },
                        { elem: 'status-text' }
                    ]
                })
            )
        });
    });

    beforeEach(function() {
        sandbox = sinon.sandbox.create();

    });

    afterEach(function() {
        sandbox.restore();
    });

    /*
    * отрисовка
    *
    * */
    describe('Использование', function() {

        afterEach(function() {
            composite.destruct();
        });

        describe('с ctx.layout', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    layout: { block: 'b-some', elem: 'wrap' },
                    collection: [
                        { id: 1, name: 'item1' },
                        { id: 2, name: 'item2' },
                        { id: 3, name: 'item3' }
                    ]
                });
            });

            it('шаблонизирует содержимое ctx.layout', function() {
                expect(composite.domElem.children(':first').attr('class')).to.be.equal('b-some__wrap');
            });

            it('элемент items находится внутри ctx.layout', function() {
                expect(composite.findElem($('.b-some__wrap'), 'items').length).to.be.equal(1);
            });
        });

        describe('с itemView с данными на контексте', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item', flags: [1,2,3] },
                    layout: { block: 'b-some', elem: 'wrap' },
                    collection: [{ id: 1, name: 'item1' }]
                });
            });

            it('сохраняет инфо на контексте', function() {
                expect(composite.findElem('item').first().text()).to.be.equal('1,2,3item1')
            });
        });

        describe('с itemView с модификаторами', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item', elemMods: { state: 'initial' } },
                    layout: { block: 'b-some', elem: 'wrap' },
                    collection: [{ id: 1, name: 'item1' }]
                });
            });

            it('сохраняет модификаторы itemView при шаблонизации', function() {
                expect(/b-some__item_state_initial/.test(composite.elemInstance('item').domElem.attr('class'))).to.be.true;
            });
        });

        describe('с itemView с массивом миксов', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: {
                        block: 'b-some',
                        elem: 'item',
                        mix: [{ block: 'b-other', elem: 'item' }]
                    },
                    layout: { block: 'b-some', elem: 'wrap' },
                    collection: [{ id: 1, name: 'item1' }]
                });
            });

            it('сохраняет модификаторы itemView при шаблонизации', function() {
                expect(/b-other__item/.test(composite.elemInstance('item').domElem.attr('class'))).to.be.true;
            });
        });

        describe('с itemView с объектом миксов', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: {
                        block: 'b-some',
                        elem: 'item',
                        mix: { block: 'b-other', elem: 'item' }
                    },
                    layout: { block: 'b-some', elem: 'wrap' },
                    collection: [{ id: 1, name: 'item1' }]
                });
            });

            it('сохраняет модификаторы itemView при шаблонизации', function() {
                expect(/b-other__item/.test(composite.elemInstance('item').domElem.attr('class'))).to.be.true;
            });
        });

        describe('с collection', function() {
            var collection = [
                { id: 1, name: 'item1' },
                { id: 2, name: 'item2' },
                { id: 3, name: 'item3' }
            ];

            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    collection: collection
                });
            });

            it('шаблонизирует ' + collection.length + 'элемента списка', function() {
                expect(composite).to.haveElems('item', collection.length);
            });
        });

        describe('без collection', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' }
                });
            });

            it('выводит пустой список', function() {
                expect(composite).to.haveElems('item', 0);
            });
        });

        describe('с id', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    id: 'my-list',
                    collection: [{ id: 1, name: 'item1' }]
                });
            });

            it('модификатор pid элемента items равен заданному ID', function() {
                expect(composite.elemInstance('items')).haveMod('pid', 'my-list')
            });
        });

        describe('с attrId', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    idAttr: 'elementId',
                    collection: [{ elementId: 'item123', name: 'item1' }]
                });
            });

            it('модификатор item-id элемента строки равен значению поля elementId', function() {
                expect(/item123/.test(composite.elemInstance('item').getMod('item-id'))).to.be.true;
            });
        });

        describe('без id элемента коллекции', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    collection: [{ name: 'item1' }]
                });
            });

            it('модификатор item-id элемента строки будет сгенерирован', function() {
                expect(/composite-item/.test(composite.elemInstance('item').getMod('item-id'))).to.be.true;
            });
        });

        describe('без id', function() {
            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    collection: [{ id: 1, name: 'item1' }]
                });
            });

            it('модификатор pid элемента items будет сгенерирован', function() {
                expect(/c-/.test(composite.elemInstance('items').getMod('pid'))).to.be.true;
            });
        });
    });

    /*
     * проверка добавления
     * */
    describe('Работа с ctx itemView', function() {

        describe('с полем типа array', function() {
            var addData = { id: 1, name: 'newitem7' };

            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item', flags: [1,2,3] },
                    collection: []
                });
            });

            it('добавляет в исходный массив новые элементы { itemOptions: { flags: [...] }', function() {
                composite.add(addData, { itemOptions: { flags: [4,5] } });

                expect(composite.findElem('item').last().text()).to.be.equal('1,2,3,4,5' + addData.name)
            });
        });

        describe('с полем типа object', function() {

            describe('с непустым объектом расширения', function() {
                var addData = { id: 7, name: 'newitem7' };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item', flagsObj: { one: 1 } },
                        collection: []
                    });
                });

                it('добавляет в исходный объект новые поля { itemOptions: { flagsObj: {...} }', function() {
                    composite.add(addData, { itemOptions: { flagsObj: { two: 22 } } });

                    expect(composite.findElem('item').last().text()).to.be.equal('one,two' + addData.name)
                });
            });

            describe('с пустым объектом расширения', function() {
                var addData = { id: 7, name: 'newitem7' };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item', flagsObj: { one: 1 } },
                        collection: []
                    });
                });

                it('шаблонизирует элементы с корректными данными, расширяя исходные доп. данные', function() {
                    composite.add(addData, { itemOptions: null });

                    expect(composite.findElem('item').last().text()).to.be.equal('one' + addData.name)
                });
            });
        });
    });

    /*
    * проверка добавления
    * */
    describe('метод добавления строк (add)', function() {
        var initialData = [
            { id: 1, name: 'item1' },
            { id: 2, name: 'item2' }
        ];

        afterEach(function() {
            composite.destruct();
        });

        describe('вызывает событие', function() {
            var spy,
                addData = { id: 2, name: 'new1' };

            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    collection: initialData
                });

                spy = sandbox.spy(composite, 'trigger');
            });

            ['before:add', 'add'].forEach(function(event, i) {
                it(event + ' с данными обновления', function() {
                    composite.add(addData);

                    expect(spy[!i ? 'firstCall' : 'secondCall'].calledWith(event, {
                        items: [addData]
                    })).to.true;
                });
            })
        });

        describe('при использовании в виде', function() {

            describe('add(newItem)', function() {
                var addData = { id: 1, name: 'newitem1' };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: []
                    });
                });

                it('добавит элемент в список', function() {
                    composite.add(addData);

                    expect(composite).to.haveElems('item', 1);
                });

                it('шаблонизирует элементы с корректными данными', function() {
                    composite.add(addData);

                    expect(composite.findElem('item').last().text()).to.be.equal(addData.name)
                });
            });

            describe('add(newItem, { at: ... })', function() {

                describe('если в списке есть элементы', function() {
                    var addData = { id: 1, name: 'newitem1' },
                        addParams = { at: 1 };

                    beforeEach(function() {
                        composite = createBlock({
                            itemView: { block: 'b-some', elem: 'item' },
                            collection: initialData
                        });
                    });

                    it('вставит элемент в список по индексу ' + addParams.at, function() {
                        composite.add(addData, addParams);

                        expect(composite).to.haveElems('item', initialData.length + 1);
                    });

                    it('шаблонизирует элементы с корректными данными', function() {
                        composite.add(addData, addParams);

                        expect(composite.findElem('item').eq(addParams.at).text()).to.be.equal(addData.name)
                    });
                });

                describe('если список пуст', function() {
                    var addData = { id: 1, name: 'newitem1' },
                        addParams = { at: 1 };

                    beforeEach(function() {
                        composite = createBlock({
                            itemView: { block: 'b-some', elem: 'item' },
                            collection: []
                        });
                    });

                    it('добавит элемент в конец списка', function() {
                        composite.add(addData, addParams);

                        expect(composite).to.haveElems('item', 1);
                    });

                    it('шаблонизирует элементы с корректными данными', function() {
                        composite.add(addData, addParams);

                        expect(composite.findElem('item').last().text()).to.be.equal(addData.name)
                    });
                })
            });

            describe('add([newItem1, newitem2])', function() {
                var addData = [
                    { id: 7, name: 'newitem7' },
                    { id: 8, name: 'newitem8' }
                ];

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: []
                    });
                });

                it('добавит элементы в список', function() {
                    composite.add(addData);

                    expect(composite).to.haveElems('item', addData.length);
                });

                it('шаблонизирует элементы с корректными данными', function() {
                    var result = [];

                    composite.add(addData);

                    composite.findElem('item').each(function(index) {
                        result.push($(this).text() == addData[index].name);
                    });

                    expect(result.every(function(item) { return item })).to.be.true;
                });
            });

            describe('add(item, { itemOptions: { elemMods: { ... } } )', function() {
                var addData = { id: 9, name: 'newitem9' },
                    addParams = { itemOptions: { elemMods: { state: 'new' } } };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: []
                    });
                });

                it('добавит элемент в список', function() {
                    composite.add(addData);

                    expect(composite).to.haveElems('item', 1);
                });

                it('применит доп. информацию при шаблонизации строки', function() {
                    composite.add(addData, addParams);

                    expect(/b-some__item_state_new/.test(composite.elem('item').first().attr('class'))).to.be.true;
                });
            });

            describe('add([item1, item2], { itemOptions: { elemMods: { ... } })', function() {
                var addData = [{ id: 9, name: 'newitem9' }, { id: 10, name: 'newitem10' }],
                    addParams = { itemOptions: { elemMods: { state: 'new' } } };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: []
                    });
                });

                it('добавит элементы в список', function() {
                    composite.add(addData);

                    expect(composite).to.haveElems('item', addData.length);
                });

                it('применит доп. информацию при шаблонизации строк', function() {
                    var result = [];

                    composite.add(addData, addParams);

                    composite.findElem('item').each(function() {
                        result.push(/b-some__item_state_new/.test($(this).attr('class')));
                    });

                    expect(result.every(function(item) { return item })).to.be.true;
                });
            });
        });
    });

    /*
     * проверка обновления
     * */
    describe('метод обновления строк (update)', function() {
        var initialData = [
            { id: 1, name: 'item1' },
            { id: 2, name: 'item2' }
        ];

        afterEach(function() {
            composite.destruct();
        });

        describe('вызывает событие', function() {
            var spy,
                updateData = { id: 2, name: 'updated' };

            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    collection: initialData
                });

                spy = sandbox.spy(composite, 'trigger');
            });

            ['before:update', 'update'].forEach(function(event, i) {
                it(event + ' с данными обновления', function() {
                    composite.update(2, updateData);

                    expect(spy[!i ? 'firstCall' : 'secondCall'].calledWith(event, {
                        items: [{
                            id: updateData.id,
                            newData: updateData
                        }]
                    })).to.true;
                });
            })
        });

        describe('при использовании в виде', function() {

            describe('update(id, data)', function() {
                var updateData = { id: 1, name: 'updated' };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: initialData
                    });
                });

                it('перерисует элемент списка с id ' + updateData.id  + ' с новыми данными', function() {
                    composite.update(1, updateData);

                    expect(composite.findElem('item').first().text()).to.be.equal(updateData.name)
                });
            });

            describe('update({ id1: data1, id2: data2 }})', function() {
                var updateData = {
                    1: { name: 'update1' },
                    2: { name: 'update2' }
                };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: initialData
                    });
                });

                it('перерисует элементы списка с id ' + Object.keys(updateData).join() + ' с новыми данными', function() {
                    var result = [];

                    composite.update(updateData);

                    composite.findElem('item').each(function(index) {
                        result.push($(this).text() == updateData[index + 1].name);
                    });

                    expect(result.every(function(item) { return item })).to.be.true;

                });
            });

            describe('update(id, data, { itemOptions: { ... })', function() {
                var updateData = { id: 1, name: 'updated' },
                    updateParams = { itemOptions: { elemMods: { state: 'updated' } } };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: initialData
                    });
                });

                it('перерисует элемент списка с id ' + updateData.id  + ' с новыми данными', function() {
                    composite.update(1, updateData, updateParams);

                    expect(composite.findElem('item').first().text()).to.be.equal(updateData.name)
                });

                it('применит доп. информацию при шаблонизации строки', function() {
                    composite.update(1, updateData, updateParams);

                    expect(/b-some__item_state_updated/.test(composite.elem('item').first().attr('class'))).to.be.true;
                });
            });

            describe('update({ id1: data1, id2: data2 }, { itemOptions: { ... } })', function() {
                var updateData = {
                        1: { name: 'update1' },
                        2: { name: 'update2' }
                    },
                    updateParams = { itemOptions: { elemMods: { state: 'updated' } } };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: initialData
                    });
                });

                it('перерисует элементы списка с id ' + Object.keys(updateData).join() + ' с новыми данными', function() {
                    var result = [];

                    composite.update(updateData, updateParams);

                    composite.findElem('item').each(function(index) {
                        result.push($(this).text() == updateData[index + 1].name);
                    });

                    expect(result.every(function(item) { return item })).to.be.true;
                });

                it('применит доп. информацию при шаблонизации строк', function() {
                    var result = [];

                    composite.update(updateData, updateParams);

                    composite.findElem('item').each(function() {
                        result.push(/b-some__item_state_updated/.test($(this).attr('class')));
                    });

                    expect(result.every(function(item) { return item })).to.be.true;
                });
            });
        });
    });

    /*
     * проверка удаления
     * */
    describe('метод удаления строк (remove)', function() {
        var initialData = [
            { id: 1, name: 'item1' },
            { id: 2, name: 'item2' },
            { id: 3, name: 'item3' }
        ];

        beforeEach(function() {
            composite = createBlock({
                itemView: { block: 'b-some', elem: 'item' },
                collection: initialData
            });
        });

        afterEach(function() {
            composite.destruct();
        });

        describe('вызывает событие', function() {
            var spy,
                removeData = 2;

            beforeEach(function() {
                spy = sandbox.spy(composite, 'trigger');
            });

            ['before:remove', 'remove'].forEach(function(event, i) {
                it(event + ' с данными новых строк', function() {
                    composite.remove(removeData);

                    expect(spy[!i ? 'firstCall' : 'secondCall'].calledWith(event, {
                        items: [removeData]
                    })).to.true;
                });
            })
        });

        describe('при использовании в виде', function() {
            describe('remove(id)', function() {

                it('удалит один элемент по ID', function() {
                    composite.remove(2);

                    expect(composite).to.haveElems('item', initialData.length - 1);
                });
            });

            describe('remove(id1, id2)', function() {
                var removeData = [1,2];

                it('удалит элементы с переданными ID (' + removeData.length + 'шт.)', function() {
                    composite.remove(removeData);

                    expect(composite).to.haveElems('item', initialData.length - removeData.length);
                });
            });
        });
    });

    /*
     * проверка замены элементов
     * */
    describe('метод замены всех строк (reset)', function() {
        afterEach(function() {
            composite.destruct();
        });

        describe('вызывает событие', function() {
            var spy,
                resetData = { id: 'new-item', name: 'reseted' };

            beforeEach(function() {
                composite = createBlock({
                    itemView: { block: 'b-some', elem: 'item' },
                    collection: [
                        { id: 1, name: 'item1' },
                        { id: 2, name: 'item2' },
                        { id: 3, name: 'item3' }
                    ]
                });

                spy = sandbox.spy(composite, 'trigger');
            });

            ['before:reset', 'reset'].forEach(function(event, i) {
                it(event + ' с данными новых строк', function() {
                    composite.reset(resetData);

                    expect(spy[!i ? 'firstCall' : 'secondCall'].calledWith(event, {
                        items: [resetData]
                    })).to.true;
                });
            })
        });

        describe('при использовании в виде', function() {

            describe('reset(data)', function() {

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: [
                            { id: 1, name: 'item1' },
                            { id: 2, name: 'item2' },
                            { id: 3, name: 'item3' }
                        ]
                    });
                });

                it('заменить элементы списка, одним элементом', function() {
                    composite.reset({ name: 'reseted' });

                    expect(composite).to.haveElems('item', 1);
                });

                it('шаблонизирует элемент с корректными данными', function() {
                    composite.reset({ name: 'reseted' });

                    expect(composite.findElem('item').first().text()).to.be.equal('reseted')
                });
            });

            describe('reset([data1, data2, data3])', function() {
                var resetData = [
                    { name: 'reset1' },
                    { name: 'reset2' },
                    { name: 'reset2' }
                ];

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: [
                            { id: 1, name: 'item1' },
                            { id: 2, name: 'item2' },
                            { id: 3, name: 'item3' }
                        ]
                    });
                });

                it('заменить элементы списка, новым набором (' + resetData.length + 'шт.)', function() {
                    composite.reset(resetData);

                    expect(composite).to.haveElems('item', resetData.length);
                });

                it('шаблонизирует элементы с корректными данными', function() {
                    var result = [];

                    composite.reset(resetData);

                    composite.findElem('item').each(function(index) {
                        result.push($(this).text() == resetData[index].name);
                    });

                    expect(result.every(function(item) { return item })).to.be.true;
                });
            });

            describe('reset(data, { itemOptions: { elemMods: { state: \'reseted\' } } })', function() {
                var resetData = { name: 'reseted' },
                    resetParams = { itemOptions: { elemMods: { state: 'reseted' } } };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: [
                            { id: 1, name: 'item1' },
                            { id: 2, name: 'item2' },
                            { id: 3, name: 'item3' }
                        ]
                    });
                });

                it('заменить элементы списка, одним элементом', function() {
                    composite.reset(resetData, resetParams);

                    expect(composite).to.haveElems('item', 1);
                });

                it('шаблонизирует элемент с корректными данными', function() {
                    composite.reset(resetData, resetParams);

                    expect(composite.findElem('item').first().text()).to.be.equal(resetData.name)
                });

                it('применит доп. информацию при шаблонизации', function() {
                    composite.reset(resetData, resetParams);

                    expect(/b-some__item_state_reseted/.test(composite.elem('item').first().attr('class'))).to.be.true;
                });
            });

            describe('reset([data, data1], { itemOptions: { elemMods: { state: \'reseted\' } } })', function() {
                var resetData = [
                        { name: 'reset1' },
                        { name: 'reset2' },
                        { name: 'reset2' }
                    ],
                    resetParams = { itemOptions: { elemMods: { state: 'reseted' } } };

                beforeEach(function() {
                    composite = createBlock({
                        itemView: { block: 'b-some', elem: 'item' },
                        collection: [
                            { id: 1, name: 'item1' },
                            { id: 2, name: 'item2' },
                            { id: 3, name: 'item3' }
                        ]
                    });
                });

                it('заменить элементы списка, новым набором (' + resetData.length + 'шт.)', function() {
                    composite.reset(resetData, resetParams);

                    expect(composite).to.haveElems('item', resetData.length);
                });

                it('шаблонизирует элементы с корректными данными', function() {
                    var result = [];

                    composite.reset(resetData, resetParams);

                    composite.findElem('item').each(function(index) {
                        result.push($(this).text() == resetData[index].name);
                    });

                    expect(result.every(function(item) { return item })).to.be.true;
                });

                it('применит доп. информацию при шаблонизации строк', function() {
                    var result = [];

                    composite.reset(resetData, resetParams);

                    composite.findElem('item').each(function() {
                        result.push(/b-some__item_state_reseted/.test($(this).attr('class')));
                    });

                    expect(result.every(function(item) { return item })).to.be.true;
                });
            });
        });
    });

    /*
     * проверка очистки списка
     * */
    describe('метод очистки списка (clear)', function() {

        beforeEach(function() {
            composite = createBlock({
                itemView: { block: 'b-some', elem: 'item' },
                collection: [
                    { id: 1, name: 'item1' },
                    { id: 2, name: 'item2' },
                    { id: 3, name: 'item3' }
                ]
            });
        });

        afterEach(function() {
            composite.destruct();
        });

        describe('вызывает событие', function() {
            var spy;

            beforeEach(function() {
                spy = sandbox.spy(composite, 'trigger');
            });

            ['before:clear', 'clear'].forEach(function(event, i) {
                it(event + ' с данными новых строк', function() {
                    composite.clear();

                    expect(spy[!i ? 'firstCall' : 'secondCall'].calledWith(event, null)).to.true;
                });
            })
        });

        it('удаляет все элементы', function() {
            composite.clear();

            expect(composite).to.haveElems('item', 0);
        });
    });

    afterEach(function() {
        sandbox.restore();
    });
});
