describe('b-metrika-counters-popup-adapter', function() {
    var block,
        ctx = {
            block: 'b-metrika-counters-popup-adapter',
            metrikaCounters: '',
            OrderID: '0'
        },
        clock;

    beforeEach(function() {
        clock = sinon.useFakeTimers();
        block = u.createBlock(ctx);
     });

    afterEach(function() {
        clock.tick(1);
        block.destruct();
        clock.restore();
    });

    describe('Обновления при сохранении', function() {
        beforeEach(function() {
            block.findBlockInside('button').trigger('click');
        });

        it('Обновляется хинт со счетчиками метрики (при этом форматируется запись)', function() {
            block._notificationBlock.trigger('save', '1111 2222, 123');
            clock.tick(1);
            expect(block.elem('metrika-counters-string').text()).to.be.equal('1111, 2222, 123');
        });

        it('Обновляется кнопка', function() {
            block._notificationBlock.trigger('save', '1111 2222, 123');
            clock.tick(1);
            expect(block.findBlockInside('button').domElem.text()).to.be.equal('Изменить');
        });

        it('Обновляются скрытые инпуты (передается значение из инпута b-metrika-counters)', function() {
            block._notificationBlock.trigger('save', '1111 2222, 123');
            clock.tick(1);
            expect(block.elem('hidden-value').val()).to.be.equal('1111 2222, 123');
        });
    });
});
