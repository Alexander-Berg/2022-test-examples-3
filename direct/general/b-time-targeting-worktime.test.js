describe('b-time-targeting-worktime', function() {
    var block,
        sandbox,
        getSelect = function(name) {
            return block.findBlocksInside('select')[['dayFrom', 'dayTo', 'hourFrom', 'hourTo'].indexOf(name)];
        };

    function createBlock(data) {
        block = u.getInitedBlock({
            block: 'b-time-targeting-worktime',
            value: u['b-time-targeting'].parseToCode(data || {
                dayFrom: 1,
                dayTo: 5,
                hourFrom: 8,
                hourTo: 20
            })
        });

        sinon.spy(block, 'trigger');
    }

    function destructBlock() {
        block.trigger.restore();
        block.destruct();
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true, useFakeServer: true });
    });

    afterEach(function() {
        destructBlock();
        sandbox.restore();
    });

    it('После вызова init должны создаться селекты для выбора дня-времени', function() {
        createBlock();

        expect(block.findBlocksInside('select').length).to.be.equal(4);
    });

    it('Селекты должны проинициализировать с теми данными, которые были отправлены в начальных данных', function() {
        createBlock({
            dayFrom: 2,
            dayTo: 5,
            hourFrom: 11,
            hourTo: 15
        });

        expect(getSelect('dayFrom').val()).to.be.equal('2');
        expect(getSelect('dayTo').val()).to.be.equal('5');
        expect(getSelect('hourFrom').val()).to.be.equal('11');
        expect(getSelect('hourTo').val()).to.be.equal('15');
    });

    it('В dayTo не должно быть значение меньше dayFrom', function() {
        createBlock({
            dayFrom: 1,
            dayTo: 2,
            hourFrom: 11,
            hourTo: 15
        });

        getSelect('dayFrom').val('5');

        expect(getSelect('dayTo').val()).to.be.equal('5');
    });

    it('В hourTo значение должно быть не меньше, чем hourFrom + 1', function() {
        createBlock({
            dayFrom: 1,
            dayTo: 2,
            hourFrom: 5,
            hourTo: 11
        });

        getSelect('hourFrom').val('15');

        expect(getSelect('hourTo').val()).to.be.equal('16');
    });

    ['dayFrom', 'dayTo', 'hourFrom', 'hourTo'].forEach(function(name) {
        it('При изменении ' + name + '  должно триггериться событие', function() {
            createBlock({
                dayFrom: 1,
                dayTo: 2,
                hourFrom: 1,
                hourTo: 3
            });
            getSelect(name).val('5');

            sandbox.clock.tick(100);

            expect(block.trigger.called).to.be.equal(true);
        });
    });
});
