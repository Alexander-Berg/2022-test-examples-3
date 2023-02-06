describe('b-checkboxes-group_ignored_yes', function() {
    var block,
        sandbox,
        /**
         * Функция для генерации блока
         * @param count {Number} количество чекбоксов
         * @param checkedMap {Object} объект вида {0: true, 2: true, ... n: true} с выбранными чекбоксами
         * @param ignoredMap {Object} объект для обозначения ignored чекбоксов
         * @returns {*|BEM.DOM}
         */
        createBlock = function(count, checkedMap, ignoredMap) {
            var ctx = {
                    block: 'b-checkboxes-group',
                    content: [],
                    mods: { ignored: 'yes' }
                };
                checkedMap = checkedMap || {},
                ignoredMap = ignoredMap || {};

            for (var i = 1; i <= count; i++) {
                ctx.content.push({
                    block: 'checkbox',
                    mods: { checked: checkedMap[i] ? 'yes' : ''},
                    mix: [
                        {
                            block: 'b-checkboxes-group',
                            elem: 'item',
                            elemMods: { number: i }
                        },
                        ignoredMap[i] ?
                        {
                            block: 'ignored'
                        } :
                        ''
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
        },
        block,
        sandbox;

    beforeEach(function() {
        block = createBlock(4);
        sandbox = sinon.sandbox.create({ useFakeTimers: true});
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    it('Выбираем чекбокс без ignore, и он должен вернуться в выбранных чекбоксах', function() {
        var chbx = block.findBlockOn(block.elem('item', 'number', '1'), 'checkbox');
        chbx.setMod('checked', 'yes');
        expect(chbx).to.be.oneOf(block.getChecked());
    });

    it('Задаем чекбоксу ignore, выбираем его, и он не должен вернуться в выбранных чекбоксах', function() {
        var chbx = block.findBlockOn(block.elem('item', 'number', '1'), 'checkbox');
        block.setIgnored({ 0: true });
        chbx.setMod('checked', 'yes');
        expect(chbx).not.to.be.oneOf(block.getChecked());
    });

    it('Переключение общего чекбокса не влияет на ignored чекбоксы', function() {
        var mainChbx = block.findBlockInside('checkbox');
        block.setIgnored({ 0: true });
        mainChbx.setMod('checked', 'yes');
        block.rearrange();
        expect(block.findBlockOn(block.elem('item', 'number', '1'), 'checkbox'))
            .not.to.haveMod('checked', 'yes');
    });

    it('Переключение ignored чекбокса не влияет на общий', function() {
        var mainChbx = block.findBlockOn(block.elem('item', 'groupCheckbox', 'yes'), 'checkbox'),
            ignoredChbx = block.findBlockOn(block.elem('item', 'number', '1'), 'checkbox');

        block.setIgnored({ 0: true });
        block.findBlocksInside('checkbox').forEach(function(item) {
            item.setMod('checked', 'yes');
        });
        block.rearrange(); //используется в js коде блока

        expect(mainChbx).to.haveMod('checked', 'yes');

        ignoredChbx.setMod('checked', '');
        ignoredChbx.trigger('click'); // триггерим клик - имитируем действия пользователя

        sandbox.clock.tick(100);

        expect(mainChbx).to.haveMod('checked', 'yes');
    });

});
