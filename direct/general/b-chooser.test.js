describe('b-chooser', function() {
    var block,
        clock,
        wrap = $('<div>'),
        createBlock = function(params, parent) {
            params || (params = {});

            var bemjson = {
                block: 'b-chooser',
                mods: params.mods,
                content: params.content
            };

            block = BEM.DOM.init(u.getDOMTree(bemjson).appendTo(parent || wrap)).bem('b-chooser');
        };

    describe('Без модификаторов', function() {

        describe('Группы', function() {

            beforeEach(function() {
                createBlock({
                    content: {
                        elem: 'group',
                        text: 'философы Ближнего Востока',
                        content: {
                            elem: 'item',
                            name: 'avicenna',
                            content: 'Авиценна'
                        }
                    }
                }, $('body'));

                clock = sinon.useFakeTimers();
            });

            afterEach(function() {
                block.destruct();
                clock.restore();
            });

            it('Должны быть представлены элементом group', function() {
                expect(block.elemInstance('group').domElem.length).to.be.equal(1);
            });

            it('Должны содержать заголовок (элемент group-title)', function() {
                expect(block.elemInstance('group').findElem('item', 'type', 'group-title').length).to.be.equal(1);
            });

            it('Должен содержать обертку (элемент group-list)', function() {
                expect(block.elemInstance('group').findElem('group-list').length).to.be.equal(1);
            });

            it('Должны содержать элементы item', function() {
                expect(block.elemInstance('group').findElem('item', 'name', 'avicenna').length).to.be.equal(1);
            });

            it('Должны уметь сворачиваться', function() {
                var groupTitleElem = block.elemInstance('item', 'type', 'group-title').domElem;

                groupTitleElem.trigger('click');

                clock.tick(0);

                expect(block.hasMod(groupTitleElem, 'open', 'yes')).to.be.equal(true);
                expect(block.hasMod(block.findElem('group-list'), 'open', 'yes')).to.be.equal(true);
            });

            it('Должны уметь разворачиваться', function() {
                var groupTitleElem = block.elemInstance('item', 'type', 'group-title').domElem;

                groupTitleElem
                    .trigger('click')
                    .trigger('click');

                expect(block.hasMod(groupTitleElem, 'open', 'yes')).to.be.equal(false);
                expect(block.hasMod(block.findElem('group-list'), 'open', 'yes')).to.be.equal(false);
            });

        });

        describe('getSelected()', function() {

            beforeEach(function() {
                createBlock({
                    content: {
                        elem: 'item',
                        elemMods: { selected: 'yes' },
                        name: 'avicenna',
                        content: 'Авиценна'
                    }
                });
            });

            afterEach(function() {
                block.destruct();
            });

            it('Должен вернуть параметры выбранного элемента item', function() {
                var itemParams = block.getSelected();

                expect(itemParams.name).to.be.equal('avicenna');
                expect(itemParams.selected).to.be.equal(true);
                expect(itemParams.extraParams).to.be.an.instanceof(Object);
            });

        });

        describe('getAll()', function() {

            beforeEach(function() {
                createBlock({
                    content: [
                        {
                            elem: 'item',
                            elemMods: { selected: 'yes'},
                            name: 'avicenna',
                            content: 'Авиценна'
                        },
                        {
                            elem: 'item',
                            name: 'omar-khayyam',
                            content: 'Омар Хайям'
                        }
                    ]
                });
            });

            afterEach(function() {
                block.destruct();
            });

            it('Должен вернуть параметры всех элементов item', function() {
                expect(u._.isEqual(block.getAll(), [
                    { extraParams: {}, disabled: false, hidden: false, indeterminate: false, selected: true, name: 'avicenna' },
                    { extraParams: {}, disabled: false, hidden: false, indeterminate: false, selected: false, name: 'omar-khayyam' }
                ])).to.be.equal(true);
            });

        });

        describe('check()', function() {

            beforeEach(function() {
                createBlock({
                    content: [
                        {
                            elem: 'item',
                            name: 'democritus',
                            content: 'Демокрит'
                        },
                        {
                            elem: 'item',
                            elemMods: { selected: 'yes'},
                            name: 'avicenna',
                            content: 'Авиценна'
                        },
                        {
                            elem: 'item',
                            name: 'omar-khayyam',
                            content: 'Омар Хайям'
                        }
                    ]
                });
            });

            afterEach(function() {
                block.destruct();
            });

            it('Должен выбирать только один элемент item', function() {
                block.check('omar-khayyam');

                expect(block.findElem('item', 'selected', 'yes').length).to.be.equal(1);
                expect(block.getSelected().name).to.be.equal('omar-khayyam');
            });

            it('Должен быть выбран один правильный элемент item, после всех check-ов', function() {
                block
                    .check('omar-khayyam')
                    .check('democritus');

                expect(block.findElem('item', 'selected', 'yes').length).to.be.equal(1);
                expect(block.getSelected().name).to.be.equal('democritus');
            });

            it(['Должен вызывать два события change на блоке:',
                'первое про отмену выбора, второе про выбор нового элемента'
            ].join(' '), function() {
                sinon.spy(block, 'trigger');
                block.check('omar-khayyam');

                expect(block.trigger.calledWith('change')).to.be.equal(true);
                expect(block.trigger.callCount).to.be.equal(2);

                // uncheck «Авиценна»
                expect(block.trigger.firstCall.args[1].selected).to.be.equal(false);
                expect(block.trigger.firstCall.args[1].name).to.be.equal('avicenna');

                // check «Омар Хайям»
                expect(block.trigger.secondCall.args[1].selected).to.be.equal(true);
                expect(block.trigger.secondCall.args[1].name).to.be.equal('omar-khayyam');

                block.trigger.restore();
            });

        });

        describe('uncheck()', function() {

            beforeEach(function() {
                createBlock({
                    content: [
                        {
                            elem: 'item',
                            name: 'democritus',
                            content: 'Демокрит'
                        },
                        {
                            elem: 'item',
                            elemMods: { selected: 'yes'},
                            name: 'avicenna',
                            content: 'Авиценна'
                        }
                    ]
                });
            });

            afterEach(function() {
                block.destruct();
            });

            it('После снятия выбора с активного item-а не должно остаться выбранных item-ов', function() {
                block.uncheck('avicenna');

                expect(block.findElem('item', 'selected', 'yes').length).to.be.equal(0);
            });

            it('Не должен поменяться выбранный item при uncheck-е невыбранного', function() {
                block.uncheck('democritus');

                expect(block.getSelected().name).to.be.equal('avicenna');
            });

            it('Должно происходить событие change', function() {
                sinon.spy(block, 'trigger');
                block.uncheck('avicenna');

                expect(block.trigger.calledWith('change')).to.be.equal(true);
                expect(block.trigger.callCount).to.be.equal(1);

                // uncheck «Авиценна»
                expect(block.trigger.firstCall.args[1].selected).to.be.equal(false);
                expect(block.trigger.firstCall.args[1].name).to.be.equal('avicenna');

                block.trigger.restore();
            });

            it('Не должно происходить событие change при uncheck-е невыбранного', function() {
                sinon.spy(block, 'trigger');
                block.uncheck('democritus');

                expect(block.trigger.calledWith('change')).to.be.equal(false);
                expect(block.trigger.callCount).to.be.equal(0);

                block.trigger.restore();
            });

        });

        describe('remove()', function() {

            beforeEach(function() {
                createBlock({
                    content: {
                        elem: 'item',
                        name: 'avicenna',
                        content: 'Авиценна'
                    }
                });
            });

            afterEach(function() {
                block.destruct();
            });

            it('Должен удалить item', function() {
                block.remove('avicenna');

                expect(block.findElem('item', 'name', 'avicenna').length).to.be.equal(0);
            });

            it('Не должно быть изменений в блоке после вызова remove несуществующего элемента item', function() {
                block.remove('nietzsche');

                expect(block.getAll().length).to.be.equal(1);
            });

        });

        describe('add()', function() {

            beforeEach(function() {
                createBlock({
                    content: {
                        elem: 'item',
                        name: 'avicenna',
                        content: 'Авиценна'
                    }
                });
            });

            afterEach(function() {
                block.destruct();
            });

            it('Должен добавить новый item', function() {
                block.add({
                    name: 'heraclitus',
                    content: 'Гераклит'
                });

                expect(block.findElem('item', 'name', 'heraclitus').length).to.be.equal(1);
            });

            it('Не должен добавить новый item, если в блоке уже находится item с таким же name', function() {
                var originalItem = block.findElem('item', 'name', 'avicenna')[0];

                block.add({
                    name: 'avicenna',
                    content: 'Авиценна'
                });

                expect(block.findElem('item', 'name', 'avicenna')[0]).to.be.equal(originalItem);
            });

        });

        describe('disable()', function() {

            beforeEach(function() {
                createBlock({
                    content: {
                        elem: 'item',
                        name: 'avicenna',
                        content: 'Авиценна'
                    }
                });
            });

            afterEach(function() {
                block.destruct();
            });

            it('Должен выставить модификатор disabled_yes на item', function() {
                block.disable('avicenna');

                expect(block.findElem('item', 'disabled', 'yes').length).to.be.equal(1);
                expect(block.hasMod(block.findElem('item', 'name', 'avicenna'), 'disabled', 'yes')).to.be.equal(true);
            });

        });

        describe('enable()', function() {

            beforeEach(function() {
                createBlock({
                    content: {
                        elem: 'item',
                        name: 'avicenna',
                        content: 'Авиценна'
                    }
                });
            });

            afterEach(function() {
                block.destruct();
            });

            it('Должен удалить модификатор disabled_yes с item-а', function() {
                block
                    .disable('avicenna')
                    .enable('avicenna');

                expect(block.findElem('item', 'disabled', 'yes').length).to.be.equal(0);
            });

        });
        
        describe('Динамическое добавление multi_yes', function() {
            beforeEach(function() {
                createBlock({
                    content: [
                        {
                            elem: 'item',
                            name: 'democritus',
                            content: 'Демокрит'
                        },
                        {
                            elem: 'item',
                            name: 'avicenna',
                            content: 'Авиценна'
                        },
                        {
                            elem: 'item',
                            name: 'omar-khayyam',
                            content: 'Омар Хайям'
                        }
                    ]
                });
            });
    
            afterEach(function() {
                block.destruct();
            });
            
            it('Должен выбрать элемент avicenna и democritus', function() {
                block.check('avicenna');
                block.setMod('multi', 'yes');
                block.check('democritus');
                
                var selected = block.getSelected().map(u._.property('name'));
                
                expect(selected).to.be.include.all('avicenna', 'democritus');
            });
            
            it('Должен выбрать элемент avicenna и democritus', function() {
                block.check('avicenna');
                block.setMod('multi', 'yes');
                block.check('democritus');
                
                var selected = block.getSelected().map(u._.property('name'));
                
                expect(selected).to.be.include.all('avicenna', 'democritus');
            });
            
            it('Должен иметь один выбранный элемент', function() {
                block.setMod('multi', 'yes');
                block.check('avicenna');
                block.setMod('multi', '');
                block.check('democritus');
                
                var selected = block.getAll('selected').map(u._.property('name'));
                
                expect(selected.length).to.be.eq(1);
            });
            
            it('Должен выбрать элемент democritus', function() {
                block.setMod('multi', 'yes');
                block.check('avicenna');
                block.setMod('multi', '');
                block.check('democritus');
                
                var selected = block.getAll('selected').map(u._.property('name'));
                
                expect(selected).to.include('democritus');
            });
        });

    });

    describe('Модификатор multi_yes', function() {

        beforeEach(function() {
            createBlock({
                mods: { multi: 'yes' },
                content: [
                    {
                        elem: 'item',
                        elemMods: { selected: 'yes'},
                        name: 'democritus',
                        content: 'Демокрит'
                    },
                    {
                        elem: 'item',
                        name: 'avicenna',
                        content: 'Авиценна'
                    },
                    {
                        elem: 'item',
                        name: 'omar-khayyam',
                        content: 'Омар Хайям'
                    }
                ]
            });
        });

        afterEach(function() {
            block.destruct();
        });
        
        describe('Удаление модификатора', function() {
            
            it('После удаления модификатора и смене значения, должен убрать check с предыдущего', function() {
                block.setMod('multi', '');
                block.check('avicenna');
                
                expect(block.elemInstance('item', 'name', 'democritus').hasMod('selected', 'yes')).to.be.equal(false);
            });
            
            it('После удаления модификатора и смене значения, должен выставить check новому элементу', function() {
                block.setMod('multi', '');
                block.check('avicenna');
                
                expect(block.elemInstance('item', 'name', 'avicenna').hasMod('selected', 'yes')).to.be.equal(true);
            });
            
        });

        describe('getSelected()', function() {

            it('Должен возвращать параметры всех выбранных элементов item', function() {
                block.check('avicenna');

                expect(block.findElem('item', 'selected', 'yes').length).to.be.equal(2);

                expect(u._.isEqual(block.getSelected(), [
                    { selected: true, disabled: false, hidden: false, indeterminate: false, extraParams: {}, name: 'democritus' },
                    { selected: true, disabled: false, hidden: false, indeterminate: false, extraParams: {}, name: 'avicenna' }
                ])).to.be.equal(true);
            });

        });

        describe('uncheck()', function() {

            it('Должен работает корректно, когда выбрано несколько элементов item', function() {
                block.check('avicenna');
                block.check('omar-khayyam');
                block.uncheck('democritus');

                expect(block.elemInstance('item', 'name', 'avicenna').hasMod('selected', 'yes')).to.be.equal(true);
                expect(block.elemInstance('item', 'name', 'omar-khayyam').hasMod('selected', 'yes')).to.be.equal(true);
                expect(block.elemInstance('item', 'name', 'democritus').hasMod('selected', 'yes')).to.be.equal(false);
            });

        });

    });

    describe('Модификатор search_yes', function() {
        var isHidden = function(name, modVal, modName) {
            return block.hasMod(block.findElem(name, modVal, modName), 'visibility', 'hidden');
        };

        beforeEach(function() {
            createBlock({
                mods: { search: 'yes' },
                content: [
                    { elem: 'search' },
                    {
                        elem: 'wrap',
                        content: [
                            {
                                elem: 'item',
                                elemMods: { selected: 'yes' },
                                js: { text: 'Фалес' },
                                name: 'thales',
                                content: 'Фалес'
                            },
                            {
                                elem: 'item',
                                js: { text: 'Пифагор' },
                                name: 'pythagoras',
                                content: 'Пифагор'
                            },
                            {
                                elem: 'item',
                                js: { text: 'Демокрит' },
                                name: 'democritus',
                                content: 'Демокрит'
                            },
                            {
                                elem: 'group',
                                text: 'философы Ближнего Востока',
                                content: [
                                    {
                                        elem: 'item',
                                        js: { text: 'Авиценна' },
                                        name: 'avicenna',
                                        content: 'Авиценна'
                                    },
                                    {
                                        elem: 'item',
                                        js: { text: 'Омар Хайям' },
                                        name: 'omar-khayyam',
                                        content: 'Омар Хайям'
                                    }
                                ]
                            }
                        ]
                    },
                    { elem: 'not-found' }
                ]
            });

            clock = sinon.useFakeTimers();
        });

        afterEach(function() {
            block.destruct();
            clock.restore();
        });

        it('Должна быть строка поиска', function() {
            expect(block.getInput().domElem.length).to.be.equal(1);
        });

        it('Должен быть элемент not-found(«Совпадений не найдено.») на случай неудачного поиска', function() {
            expect(block.getInput().domElem.length).to.be.equal(1);
        });

        describe('add()', function() {

            it('Должен добавить новый item, во врапер(элемент wrap)', function() {
                block.add({
                    js: { text: 'Гераклит' },
                    name: 'heraclitus',
                    content: 'Гераклит'
                });

                expect(block.findElem('item', 'name', 'heraclitus').length).to.be.equal(1);
            });

        });

        describe('search()', function() {

            it('Должно происходить событие search', function() {
                sinon.spy(block, 'trigger');
                block.search('е');

                clock.tick(0);

                expect(block.trigger.calledWith('search')).to.be.equal(true);
                expect(block.trigger.firstCall.args[0]).to.be.equal('search');

                block.trigger.restore();
            });

            it('Не должен выставить блоку модификатор found_no при успешном поиске', function() {
                block.search('е');

                clock.tick(0);

                expect(block.hasMod('found', 'no')).to.be.equal(false);
            });

            it([
                'Должен выставить блоку модификатор found_no при неудачном поиске.',
                'Модификатор found_no показывает сообщение «Совпадений не найдено.»'
            ].join(' '), function() {
                block.search('абр-абр-абр');

                clock.tick(0);

                expect(block.hasMod('found', 'no')).to.be.equal(true);
            });

            it('Должен найти два item-а', function() {
                block.search('фа');

                clock.tick(0);

                // найдено
                expect(isHidden('item', 'name', 'thales')).to.be.equal(false);
                expect(isHidden('item', 'name', 'pythagoras')).to.be.equal(false);

                // не найдено
                expect(isHidden('item', 'name', 'democritus')).to.be.equal(true);

                // не найдено, но скрываются группой
                expect(isHidden('item', 'name', 'avicenna')).to.be.equal(false);
                expect(isHidden('item', 'name', 'omar-khayyam')).to.be.equal(false);
                expect(isHidden('group')).to.be.equal(true);
            });

            it('У найденных item-ов должна быть выделена искомая подстрока', function() {
                block.search('фа');

                clock.tick(0);

                ['thales', 'pythagoras'].map(function(name) {
                    var elemInstance = block.elemInstance('item', 'name', name),
                        elemHlted = elemInstance.findElem('hlted');

                    expect(elemHlted.length).to.be.equal(1);
                    expect(elemHlted.text().toLocaleLowerCase()).to.be.equal('фа');
                });
            });

            it('Блок должен возвращаться в исходное состояние при вводе пустой строки в инпут', function() {
                block.search('и');
                clock.tick(0);

                block.search('ъ');
                clock.tick(0);

                block.search('');
                clock.tick(0);

                expect(isHidden('item', 'name', 'thales')).to.be.equal(false);
                expect(isHidden('item', 'name', 'pythagoras')).to.be.equal(false);
                expect(isHidden('item', 'name', 'democritus')).to.be.equal(false);
                expect(isHidden('item', 'name', 'avicenna')).to.be.equal(false);
                expect(isHidden('item', 'name', 'omar-khayyam')).to.be.equal(false);
                expect(isHidden('group')).to.be.equal(false);
            });

            it('Должны сворачиваться группы при вводе пустой строки в инпут', function() {
                var groupTitleElem = block.elemInstance('item', 'type', 'group-title').domElem;

                block.search('и');
                clock.tick(0);

                block.search('');
                clock.tick(0);

                expect(block.hasMod(groupTitleElem, 'open', 'yes')).to.be.equal(false);
                expect(block.hasMod(block.findElem('group-list'), 'open', 'yes')).to.be.equal(false);
            });

            it('В названии группы должна быть выделена искомая подстрока', function() {
                block.search('и');

                clock.tick(0);

                var elemInstance = block.elemInstance('item', 'type', 'group-title'),
                    elemHlted = elemInstance.findElem('hlted');

                // «философы Ближнего Востока» содержит две «и»
                expect(elemHlted.length).to.be.equal(2);
                elemHlted.each(function(index, elem) {
                    expect($(elem).text().toLocaleLowerCase()).to.be.equal('и');
                });
            });

            it([
                'Группа должна быть развернута и все её item-ы показаны,',
                'если в её названии есть искомая подстрока'
            ].join(' '), function() {
                block.search('и');

                clock.tick(0);

                // найдено в названии группы
                expect(block.hasMod(block.findElem('item', 'type', 'group-title'), 'open', 'yes')).to.be.equal(true);

                // найдено в элементах
                expect(isHidden('item', 'name', 'democritus')).to.be.equal(false);
                expect(isHidden('item', 'name', 'pythagoras')).to.be.equal(false);
                expect(isHidden('item', 'name', 'avicenna')).to.be.equal(false);

                // не найдено в элементе, но найдено в группе
                expect(isHidden('item', 'name', 'omar-khayyam')).to.be.equal(false);
                expect(isHidden('group')).to.be.equal(false);

                // не найдено
                expect(isHidden('item', 'name', 'thales')).to.be.equal(true);
            });

            it([
                'Элементы в которых не встречается искомая подстрока должны быть скрыты, если они лежат внутри группы',
                'в названии которой отсутствует искомая подстрока и рядом(в этой группе) есть элементы',
                'в которых искомая подстрока присутствует',
                '(необходимо уловить разницу с тестом, где элементы не скрывались',
                'т.к. в названии группы была искомая подстрока)'
            ].join(' '), function() {
                block.search('ави');

                clock.tick(0);

                // элементы в которых не встречается искомая подстрока
                expect(isHidden('item', 'name', 'democritus')).to.be.equal(true);
                expect(isHidden('item', 'name', 'pythagoras')).to.be.equal(true);
                expect(isHidden('item', 'name', 'omar-khayyam')).to.be.equal(true);
                expect(isHidden('item', 'name', 'thales')).to.be.equal(true);

                // элементы в которых искомая строка присутствует
                expect(isHidden('item', 'name', 'avicenna')).to.be.equal(false);

                // группа в названии которой отсутствует искомая подстрока
                expect(block.hasMod(block.findElem('item', 'type', 'group-title'), 'open', 'yes')).to.be.equal(true);
                expect(block.elemInstance('item', 'type', 'group-title').findElem('hlted').length).to.be.equal(0);
            });

        });

    });

    describe('Модификатор with_remove-confirm элемента item', function() {

        beforeEach(function() {
            createBlock({
                content: {
                    elem: 'item',
                    elemMods: { with: 'remove-confirm' },
                    js: { text: 'Пифагор' },
                    name: 'pythagoras',
                    content: 'Пифагор'
                }
            }, $('body'));

            clock = sinon.useFakeTimers();
        });

        afterEach(function() {
            block.destruct();
            clock.restore();
        });

        it('Должен быть выставлен модификатор with_remove-confirm, который позволяет удалять item-ы', function() {
            expect(block.hasMod(block.findElem('item', 'name', 'pythagoras'), 'with', 'remove-confirm')).to.be.equal(true);
        });

        it('У item-ов должны быть кнопки удаления(remove-confirm) и подтверждение удаления(remove)', function() {
            var elemInstance = block.elemInstance('item', 'name', 'pythagoras');

            expect(elemInstance.findElem('action', 'type', 'remove-confirm').length).to.be.equal(1);
            expect(elemInstance.findElem('action', 'type', 'remove').length).to.be.equal(1);
        });

        it([
            'По клику на кнопку remove-confirm должен быть выставлен модификатор state_show-confirm элементу item.',
            'Модификатор state_show-confirm показывает кнопку удаления(remove)'
        ].join(' '), function() {
            var itemInstance = block.elemInstance('item', 'name', 'pythagoras'),
                buttonInstance = itemInstance.elemInstance('action', 'type', 'remove-confirm');

            buttonInstance.domElem.trigger('click');

            expect(itemInstance.hasMod('state', 'show-confirm')).to.be.equal(true);
        });

        it('По нажатию на кнопке удаления(remove) она должна становиться неактивной', function() {
            var itemInstance = block.elemInstance('item', 'name', 'pythagoras'),
                buttonInstance = itemInstance.elemInstance('action', 'type', 'remove'),
                button = buttonInstance.findBlockOn('button');

            buttonInstance.domElem.trigger('click');

            expect(button.hasMod('disabled', 'yes')).to.be.equal(true);
        });

        it('По нажатию кнопки удаления(remove) на блоке должно происходить событие removing', function() {
            var itemInstance = block.elemInstance('item', 'name', 'pythagoras'),
                buttonInstance = itemInstance.elemInstance('action', 'type', 'remove');

            sinon.spy(block, 'trigger');

            buttonInstance.domElem.trigger('click');

            expect(block.trigger.calledWith('removing')).to.be.equal(true);
            expect(block.trigger.callCount).to.be.equal(1);

            block.trigger.restore();
        });

        describe('onError()', function() {

            it('Должно происходить событие error', function() {
                sinon.spy(block, 'trigger');
                block.onError({ type: 'remove' });

                expect(block.trigger.calledWith('error')).to.be.equal(true);
                expect(block.trigger.callCount).to.be.equal(1);

                block.trigger.restore();
            });

            it('Кнопка удаления(remove) должна быть активной, после события error', function() {
                var itemInstance = block.elemInstance('item', 'name', 'pythagoras'),
                    buttonInstance = itemInstance.elemInstance('action', 'type', 'remove'),
                    button = buttonInstance.findBlockOn('button');

                buttonInstance.domElem.trigger('click');
                block.onError({ type: 'remove' });

                expect(button.hasMod('disabled', 'yes')).to.be.equal(false);
            });

        });

    });

});
