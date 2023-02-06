describe('b-statistics-template-list', function() {
    var block,
        clock,
        wrap = $('<div>'),
        /**
         * Создает блок
         * @param {Array} [templates] - список шаблонов
         * @param {jQuery} [parent] - jQuery нода в которую будет добавлен построенный блок
         */
        createBlock = function(templates, parent) {
            if (!u._.isArray(templates)) {
                parent = templates;
                templates = undefined;
            }

            var bemjson = {
                block: 'b-statistics-template-list',
                defaultTitle: 'Шаблоны',
                templates: templates || [
                    {
                        params: { template: '1' },
                        title: 'Первый',
                        selected: true
                    },
                    {
                        params: { template: '2' },
                        title: 'Второй'
                    }
                ]
            };

            block = BEM.DOM.init(u.getDOMTree(bemjson).appendTo(parent || wrap)).bem('b-statistics-template-list');
        };

    describe('Без шаблонов', function() {

        beforeEach(function() {
            createBlock([]);
        });

        afterEach(function() {
            block.destruct();
        });

        it('Должен отсутствовать элемент dropdown', function() {
            expect(block.findBlocksInside('dropdown').length).to.be.equal(0);
        });

        describe('add()', function() {

            it('Должен строить dropdown при добавление нового элемента(шаблона)', function() {
                block.add({
                    params: { template: '1' },
                    title: 'Шаблон'
                });

                expect(block.findBlocksInside('dropdown').length).to.be.equal(1);
            });

            it('Добавленный элемент должен быть выбран', function() {
                block.add({
                    params: { template: '1' },
                    title: 'Шаблон'
                });

                expect(block.getSelected().title).to.be.equal('Шаблон');
                expect(block.getSelected().params).to.be.eql({ template: '1' });
            });

        });

        describe('getSelected()', function() {

            it('Должен вернуть null когда нет доступных элементов', function() {
                expect(block.getSelected()).to.be.null;
            });

        });

    });

    describe('C шаблонами', function() {

        describe('', function() {

            beforeEach(function() {
                createBlock($('body'));
            });

            afterEach(function() {
                block.destruct();
            });

            it('Должен содержать «Новый шаблон»', function() {
                expect(block._chooser.findElem('item', 'name', 'reset').length).to.be.equal(1);
            });

            it('Не должно остаться выбранных элементов(шаблонов) после клика на «Новый шаблон»', function() {
                block.findElem('switcher').trigger('click');
                block._chooser.findElem('item', 'name', 'reset').trigger('click');

                expect(block.getSelected()).to.be.null;
            });

            it('Должен закрываться popup после выбора элемента из блока b-chooser', function() {
                block.findElem('switcher').trigger('click');
                block._chooser.findElem('item', 'name', 'reset').trigger('click');

                expect(block._dropdown.getPopup().isShown()).to.be.equal(false);
            });

            it('Должно происходить событие select когда выбирается элемент(шаблон)', function() {
                expect(block).to.triggerEvent(
                    'select',
                    { title: 'Шаблоны', params: {}, name: 'reset', statType: undefined },
                    function() {
                        block.findElem('switcher').trigger('click');
                        block._chooser.findElem('item', 'name', 'reset').trigger('click');
                    }
                );
            });

        });

        describe('', function() {

            beforeEach(function() {
                createBlock();

                clock = sinon.useFakeTimers();
            });

            afterEach(function() {
                block.destruct();
                clock.restore();
            });

            describe('findTemplate()', function() {

                it('Должен найти шаблон с заголовком «Первый»', function() {
                    expect(block.findTemplate('Первый').title).to.be.equal('Первый');
                });

                it('Должен вернуть параметры найденного шаблона', function() {
                    expect(u._.isEqual(block.findTemplate('Первый').params, { template: '1' })).to.be.equal(true);
                });

                it('Должен вернуть undefined если нет шаблона с заданным названием', function() {
                    expect(block.findTemplate('абр-абр-абр')).to.be.undefined;
                });

            });

            describe('add()', function() {

                it('Должен добавлять элемент в существующий список', function() {
                    block.add({
                        params: { template: '3' },
                        title: 'Шаблон 3'
                    });

                    // 4 т.к. у блока есть «Новый шаблон»
                    expect(block._chooser.getAll().length).to.be.equal(4);
                });

                it('Добавленный элемент должен быть выбран', function() {
                    block.add({
                        params: { template: '3' },
                        title: 'Шаблон 3'
                    });

                    expect(block.getSelected().title).to.be.equal('Шаблон 3');
                    expect(block.getSelected().params).to.be.eql({ template: '3' });
                });

                it('Текст заголовка должен измениться на title выбранного шаблона', function() {
                    expect(block._title.text()).to.be.equal('Первый');

                    block.add({
                        params: { template: '3' },
                        title: 'Шаблон 3'
                    });

                    expect(block._title.text()).to.be.equal('Шаблон 3');
                });

                // skip убрать или удалить выражение после — DIRECT-58019
                it([
                    'Должен перезаписать данные шаблона, если добавляемый шаблон содержит название(title),',
                    'которое уже есть в списке шаблонов'
                ].join(' '), function() {
                    var originalElem = block._chooser.findElem('item', 'name', block.findTemplate('Первый').name)[0],
                        updatedElem;

                    block.add({
                        params: { template: '3' },
                        title: 'Первый',
                        content: 'Первый'
                    });

                    updatedElem = block._chooser.findElem('item', 'name', block.findTemplate('Первый').name)[0];

                    expect(updatedElem.innerHTML).to.be.equal(originalElem.innerHTML);
                    expect(block.getSelected().title).to.be.equal('Первый');
                    expect(block.getSelected().params).to.be.eql({ template: '3' });
                });

                it('Не должно происходить событие select при добавлении', function() {
                    sinon.spy(block, 'trigger');
                    block.add({
                        params: { template: '3' },
                        title: 'Шаблон 3'
                    });

                    expect(block.trigger.calledWith('select')).to.be.equal(false);
                    expect(block.trigger.callCount).to.be.equal(0);

                    block.trigger.restore();
                });

            });

            describe('remove()', function() {

                it('Должен удалить шаблон из списка', function() {
                    var template = block.getSelected();

                    block.add({
                        params: { template: '3' },
                        title: 'Шаблон 3'
                    });
                    block.remove(template.name);

                    expect(block.findTemplate(template.template)).to.be.undefined;
                });

                it('Должен удалять dropdown если не осталось шаблонов', function() {
                    var templates = block._chooser.getAll();

                    block.remove(templates[1].name);
                    block.remove(templates[2].name);

                    clock.tick(0);

                    expect(block.findBlocksInside('dropdown').length).to.be.equal(0);
                });

                it('Текст заголовка должен измениться на defaultTitle, если не осталось шаблонов', function() {
                    var templates = block._chooser.getAll();

                    block.remove(templates[1].name);
                    block.remove(templates[2].name);

                    clock.tick(0);

                    expect(block.domElem.text()).to.be.equal('Шаблоны');
                });

                it('На блоке должно происходить событие remove когда удаляется элемент(шаблон)', function() {
                    var templates = block._chooser.getAll();

                    sinon.spy(block, 'trigger');
                    block.remove(templates[2].name);

                    clock.tick(0);

                    expect(block.trigger.calledWith('remove')).to.be.equal(true);
                    expect(block.trigger.callCount).to.be.equal(1);
                    expect(block.trigger.firstCall.args[1].name).to.be.equal(templates[2].name);

                    block.trigger.restore();
                });

            });

        });

        describe('Отложенная инициализация:', function() {

            beforeEach(function() {
                block = BEM.DOM.append($('body'), u.getDOMTree({
                    block: 'b-statistics-template-list',
                    defaultTitle: 'Шаблоны',
                    templates: [{
                        params: { template: '1' },
                        title: 'Первый',
                        selected: true
                    }]
                }));
            });

            afterEach(function() {
                block = block.bem('b-statistics-template-list');

                block.destruct();
            });

            it('Должен инититься вместе с блоком dropdown', function() {
                    $('.dropdown', block).bem('dropdown');

                    expect($('.b-statistics-template-list')
                        .hasClass('b-statistics-template-list_js_inited')).to.be.true;
                });

        });

    });

});
