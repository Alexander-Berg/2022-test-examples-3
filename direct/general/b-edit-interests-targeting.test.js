describe('b-edit-interests-targeting', function() {
    var block,
        chooser,
        sandbox,
        createBlock = function() {
            var block = u.createBlock({
                block: 'b-edit-interests-targeting'
            });

            block.init({
                    401: {
                        id: '401',
                        name: 'Игры',
                        available: false,
                        orderIndex: '2600'
                    },
                    406: {
                        id: '406',
                        name: 'Социальные',
                        available: true,
                        orderIndex: '2400'
                    },
                    301: {
                        id: '301',
                        name: 'Книги',
                        available: true,
                        orderIndex: '2200'
                    },
                    396: {
                        id: '396',
                        name: 'Словесные',
                        available: true,
                        orderIndex: '2658',
                        parentId: '401'
                    },
                    391: {
                        id: '391',
                        name: 'Викторины',
                        available: true,
                        orderIndex: '2655',
                        parentId: '401'
                    }
                },
                ['301', '396']);

            return block;
        };

    beforeEach(function() {
        block = createBlock();
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        chooser = block.findBlockInside('chooser', 'b-chooser');
    });

    afterEach(function() {
        sandbox.restore();
        block.destruct();
    });

    describe('Блок корректно инициализируется', function() {
        var chooserItems;

        beforeEach(function() {
            chooserItems = chooser.getAll();
        });

        it('Cписок категорий интересов для выбора не пустой', function() {
            expect(chooserItems !== 0).to.be.true;
        });

        it('Cписок категорий интересов содержит только варианты с флагом available = true', function() {
            expect(chooserItems.filter(function(chooserItem) {
                    return chooserItem.name === '401';
                }).length === 0).to.be.true;
        });

        it('Cписок категорий интересов для выбора отсортирован по полю orderIndex', function() {
            expect(chooserItems[0].name).to.be.eql('301');
            expect(chooserItems[chooserItems.length-1].name).to.be.eql('396');
        });

        it('Выбраны категории указынные активными при инициализации', function() {
            expect(chooser.getSelected().map(function(item) { return item.name; })).to.be.eql(['301', '396']);
        });
    });

    describe('Панель выбранных категорий', function() {
        it('Корректно инициализируется', function() {
            expect(block.elem('selected-item')).to.have.length(2);
        });

        describe('При изменении выбора элементов в b-chooser', function() {
            beforeEach(function() {
                chooser.check('406');
            });

            it('При выборе нового элемента список выбранных категорий корректно обновляется', function() {
                expect(block.elem('selected-item')).to.have.length(3);
            });

            it('При снятии выбора список выбранных категорий корректно обновляется', function() {
                chooser.uncheck('406');
                expect(block.elem('selected-item')).to.have.length(2);
            });

            it('Корректно обновляется информация о количестве выбранных интересов', function() {
                expect(block.elem('selected-items-num-value').text()).to.be.eql('3');
            });
        });

        describe('Удаление элемента из списка выбранных', function() {
            beforeEach(function() {
                block.findBlockOn(block.elem('selected-item'), 'b-edit-interests-targeting-item').trigger('delete', { id: '301'});
                sandbox.clock.tick(100);
            });

            it('Элемент удаляется из списка', function() {
                expect(block.findElem('selected-item')).to.have.length(1);
            });

            it('Соответсующий элемент в b-chooser перестает быть выбранным', function() {
                expect(chooser.getSelected().indexOf(function(item) {
                    return item.name === '301';
                })).to.be.eql(-1);
            });

            it('Корректно обновляется информация о количестве выбранных интересов', function() {
                expect(block.elem('selected-items-num-value').text()).to.be.eql('1');
            });
        });
    });

    describe('Кнопка "Очистить всё"', function() {
        var clearBtn;

        beforeEach(function() {
            block.findBlockOn('clear-items', 'link').trigger('click');

            sandbox.clock.tick(100);
        });

        it('Очищает список выбранных интересов', function() {
            expect(block.findElem('selected-item')).to.have.length(0);
        });

        it('Сбрасывает выбор в b-chooser', function() {
            expect(chooser.getSelected()).to.have.length(0);
        });
    });

    describe('Когда ничего не выбрано', function() {
        beforeEach(function() {
            chooser.uncheckAll();
            sandbox.clock.tick(100);
        });

        it('Кнопка "Очистить всё" недоступна', function() {
            expect(block.findBlockOn('clear-items', 'link')).to.haveMod('disabled', 'yes');
        });

        it('Показывается сообщение о том что ничего не выбрано', function() {
            expect(block).not.to.haveMod(block.elem('empty-list-text'), 'hidden');
        });

        it('Скрыта панель с информацией о количестве выбранных элементов', function() {
            expect(block).to.haveMod(block.elem('selected-items-num'), 'hidden', 'yes');
        });
    });

    describe('Кнопка "Сохранить"', function() {
        it('Задизэйблена при инициализации блока', function() {
            expect(block.findBlockOn('save', 'button')).to.haveMod('disabled', 'yes');
        });

        it('Становится доступной при изменении списка выбранных интересов', function() {
            chooser.check('406');
            expect(block.findBlockOn('save', 'button')).not.to.haveMod('disabled', 'yes');
        });
    });

    it('Блок корректно сохраняется', function() {
        var saveBtn = block.findBlockOn('save', 'button');

        sandbox.spy(block, 'trigger');

        chooser.check('406');
        saveBtn.trigger('click');

        expect(block.trigger.calledWith('save', { selectedIds: ['301', '406', '396'] })).to.be.true;
    });

    describe('Работа поиска интересов', function() {
        it('При задании текста элементы без такой подстроки получают модификатор visibility_hidden', function() {
            var elemsNoLetter = [
                chooser.elem('item', 'name', '301'), //книги
                chooser.elem('item', 'name', '391')  //викторины
            ];
            chooser.search('С');

            sandbox.clock.tick(100);

            var allInvisible = elemsNoLetter.every(function(elem) {
                return chooser.getMod(elem, 'visibility') == 'hidden';
            });

            expect(allInvisible).to.be.true;
        });

        it('При очищении строки поиска все элементы списка становятся видимы', function() {
            var elems = [
                chooser.elem('item', 'name', '301'), //книги
                chooser.elem('item', 'name', '406'), //спортивные
                chooser.elem('item', 'name', '396'), //словесные
                chooser.elem('item', 'name', '391')  //викторины
            ];

            chooser.search('');

            sandbox.clock.tick(100);

            var allVisible = elems.every(function(elem) {
                return chooser.getMod(elem, 'visibility') == '';
            });

            expect(allVisible).to.be.true;
        });

        it('Если заданной подстроки нет ни в 1 элементе, то все элементы становятся невидимы', function() {
            var elems = [
                chooser.elem('item', 'name', '301'), //книги
                chooser.elem('item', 'name', '406'), //спортивные
                chooser.elem('item', 'name', '396'), //словесные
                chooser.elem('item', 'name', '391')  //викторины
            ];

            chooser.search('У');

            sandbox.clock.tick(100);

            var allInvisible = elems.every(function(elem) {
                return chooser.getMod(elem, 'visibility') == 'hidden';
            });

            expect(allInvisible).to.be.true;
        });
    });

    it('При нажатии на ОТМЕНА триггерится событие', function() {
        sandbox.spy(block, 'trigger');
        chooser.check('406');

        block._cancelBtn.trigger('click');

        expect(block.trigger.calledWith('cancel')).to.be.true;
    });

    describe('Галочка ВЫБРАТЬ ВСЕ', function() {
        it('При отмечании галочки все элементы добавляются в список выбранных', function() {
            chooser.findBlockInside('select-all', 'checkbox').trigger('change', { checked: true });

            sandbox.clock.tick(100);

            expect(chooser.getSelected().length).to.equal(4);
            expect(block.findElem('selected-item')).to.have.length(4);
        });

        it('При снятии галочки все элементы удаляются из списка выбранных', function() {
            chooser.findBlockInside('select-all', 'checkbox').trigger('change', { checked: false });

            sandbox.clock.tick(100);

            expect(chooser.getSelected().length).to.equal(0);
            expect(block.findElem('selected-item')).to.have.length(0);
        })
    })
});
