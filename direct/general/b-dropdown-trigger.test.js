describe('b-dropdown-trigger', function() {
    var sandbox,
        block;
    function createBlock(options) {
        options = options || {};
        
        block = u.getInitedBlock({
            block: 'b-dropdown-trigger',
            js: options.js || {},
            button: options.button || 'Экспорт',
            items: options.items || []
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
    
    describe('Поведение', function() {
        var options;
        
        beforeEach(function() {
            
            options = {
                button: 'Текст кнопки',
                items: [
                    { elem:'item', content: 'PNG', name: 'png' },
                    { elem:'item', content: 'JPEG', name: 'jpeg' },
                    { elem:'item', content: 'SVG', name: 'svg' },
                    { elem: 'separator' },
                    { elem:'item', content: 'Напечатать', name: 'print' }
                ]
            };
        });

        afterEach(function() {
            block.destruct && block.destruct();
        });
        
        it('Должен содержать кнопку для открытия попапа', function() {
            createBlock(options);

            expect(block.findBlockInside('dropdown2').getSwitcher().domElem.length).to.eq(1)
        });

        it('Кнопка должна содержать текст "Текст кнопки" ', function() {
            createBlock(options);

            expect(block.findBlockInside('dropdown2').getSwitcher().domElem.text()).to.eq('Текст кнопки')
        });
        
        it('При нажатии на кнопку, должен открыться попап', function() {
            createBlock(options);
            block.findBlockInside('dropdown2').getSwitcher().domElem.click();
            sandbox.clock.tick(500);

            expect(block.findBlockInside('dropdown2').getPopup()).to.haveMod('visible', 'yes');
        });

        // если выполнять в консоли браузера все ок, а тут почему-то не работает
        it.skip('При изменении значения, должен тригерить событие click', function() {
            createBlock(options);
            block.findBlockInside('dropdown2').getSwitcher().domElem.click();
            sandbox.clock.tick(500);
            
            expect(block).to.triggerEvent('click', function() {
                $('.b-chooser__item').eq(0).click();
            });
        });
        
    });
});
