describe('b-group-tags2-edit', function() {
    var block,
        sandbox,
        ctx = {
            block: 'b-group-tags2-edit',
            selectedTags: ['стакан'],
            availableTags: ['стакан', 'кефир']
        },
        createBlock = function(extendedCtx) {
            block = u.createBlock(u._.extend({}, ctx, extendedCtx));
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('Верстка', function() {
        it('Если выбранного тега нет в списке доступных, он не отрисовывается', function() {
            var checkboxesNames;

            createBlock({ selectedTags: ['стакан', 'ряженка'] });

            checkboxesNames = block.findBlocksInside('checkbox')
                .reduce(function(res, checkbox) {
                    checkbox.isChecked() && res.push(checkbox.elem('label').text());
                    return res;
                }, []);
            expect(checkboxesNames).to.deep.equal(['стакан']);
        });

        it('Чекбоксы отсортированы по алфавиту', function() {
            var checkboxesNames;

            createBlock();

            checkboxesNames = block.findBlocksInside('checkbox')
                .map(function(checkbox) {
                    return checkbox.elem('label').text();
                });
            expect(checkboxesNames).to.deep.equal(['кефир', 'стакан']);
        });
        it('Правильно формируются данные для блока i-selectable', function() {
            createBlock();

            expect(block.findBlockInside('i-selectable').params.items).to.deep.equal({
                кефир: {
                    data: { tag: 'кефир' },
                    isSelected: false
                },
                стакан: {
                    data: { tag: 'стакан' },
                    isSelected: true
                }
            })
        });
        it('если приходит useBanner: true, то заголовк "Метки баннера"', function() {
            createBlock({ useBanner: true });

            expect(block.elem('header').text()).to.equal('Метки баннера');
        });
        it('если приходит useBanner: false, то заголовк "Метки объявления"', function() {
            createBlock();

            expect(block.elem('header').text()).to.equal('Метки объявления');
        });
    });

    describe('События', function() {
        beforeEach(function() {
            createBlock();
        });

        it('При чекании тега он добавляется в список выбранных в модели ', function() {
            var unchecked = block.findBlocksInside('checkbox')
                .filter(function(checkbox) {
                    return !checkbox.isChecked();
                });

            unchecked[0].toggle();

            expect(block.model.get('selectedTags')).to.deep.equal({ стакан: true, кефир: true });
        });
        it('При отчекивании тега он удаляется из списка выбранных в модели', function() {
            var checked = block.findBlocksInside('checkbox')
                .filter(function(checkbox) {
                    return checkbox.isChecked();
                });

            checked[0].toggle();

            expect(block.model.get('selectedTags')).to.deep.equal({ стакан: false, кефир: false });
        });

        it('При добавлении нового тега он добавляется в список новых в модели', function() {
            block.findBlockInside('input').val('булочка');

            expect(block.model.get('newTags').toString()).to.equal('булочка');
        });

        it('При добавлении новых тегов они добавляются в список новых в модели', function() {
            block.findBlockInside('input').val('булочка, полбатона');

            expect(block.model.get('newTags').toString()).to.equal('булочка,полбатона');
        });

        it('При нажатии на enter на инпуте триггерится событие save', function() {
            var e = $.Event('keypress');

            e.which = 13;
            sandbox.spy(block, 'trigger');

            block.findBlockInside('input').elem('control').trigger(e);

            expect(block.trigger.calledWith('save')).to.be.true;
        });

        it('При нажатии на кнопку "Сохранить" триггерится событие save', function() {
            sandbox.spy(block, 'trigger');

            block.findBlockOn('save', 'button').trigger('click');

            expect(block.trigger.calledWith('save')).to.be.true;
        });
        it('При нажатии на кнопку "Отмена" триггерится событие cancel', function() {
            sandbox.spy(block, 'trigger');

            block.findBlockOn('cancel', 'button').trigger('click');

            expect(block.trigger.calledWith('cancel')).to.be.true;
        });

        it('При чекании тега триггерится событие tagsChanged', function() {
            var unchecked = block.findBlocksInside('checkbox')
                .filter(function(checkbox) {
                    return !checkbox.isChecked();
                });

            sandbox.spy(block, 'trigger');

            unchecked[0].toggle();

            expect(block.trigger.calledWith('tagsChanged')).to.be.true;
        });
        it('При отчекивании тега триггерится событие tagsChanged', function() {
            var checked = block.findBlocksInside('checkbox')
                .filter(function(checkbox) {
                    return checkbox.isChecked();
                });

            sandbox.spy(block, 'trigger');

            checked[0].toggle();

            expect(block.trigger.calledWith('tagsChanged')).to.be.true;
        });
        it('При добавлении нового тега триггерится событие tagsChanged', function() {
            sandbox.spy(block, 'trigger');

            block.findBlockInside('input').val('булочка, полбатона');

            expect(block.trigger.calledWith('tagsChanged')).to.be.true;
        });
    });

    describe('Парсинг новых тегов', function() {
        beforeEach(function() {
            createBlock();
        });

        ['Тег1,тег2', 'Тег1, тег2', 'Тег1 , тег2', 'Тег1 ,, тег2', 'Тег1, , , тег2',
            ',Тег1, тег2,', ' Тег1, тег2 ', ' , Тег1, тег2 , ', ', Тег1, тег2 ,'].forEach(function(test) {
                it('При вводе ' + test + ' в модель записывается "Тег1,тег2"', function() {
                    block.findBlockInside('input').val(test);

                    expect(block.model.get('newTags').toString()).to.equal('Тег1,тег2');
                });
        });
    });

    describe('Публичные методы и модификаторы', function() {
        beforeEach(function() {
            createBlock();
        });

        it('Если ставится state: "", то кнопка сохранения энейблится', function() {
            block.delMod('state');

            expect(block.findBlockOn('save', 'button')).not.to.haveMod('disabled');
        });
        it('Если ставится state: "saveDisabled", то кнопка сохранения энейблится', function() {
            block.setMod('state', 'saveDisabled');

            expect(block.findBlockOn('save', 'button')).to.haveMod('disabled');
        });
        it('Метод setError рисует сообщение об ошибке, если есть текст ошибки', function() {
            block.setError('Ошибка');

            expect(block.findElem('errors').text()).to.equal('Ошибка');
        });
        it('Метод setError ставит state: "saveDisabled", если есть текст ошибки', function() {
            block.setError('Ошибка');

            expect(block).to.haveMod('state', 'saveDisabled');
        });
        it('Метод setError стирает сообщение об ошибке, если нет текста ошибки', function() {
            block.setError('Ошибка');
            expect(block.findElem('errors').text()).to.equal('Ошибка');

            block.setError('');
            expect(block.findElem('errors').text()).to.equal('');
        });
        it('Метод setError энейблит кнопку сохранения, если нет текста ошибки', function() {
            block.setError('Ошибка');
            expect(block).to.haveMod('state', 'saveDisabled');

            block.setError('');
            expect(block).not.to.haveMod('state');
        });

        it('Метод getValue возвращает выбранные и новые теги', function() {
            var unchecked = block.findBlocksInside('checkbox')
                .filter(function(checkbox) {
                    return !checkbox.isChecked();
                });
            unchecked[0].toggle();

            block.findBlockInside('input').val('булочка');

            expect(block.getValue()).to.deep.equal({ newTags: ['булочка'], tags: ['стакан', 'кефир', 'булочка'] });
        });
    });

    describe('Модель', function() {
        beforeEach(function() {
            createBlock();
        });

        it('Поле tags формируется из списка выбранных и новых тегов', function() {
            var unchecked = block.findBlocksInside('checkbox')
                .filter(function(checkbox) {
                    return !checkbox.isChecked();
                });
            unchecked[0].toggle();
            block.findBlockInside('input').val('булочка');

            expect(block.model.get('tags').toString()).to.deep.equal('стакан,кефир,булочка');
        });
        it('Метод getSelectedTags возвращает список только выбранных тегов', function() {
            expect(block.model.getSelectedTags()).to.deep.equal(['стакан']);
        });
        it('Метод getNewTags возвращает новые теги, которых нет в списке доступных', function() {
            block.findBlockInside('input').val('стакан, булочка');

            expect(block.model.getNewTags()).to.deep.equal(['булочка']);
        });
    });
});
