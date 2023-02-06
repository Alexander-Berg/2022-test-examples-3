describe('b-checkboxes-group', function() {
    var block,
        sandbox,
        /**
         * Функция для генерации блока
         * @param count {Number} количество чекбоксов
         * @param checkedMap {Object} объект вида {0: true, 2: true, ... n: true} с выбранными чекбоксами
         * @returns {*|BEM.DOM}
         */
        createBlock = function(count, checkedMap) {
            var ctx = {
                    block: 'b-checkboxes-group',
                    content: []
                };
                checkedMap = checkedMap || {};

            for (var i = 1; i <= count; i++) {
                ctx.content.push({
                    block: 'checkbox',
                    mods: { checked: checkedMap[i] ? 'yes' : ''},
                    mix: [
                        {
                            block: 'b-checkboxes-group',
                            elem: 'item',
                            elemMods: { number: i }
                        }
                    ]
                });
            }

            ctx.content.unshift({
                block: 'checkbox',
                mix: [
                    {
                        block: 'b-checkboxes-group',
                        elem: 'item',
                        elemMods: { groupCheckbox: 'yes' }
                    }
                ]

            });

            return  u.createBlock(ctx);
        };
    beforeEach(function() {
        block = createBlock(4);
        sandbox = sinon.sandbox.create({ useFakeTimers: true});
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('Проверка публичных функций', function() {
        it('getCheckboxes возвращает все чекбоксы кроме общего', function() {
            expect(block.findBlockOn(block.elem('item', 'groupCheckbox', 'yes'), 'checkbox'))
                .not.to.be.oneOf(block.getCheckboxes());
        });

        it('Список объектов без 1-го совпадает с getCheckboxes', function() {
            expect(block.findBlocksInside('checkbox').slice(1)).deep.equal(block.getCheckboxes());
        });

        it('getChecked возвращает выбранные чекбоксы', function() {
            var res;

            block.findBlockOn(block.elem('item', 'number', '1'), 'checkbox').setMod('checked', 'yes');
            res = block.getChecked().every(function(checkbox) {
                return checkbox.getMod('checked') == 'yes'
            });
            expect(res).to.be.true;
        });

        it('getChecked возвращает пустой массив, если ничего не выбрано', function() {
            expect(block.getChecked()).to.have.lengthOf(0);
        });

        it('unCheckAll сбрасывает checked у всех чекбоксов checked', function() {
            block.getCheckboxes().forEach(function(item) {
                item.setMod('checked', 'yes');
            });
            block.unCheckAll();
            expect(block.findBlocksInside({ block: 'checkbox', modName: 'checked', modVal: 'yes'}))
                .to.have.lengthOf(0);
        });

        it('rearrange даёт возможность перерисовки чекбоксов (если состояния checkbox-ов менялись js-ом)', function() {
            block.findBlocksInside('checkbox').forEach(function(item, i) {
                if (i > 0) item.setMod('checked', 'yes')
            });
            block.rearrange();
            expect(block.findBlockOn(block.elem('item', 'groupCheckbox', 'yes'), 'checkbox'))
                .to.haveMod('checked', 'yes');
        });
    });

    describe('Проверка поведения общего чекбокса (им становится первый переданный)', function() {
        it('Если никакие чекбоксы не выбраны, то при выборе общего чекбоска ' +
            'у всех чекбоксов в группе проставляется checked yes', function() {
            var mainChbx = block.findBlockOn(block.elem('item', 'groupCheckbox', 'yes'), 'checkbox'),
                res;
            mainChbx
                .setMod('checked', 'yes')
                .trigger('click');
            sandbox.clock.tick(100);

            res = block.getCheckboxes().every(function(checkbox) {
                return checkbox.getMod('checked') == 'yes'
            });

            expect(res).to.be.true;
        });

        it('При выборе всех чекбоксов поочередно, кроме общего,' +
            'общий чекбокс также выделяется', function() {
            block.findBlocksInside('checkbox').forEach(function(item, i) {
                if (i > 0) return false;
                item.setMod('checked', 'yes');
                item.trigger('click');
            });
            sandbox.clock.tick(100);
            expect(block.findBlockOn(block.elem('item', 'groupCheckbox', 'yes'), 'checkbox'))
                .to.haveMod('checked', 'yes');
        });

        it('Если все чекбоксы выбраны, и снимается выделение с одного необщего чекбокса' +
            'то checked у общего чекбокса пропадает', function() {
            var firstChbx = block.findBlockOn(block.elem('item', 'number', '1'), 'checkbox'),
                mainChbx = block.findBlockOn(block.elem('item', 'groupCheckbox', 'yes'), 'checkbox');
            mainChbx
                .setMod('checked', 'yes')
                .trigger('click');
            sandbox.clock.tick(100);
            expect(mainChbx).to.haveMod('checked', 'yes');

            firstChbx
                .setMod('checked', '')
                .trigger('click');
            sandbox.clock.tick(100);
            expect(mainChbx).not.to.haveMod('checked');
        });
    });
});
