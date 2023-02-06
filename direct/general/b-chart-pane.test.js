describe('b-chart-pane', function() {
    var block;

    beforeEach(function() {
    });

    afterEach(function() {
        block.destruct();
    });

    function createBlock(statData, points) {
        block = u.createBlock({
            block: 'b-chart-pane',
            statData: statData,
            points: points
        });
    }

    it('Корректно обрабатывается ситуация с нулевым массивом данных', function() {
        createBlock([], 0);
        BEM.blocks['b-confirm'].alert = sinon.spy();
        BEM.blocks['b-confirm'].alert.calledWith('Нет статистики для отображения');
    });

    it('Корректно обрабатывается ситуация с отсутствием массивом данных', function() {
        var fn = function() { createBlock(undefined, null) };
        expect(fn).to.throw('Не переданы данные для графиков');
    });
});
