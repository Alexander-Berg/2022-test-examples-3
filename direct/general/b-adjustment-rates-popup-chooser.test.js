describe('b-adjustment-rates-popup-chooser', function() {
    /**
     * b-adjustment-rates-popup-chooser использует b-chooser
     * выбранное условие содержат модификатор selected yes
     * созданному условию выставляется модификатор disabled yes и оно скрывается визуально(так же игнорируется поиском)
     * у созданного условия остается модификатор selected yes
     * условие считается созданным если его выбрали и нажали кнопку «Создать корректировок — n»
     */
    var block,
        clock;

    function blockBuilder() {
        return {
            jsonTree: {
                block: 'b-adjustment-rates-popup-chooser',
                limit: 2,
                conditions: [],
                selected: []
            },
            generateConditions: function(count) {
                var conditions = [];

                if (count === undefined) {
                    count = 1;
                }

                while (count--) {
                    conditions.push({
                        retargetingId: 'new-id-' + count,
                        name: 'new-item-' + count,
                        isAccessible: 1
                    });
                }

                return {
                    all: conditions,
                    byId: u._.indexBy(conditions),
                    count: conditions.length,
                    accessibleCount: conditions.length
                };
            },
            set: function(options) {
                if (options.type) {
                    this.jsonTree.mods = { type: options.type };
                }

                if (options.conditionsCount) {
                    this.jsonTree.conditions = this.generateConditions(options.conditionsCount);
                }

                return this;
            },
            create: function() {
                return this.block = BEM.DOM
                    .init(u.getDOMTree(this.jsonTree).appendTo('body'))
                    .bem('b-adjustment-rates-popup-chooser');
            }
        }
    }

    afterEach(function() {
        block.getPopup().hide();
        clock.tick(0);

        BEM.DOM.destruct(block.domElem);

        clock.restore();
    });

    describe('public-методы', function() {

        beforeEach(function() {
            clock = sinon.useFakeTimers();
            block = blockBuilder()
                .set({ type: 'add', conditionsCount: 1 })
                .create()
                .toggle($('<div>').appendTo('body'));
        });

        describe('toggle', function() {
            it('должен открыть попап', function() {
                expect(block.getPopup().isShown()).to.equal(true);
            });
        });

        describe('canAdd', function() {
            it([
                'должен возвращать true',
                'если возможно создать условие(есть условия без модификатор disabled yes)'
            ].join(' '), function() {
                expect(block.canAdd()).to.equal(true);
            });
            it([
                'должен возвращать false',
                'если невозможно создать условие (все содержат модификатор disabled yes)'
            ].join(' '), function() {
                block.getChooser().check('new-id-0');
                block.findBlockInside('add', 'button').trigger('click');

                expect(block.canAdd()).to.equal(false);
            });
        });

    });

    describe('модификатор type add — мультивыбор условий', function() {

        describe('в интерфейсе', function() {

            describe('', function() {

                beforeEach(function() {
                    clock = sinon.useFakeTimers();
                    block = blockBuilder()
                        .set({ type: 'add', conditionsCount: 1 })
                        .create()
                        .toggle($('<div>').appendTo('body'));
                });

                it('должна быть неактивна кнопка «Создать корректировок — n», если нет выбранных условий', function() {
                    expect(block.findBlockInside('add', 'button')).to.haveMod('disabled', 'yes');
                });
                it('должна быть неактивна кнопка «Создать корректировок — n», после создания условий', function() {
                    block.getChooser().check('new-id-0');
                    block.findBlockInside('add', 'button').trigger('click');

                    expect(block.findBlockInside('add', 'button')).to.haveMod('disabled', 'yes');
                });
                it('должно выводится количество выбранных условий «Создать корректировок — n»', function() {
                    block.getChooser().check('new-id-0');
                    expect(block.findElem('add-counter').text()).to.equal(' — 1');
                });

                it('должен отсутствовать indeterminate yes у блока checkbox, если не выбраны элементы', function() {
                    block.getChooser()
                        .check('new-id-0')
                        .uncheck('new-id-0');

                    expect(block.findBlockInside('mass-action', 'checkbox')).to.not.haveMod('indeterminate', 'yes');
                });
                it('должен отсутствовать модификатор checked yes у блока checkbox, если не выбраны элементы', function() {
                    block.getChooser()
                        .check('new-id-0')
                        .uncheck('new-id-0');

                    expect(block.findBlockInside('mass-action', 'checkbox')).to.not.haveMod('checked', 'yes');
                });
                it('созданные условия должны быть неактивными', function() {
                    block.getChooser().check('new-id-0');
                    block.findBlockInside('add', 'button').trigger('click');

                    block.getChooser().getAll().map(function(item) {
                        expect(item.disabled).to.equal(true);
                    });
                });
                it('после закрытия у выбранных условий должен сбрасываться флажок(не относится к созданным)', function() {
                    block.getChooser().check('new-id-0');
                    block.getPopup().hide();

                    block.getChooser().getAll().map(function(item) {
                        expect(item.selected).to.equal(false);
                    });
                });

                describe('если количество условий не превышает указанный лимит или равно ему', function() {
                    it('должна отсутствовать подсказка «Будет выбрано n условий» под групповым чекбоксом', function() {
                        expect(block).to.not.haveElems('help-text');
                    });
                    it('должны быть выбраные все доступные условия, если выбран checkbox', function() {
                        block.findBlockInside('mass-action', 'checkbox').setMod('checked', 'yes');
                        clock.tick(0);

                        expect(block.getChooser().getAll()).to.eql(block.getChooser().getSelected());
                    });
                    it('должен быть выбран checkbox, если выбраны все условия', function() {
                        block.getChooser().check('new-id-0');

                        expect(block.findBlockInside('mass-action', 'checkbox')).to.haveMod('checked', 'yes');
                    });
                });
            });

            describe('', function() {

                beforeEach(function() {
                    block = blockBuilder()
                        .set({ type: 'add', conditionsCount: 10 })
                        .create()
                        .toggle($('<div>').appendTo('body'));
                });

                it('должна показываться строка поиска, если количество видимых условий больше или равно 10', function() {
                    expect(block).to.not.haveMod('search', 'hidden');
                });
                it('не должна показываться строка поиска, если количество видимых условий меньше 10', function() {
                    block.getChooser().check('new-id-0');
                    block.findBlockInside('add', 'button').trigger('click');
                    block.toggle();

                    expect(block).to.haveMod('search', 'hidden');
                });
                it([
                    'должны быть выставлен модификатор indeterminate_yes блоку checkbox',
                    'если выбраны не все элементы'
                ].join(' '), function() {
                    block.getChooser().check('new-id-0');
                    expect(block.findBlockInside('mass-action', 'checkbox')).to.haveMod('indeterminate', 'yes');
                });
            });

            describe('количество условий превышает указанный лимит', function() {

                beforeEach(function() {
                    clock = sinon.useFakeTimers();
                    block = blockBuilder()
                        .set({ type: 'add', conditionsCount: 3 })
                        .create()
                        .toggle($('<div>').appendTo('body'));
                });

                it('должна быть подсказка «Будет выбрано n условий» под блоком checkbox', function() {
                    expect(block).to.haveElem('help-text', 1);
                });
                it([
                    'должна быть неактивна кнопка «Создать корректировок — n»',
                    '(выбрано больше чем возможно добавить)'
                ].join(' '), function() {
                    block.getChooser()
                        .check('new-id-0')
                        .check('new-id-1')
                        .check('new-id-2');

                    expect(block.findBlockInside('add', 'button')).to.haveMod('disabled', 'yes');
                });
                it([
                    'должны быть выбраны первые n условий(из доступных) не превышая лимит',
                    'если выбран checkbox'
                ].join(' '), function() {
                    block.findBlockInside('mass-action', 'checkbox').setMod('checked', 'yes');
                    clock.tick(0);

                    expect(block.getChooser().getAll().slice(0, 2)).to.eql(block.getChooser().getSelected());
                });
            });

            describe('поиск', function() {

                beforeEach(function() {
                    clock = sinon.useFakeTimers();
                    block = blockBuilder()
                        .set({ type: 'add', conditionsCount: 3 })
                        .create()
                        .toggle($('<div>').appendTo('body'));

                    block.getChooser().search('new-item-0');
                });

                it('должны быть выбраны только видимые условия, если выбран checkbox', function() {
                    clock.tick(0);
                    block.findBlockInside('mass-action', 'checkbox').setMod('checked', 'yes');
                    clock.tick(0);

                    var itemParams = {
                        extraParams: {},
                        text: 'new-item-0',
                        condition: {
                            retargetingId: 'new-id-0',
                            name: 'new-item-0',
                            isAccessible: 1
                        },
                        name: 'new-id-0',
                        originalName: 'new-id-0',
                        selected: true,
                        disabled: false,
                        indeterminate: false,
                        hidden: false
                    };

                    expect([itemParams]).to.eql(block.getChooser().getSelected());
                });
                it('должен сбрасываться поиск после закрытия попапа', function() {
                    block.getPopup().hide();
                    clock.tick(0);

                    block.getChooser().getAll().map(function(item) {
                        expect(item.hidden).to.equal(false);
                    });
                });
                it([
                    'должен быть выставлен модификатор indeterminate_yes блоку checkbox',
                    'если до поиска checkbox содержал модификатор checked_yes',
                    'и после нового поиска появились доступные условия'
                ].join(' '), function() {
                    clock.tick(0);
                    block.findBlockInside('mass-action', 'checkbox').setMod('checked', 'yes');
                    clock.tick(0);
                    block.getChooser().search('');
                    clock.tick(0);

                    expect(block.findBlockInside('mass-action', 'checkbox')).to.haveMod('indeterminate', 'yes');
                });
                it([
                    'не должно быть модификатора indeterminate_yes у блока checkbox',
                    'если до поиска checkbox содержал модификатор indeterminate_yes',
                    'и после нового поиска видны только не выбраные условия'
                ].join(' '), function() {
                    block.getChooser()
                        .check('new-id-0')
                        .search('new-id-1');

                    clock.tick(0);

                    expect(block.findBlockInside('mass-action', 'checkbox')).to.not.haveMod('indeterminate', 'yes');
                });
                it([
                    'не должно быть модификатора checked yes у блока checkbox',
                    'если до поиска checkbox содержал модификатор checked_yes',
                    'и после нового поиска видны только не выбраные условия'
                ].join(' '), function() {
                    block.getChooser().search('new-id-1');
                    block.findBlockInside('mass-action', 'checkbox').setMod('checked', 'yes');
                    block.getChooser().search('new-id-0');
                    clock.tick(0);

                    expect(block.findBlockInside('mass-action', 'checkbox')).to.not.haveMod('checked', 'yes');
                });

            });

        });

        describe('события', function() {

            beforeEach(function() {
                clock = sinon.useFakeTimers();
                block = blockBuilder()
                    .set({ type: 'add', conditionsCount: 1 })
                    .create()
                    .toggle($('<div>').appendTo('body'));

                block.getChooser().search('new-item-0');
            });

            afterEach(function() {
                block.trigger.restore();
            });

            it('должно происходить событие add по нажатию кнопки «Создать корректировок — n» на блоке', function() {
                sinon.spy(block, 'trigger');

                block.getChooser().check('new-id-0');
                block.findBlockInside('add', 'button').trigger('click');

                expect(block.trigger.calledWith('add')).to.equal(true);
            });
            it('должны содержатся новые условия в событии add', function() {
                sinon.spy(block, 'trigger');

                block.getChooser().check('new-id-0');
                block.findBlockInside('add', 'button').trigger('click');

                block.trigger.firstCall.args[1].newItems.map(function(item) {
                    expect(item.name).to.equal('new-id-0');
                });
            });

        });

    });

    describe('модификатор type editing — редактирование условия', function() {

        describe('в интерфейсе', function() {

            describe('', function() {

                beforeEach(function() {
                    block = blockBuilder()
                        .set({ type: 'editing', conditionsCount: 2 })
                        .create();

                    block.getChooser().check('new-id-0');
                    block.toggle($('<div>').appendTo('body'), 'new-id-0');
                });

                it('должен пересохранить условие, если выбрали другое и нажали «Сохранить корректировку»', function() {
                    block.getChooser().check('new-id-1');
                    block.findBlockInside('save', 'button').trigger('click');

                    block.getChooser().getSelected().map(function(item) {
                        expect(item.name).to.equal('new-id-1');
                    });
                });
                it('должен показать созданное условие', function() {
                    block.getChooser().getSelected().map(function(item) {
                        expect(item.disabled).to.equal(false);
                    });
                });
                it('должно быть выбрано созданное условие', function() {
                    block.getChooser().getSelected().map(function(item) {
                        expect(item.selected).to.equal(true);
                    });
                });
                it('не должна показываться строка поиска', function() {
                    expect(block).to.haveMod('search', 'hidden');

                });
                it([
                    'должна быть неактивной кнопка «Сохранить корректировку»',
                    'если выбрали то же условие на котором открыли попап'
                ].join(' '), function() {
                    block.getChooser().check('new-id-1');
                    block.getChooser().check('new-id-0');

                    expect(block.findBlockInside('save', 'button')).to.haveMod('disabled', 'yes');
                });
            });

            describe('', function() {

                beforeEach(function() {
                    block = blockBuilder()
                        .set({ type: 'editing', conditionsCount: 1 })
                        .create();

                    block.getChooser().check('new-id-0');
                    block.toggle($('<div>').appendTo('body'), 'new-id-0');
                });

                it([
                    'не должна показываться кнопка «Сохранить корректировку»',
                    'если нет доступных для выбора условий'
                ].join(' '), function() {
                    expect(block).to.haveMod('buttons-wrap', 'hidden');
                });

            });
        });

        describe('события', function() {

            beforeEach(function() {
                block = blockBuilder()
                    .set({ type: 'editing', conditionsCount: 2 })
                    .create();

                block.getChooser().check('new-id-0');
                block.toggle($('<div>').appendTo('body'), 'new-id-0');
            });

            afterEach(function() {
                block.trigger.restore();
            });

            it('должно происходить событие save по нажатию кнопки «Сохранить корректировку» на блоке', function() {
                sinon.spy(block, 'trigger');

                block.getChooser().check('new-id-0');
                block.findBlockInside('save', 'button').trigger('click');

                expect(block.trigger.calledWith('save')).to.equal(true);
            });
            it('должен содержатся name нового условия в событии save', function() {
                sinon.spy(block, 'trigger');

                block.getChooser().check('new-id-1');
                block.findBlockInside('save', 'button').trigger('click');

                expect(block.trigger.firstCall.args[1].newName).to.equal('new-id-1');
            });
            it('должен содержатся name предыдущего условия в событии save', function() {
                sinon.spy(block, 'trigger');

                block.getChooser().check('new-id-1');
                block.findBlockInside('save', 'button').trigger('click');

                expect(block.trigger.firstCall.args[1].prevName).to.equal('new-id-0');
            });

        });

    });

});
