describe('b-select-filter', function() {
    var sandbox,
        block;

    function createBlock(text, mods, options, messages) {
        block = u.getInitedBlock({
            block: 'b-select-filter',
            mods: mods,
            text: text,
            options: options,
            messages: messages
        });
    }
    function checkDisabledElements(elements) {
        return elements.map(function (elemValue) {
            var elem = block._chooser.elem('item', 'name', u.beminize(elemValue));
            return block._chooser.hasMod(elem, 'disabled', 'yes');
        });
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Методы', function() {
        var options,
            text,
            selectedOptions,
            disabledOptions;
        beforeEach(function() {
            text = 'Filter Name';
            options = [
                { text: 'testName', value: 'testValue' },
                { text: 'testName 1', value: 'testValue1', selected: 'yes' },
                { text: 'testName 2', value: 'testValue2' },
                { text: 'testName 3', value: 'testValue3' },
                { text: 'testName 4', value: 'testValue4' },
                { text: 'testName 5', value: 'testValue5' },
                { text: 'testNameDisabled', value: 'testValueDisabled', disabled: 'yes' }
            ];
        });

        afterEach(function() {
            block.destruct && block.destruct();
        });

        describe('Общие', function() {

            it('getSwitcher должен вернуть кнопку', function() {
                createBlock(text, { multi: 'no' }, options);

                expect(block.getSwitcher().__self._name).to.be.eq('button')
            });

            it('enableItems должен включить выключенный пункт', function() {
                var elem;

                createBlock(text, { multi: 'no' }, options);

                block.enableItems(['testValueDisabled']);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                elem = block._chooser.elem('item', 'name', u.beminize('testValueDisabled'));

                expect(block._chooser).to.not.haveMod(elem, 'disabled');
            });

            it('disableItems должен выключить включенный пункт', function() {
                var elem;
                createBlock(text, { multi: 'no' }, options);

                block.disableItems(['testValue']);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                elem = block._chooser.elem('item', 'name', u.beminize('testValue'));

                expect(block._chooser).to.haveMod(elem, 'disabled');
            });

            it('setSwitcherName должен сменить имя фильтра', function() {
                createBlock(text, { multi: 'no' }, options);

                block.setSwitcherName('New name');

                expect(block.getSwitcher().domElem.text()).to.eq('New name: testName 1');
            });

            it('getPopup должен вернуть экземпляр popup2', function() {
                createBlock(text, { multi: 'no' }, options, selectedOptions, disabledOptions);

                expect(block.getPopup().__self._name).to.be.eq('popup2')
            });

            it('setSelected должен генерировать событие change, если состояние поменялось', function() {
                createBlock(text, { multi: 'no' }, options);

                expect(block).to.triggerEvent('change', function() {
                    block.setSelected(['test-value2'])
                });
            });

            it('setSelected не должен генерировать событие change, если состояние не поменялось', function() {
                createBlock(text, { multi: 'no' }, options);

                expect(block).to.not.triggerEvent('change', function() {
                    block.setSelected(['test-value1'])
                });
            });

            it('setSelected должен включить указанный вариант', function() {
                createBlock(text, { multi: 'no' }, options);
                block.setSelected(['test-value2']);
                var selected = block.getSelected().map(u._.property('value'));
                expect(selected).to.include('testValue2')
            });

            it('setSelected должен выключить ранее выбранный вариант', function() {
                createBlock(text, { multi: 'no' }, options);
                block.setSelected(['test-value2']);
                var selected = block.getSelected().map(u._.property('value'));
                expect(selected).to.not.include('testValue');
            });
        });

        describe('Без модификатора multi', function() {

            it('getSelected должен вернуть массив выбранных вариантов', function() {
                createBlock(text, { multi: 'no' }, options);

                expect(block.getSelected()).that.is.an('array')
            })

        });

        describe('C модификатором multi', function() {

            it('getSelected должен вернуть массив выбранных вариантов', function() {
                selectedOptions = [options[1].value, options[2].value];
                createBlock(text, { multi: 'yes' }, options);

                expect(block.getSelected()).that.is.an('array')
            });

        });

    });

    describe('Содержание блока', function() {
        var options,
            text;
        beforeEach(function() {
            text = 'Filter Name';
            options = [
                { text: 'testName', value: 'testValue' },
                { text: 'testName 1', value: 'testValue1', selected: 'yes' },
                { text: 'testName 2', value: 'testValue2', disabled: 'yes' },
                { text: 'testName 3', value: 'testValue3' },
                { text: 'testName 4', value: 'testValue4' },
                { text: 'testName 5', value: 'testValue5' },
                { text: 'testNameDisabled', value: 'testValueDisabled' }
            ];
        });

        afterEach(function() {
            block.destruct && block.destruct();
        });

        describe('Общее', function() {

            it('Должен содержать кнопку для открытия попапа', function() {
                createBlock(text, { multi: 'no' }, options);

                expect(block.getSwitcher().domElem.length).to.eq(1)
            });

            it('Кнопка должна содержать текст Filter Name: не выбран', function() {
                options[1].selected = '';
                createBlock(text, { multi: 'no' }, options);

                expect(block.getSwitcher().domElem.text()).to.eq('Filter Name: не выбран')
            });

        });

        describe('Без модификатора multi', function() {

            it('Кнопка должна содержать текст Filter Name: testName 1', function() {
                createBlock(text, { multi: 'no' }, options);

                expect(block.getSwitcher().domElem.text()).to.eq('Filter Name: testName 1')
            });

        });

        describe('C модификатором multi', function() {

            it('Кнопка должна содержать текст Filter Name: testName 1, testName 3', function() {
                options[1].selected = 'yes';
                options[3].selected = 'yes';

                createBlock(text, { multi: 'no' }, options);

                expect(block.getSwitcher().domElem.text()).to.eq('Filter Name: testName 1, testName 3')
            });

        });

        describe('C модификатором group-limit', function() {

            beforeEach(function() {
                options = [
                    { text: 'testName', value: 'testValue', group: 'group1' },
                    { text: 'testName 1', value: 'testValue1', group: 'group2' },
                    { text: 'testName 2', value: 'testValue2', group: 'group1' },
                    { text: 'testName 3', value: 'testValue3', group: 'group2' },
                    { text: 'testName 4', value: 'testValue4', group: 'group3' },
                    { text: 'testName 5', value: 'testValue5', group: 'group3' },
                    { text: 'testNameDisabled', value: 'testValueDisabled', group: 'group4' }
                ];
            });

            it('Должен выключить все варианты, которые не входят в одну выбранную', function() {
                options[0].selected = 'yes'; // выбрали из группы 1
                createBlock(text, { multi: 'yes', 'group-limit': 1 }, options);

                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);

                var elemsToCheck = ['testValue1', 'testValue3', 'testValue4', 'testValue5', 'testValueDisabled'];

                expect(checkDisabledElements(elemsToCheck)).to.not.contain(false);
            });

            it('Должен выключить все варианты, которые не входят в две выбранные', function() {
                options[0].selected = 'yes';
                options[1].selected = 'yes'; // выбрали из группы 1 и 2

                createBlock(text, { multi: 'yes', 'group-limit': 2 }, options);

                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);

                var elemsToCheck = ['testValue4', 'testValue5', 'testValueDisabled'];

                expect(checkDisabledElements(elemsToCheck)).to.not.contain(false);
            });

        });
    });

    describe('Поведение', function() {
        var options,
            text;

        beforeEach(function() {
            text = 'Filter Name';
            options = [
                { text: 'testName', value: 'testValue' },
                { text: 'testName 1', value: 'testValue1', selected: 'yes' },
                { text: 'testName 2', value: 'testValue2', disabled: 'yes' },
                { text: 'testName 3', value: 'testValue3' },
                { text: 'testName 4', value: 'testValue4' },
                { text: 'testName 5', value: 'testValue5' },
                { text: 'testNameDisabled', value: 'testValueDisabled' }
            ];
        });

        afterEach(function() {
            block.destruct && block.destruct();
        });

        describe('Общее', function() {

            it('При нажатии на кнопку, должен открыться попап', function() {
                createBlock(text, { multi: 'no' }, options);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);

                expect(block.getPopup()).to.haveMod('visible', 'yes');
            });

            it('При изменении значения, должен тригерить событие change', function() {
                createBlock(text, { multi: 'no' }, options);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);

                expect(block).to.triggerEvent('change', function() {
                    block._chooser.check('test-value5');
                });
            });

            it('Если отключить последний пункт, должен изменить текст в кнопке на Filter Name: не выбран', function() {
                createBlock(text, { multi: 'yes' }, options);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                block._chooser.uncheck('test-value1');

                expect(block.getSwitcher().domElem.text()).to.eq('Filter Name: не выбран');
            });

            it('Если есть модификатор disabled, то по нажатию на кнопку не должен открыться попап', function() {
                createBlock(text, { multi: 'no', disabled: 'yes' }, options);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);

                expect(block.getPopup()).to.not.haveMod('visible', 'yes');
            });

            it('Если модификатор disabled выставлен, то по нажатию на кнопку не должен открыться попап', function() {
                createBlock(text, { multi: 'no', disabled: 'no' }, options);
                block.setMod('disabled', 'yes');
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);

                expect(block.getPopup()).to.not.haveMod('visible', 'yes');
            });

            it('Если есть модификатор disabled и есть сообщение, должен при наведении показать его', function() {
                var messages = {
                    switcher: {
                        disabled: 'text'
                    }
                }
                createBlock(text, { multi: 'no', disabled: 'yes' }, options, messages);
                block.findElem('dropdown').trigger('mouseover');
                sandbox.clock.tick(500);

                expect(block._getTooltip().findBlockOn('popup2').hasMod('visible')).to.eq(true);
            });

            it('Если есть модификатор disabled и отсутствует сообщение, должен при наведении ничего не показывать', function() {
                var messages = {
                    switcher: {}
                }
                createBlock(text, { multi: 'no', disabled: 'yes' }, options, messages);
                block.findElem('dropdown').trigger('mouseover');
                sandbox.clock.tick(500);

                expect(block._getTooltip().findBlockOn('popup2').hasMod('visible')).to.eq(false);
            });

            it('Если есть модификатор disabled, должен при наведении показать сообщение c текстом text', function() {
                var messages = {
                    switcher: {
                        disabled: 'text'
                    }
                }
                createBlock(text, { multi: 'no', disabled: 'yes' }, options, messages);
                block.findElem('dropdown').trigger('mouseover');
                sandbox.clock.tick(500);

                expect(block._getTooltip().domElem.text()).to.eq('text');
            });

        });

        describe('Без модификатора multi', function() {

            it('При выставлении значения, должен изменить текст в кнопке Filter Name: не выбран -> Filter Name: testName 5', function() {
                options[1].selected = '';
                createBlock(text, { multi: 'no' }, options);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                block._chooser.check('test-value5');

                expect(block.getSwitcher().domElem.text()).to.eq('Filter Name: testName 5');
            });

            it('При изменении значения, должен изменить текст в кнопке Filter Name: testName 1 -> Filter Name: testName 5', function() {
                createBlock(text, { multi: 'no' }, options);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                block._chooser.check('test-value5');

                expect(block.getSwitcher().domElem.text()).to.eq('Filter Name: testName 5');
            });

        });

        describe('C модификатором multi', function() {

            it('При выставлении значения, должен изменить текст в кнопке Filter Name: не выбран -> Filter Name: testName 5', function() {
                options[1].selected = '';
                createBlock(text, { multi: 'yes' }, options);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                block._chooser.check('test-value5');

                expect(block.getSwitcher().domElem.text()).to.eq('Filter Name: testName 5');
            });

            it('При изменении значения, должен изменить текст в кнопке Filter Name: testName 1 -> Filter Name: testName 1, testName 5', function() {
                createBlock(text, { multi: 'yes' }, options);
                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                block._chooser.check('test-value5');

                expect(block.getSwitcher().domElem.text()).to.eq('Filter Name: testName 1, testName 5');
            });

        });

        describe('C модификатором group-limit', function() {

            beforeEach(function() {
                options = [
                    { text: 'testName', value: 'testValue', group: 'group1' },
                    { text: 'testName 1', value: 'testValue1', group: 'group2' },
                    { text: 'testName 2', value: 'testValue2', group: 'group1' },
                    { text: 'testName 3', value: 'testValue3', group: 'group2' },
                    { text: 'testName 4', value: 'testValue4', group: 'group3' },
                    { text: 'testName 5', value: 'testValue5', group: 'group3' },
                    { text: 'testNameDisabled', value: 'testValueDisabled', group: 'group4' }
                ];
            });

            it('Должен выключить все варианты, которые не входят в одну выбранную', function() {
                createBlock(text, { multi: 'yes', 'group-limit': 1 }, options);

                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                block._chooser.elem('item', 'name', u.beminize('testValue')).click(); // выбераем первый
                sandbox.clock.tick(500);

                var elemsToCheck = ['testValue1', 'testValue3', 'testValue4', 'testValue5', 'testValueDisabled'];

                expect(checkDisabledElements(elemsToCheck)).to.not.contain(false);
            });

            it('Должен выключить все варианты, которые не входят в две выбранные', function() {
                options[0].selected = 'yes'; // изначально выбран первый из группы 1
                createBlock(text, { multi: 'yes', 'group-limit': 2 }, options);

                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);
                block._chooser.elem('item', 'name', u.beminize('testValue1')).click(); // выбераем из второй группы
                sandbox.clock.tick(500);

                var elemsToCheck = ['testValue4', 'testValue5', 'testValueDisabled'];

                expect(checkDisabledElements(elemsToCheck)).to.not.contain(false);
            });

            it('Для выключенного варианта должен показать сообщение', function() {
                options[0].selected = 'yes'; // выбрали из группы 1 и 2
                options[1].selected = 'yes'; // выбрали из группы 1 и 2
                createBlock(text, { multi: 'yes', 'group-limit': 2 }, options, { 'filter-item': { 'group-limit': 'test' }});

                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);

                block._chooser.elem('item', 'name', u.beminize('testValue4')).trigger('mouseover');
                sandbox.clock.tick(500);

                expect(block._getTooltip().findBlockOn('popup2').hasMod('visible')).to.eq(true);
            });

            it('Для выключенного варианта должен показать сообщение c текстом "test"', function() {
                options[0].selected = 'yes'; // выбрали из группы 1 и 2
                options[1].selected = 'yes'; // выбрали из группы 1 и 2
                createBlock(text, { multi: 'yes', 'group-limit': 2 }, options, { 'filter-item': { 'group-limit': 'test' }});

                block.getSwitcher().domElem.click();
                sandbox.clock.tick(500);

                block._chooser.elem('item', 'name', u.beminize('testValue4')).trigger('mouseover');
                sandbox.clock.tick(500);

                expect(block._getTooltip().domElem.text()).to.eq('test');
            });

        });

    });

});

