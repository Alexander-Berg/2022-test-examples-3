describe('b-agency-select', function() {

    describe('Содержание блока в зависимости от входных данных', function() {
        var sandbox,
            block;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({
                useFakeTimers: true
            });
        });

        afterEach(function() {
            block && block.destruct();
            sandbox.restore();
        });

        it('Должен содержать одно агенство для выбора', function() {
            block = u.getInitedBlock({
                block: 'b-agency-select',
                agencies: [{agency_name:'qwe', login: 'qwe'}],
                options: {
                    addToDropdown: 'agencies',
                    newCampBelonging: '',
                    newCampSelfType: ''
                }
            }, false);

            expect($('.select__option').length).to.be.eq(1);
        });
        it('Должен содержать два агенства для выбора', function() {
            block = u.getInitedBlock({
                block: 'b-agency-select',
                agencies: [
                    {agency_name:'qwe', login: 'qwe'},
                    {agency_name:'qwe1', login: 'qwe1'}
                ],
                options: {
                    addToDropdown: 'agencies',
                    newCampBelonging: '',
                    newCampSelfType: ''
                }
            }, false);

            expect($('.select__option').length).to.be.eq(2);
        });
        it('Должен содержать пункт самостоятельное обслуживание первым в списке', function() {
            block = u.getInitedBlock({
                block: 'b-agency-select',
                agencies: [
                    {agency_name:'qwe', login: 'qwe'},
                    {agency_name:'qwe1', login: 'qwe1'}
                ],
                options: {
                    addToDropdown: 'full',
                    newCampBelonging: '',
                    newCampSelfType: 'self'
                }
            }, false);

            expect($('.select__option').first().html() == 'для самостоятельного обслуживания').to.be.true;
        });
        it('Должен содержать пункт на обслуживание менеджером первым в списке', function() {
            block = u.getInitedBlock({
                block: 'b-agency-select',
                agencies: [
                    {agency_name:'qwe', login: 'qwe'},
                    {agency_name:'qwe1', login: 'qwe1'}
                ],
                options: {
                    addToDropdown: 'full',
                    newCampBelonging: '',
                    newCampSelfType: ''
                }
            }, false);

            expect($('.select__option').first().html() == 'на обслуживание менеджером').to.be.true;
        });
        it('Должен выбрать агенство qwe1', function() {
            block = u.getInitedBlock({
                block: 'b-agency-select',
                agencies: [
                    {agency_name:'qwe', login: 'qwe'},
                    {agency_name:'qwe1', login: 'qwe1'}
                ],
                options: {
                    addToDropdown: 'full',
                    newCampBelonging: 'qwe1',
                    newCampSelfType: ''
                }
            }, false);

            expect($('.select__control').val()).to.be.eq('qwe1');
        });
        it('Должен выбрать пустое значение (самостоятельное обслуживание)', function() {
            block = u.getInitedBlock({
                block: 'b-agency-select',
                agencies: [
                    {agency_name:'qwe', login: 'qwe'},
                    {agency_name:'qwe1', login: 'qwe1'}
                ],
                options: {
                    addToDropdown: 'full',
                    newCampBelonging: '$self$',
                    newCampSelfType: 'self'
                }
            }, false);

            expect($('.select__control').val()).to.be.eq('');
        });
    });
    describe('Тестирование методов i-bem', function() {
        var sandbox,
                block;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({
                useFakeTimers: true
            });
        });

        afterEach(function() {
            block && block.destruct();
            sandbox.restore();
        });

        it('Должен стригерить событие agency-change со значением qwe1', function() {
            block = u.getInitedBlock({
                block: 'b-agency-select',
                agencies: [
                    { agency_name: 'qwe', login: 'qwe' },
                    { agency_name: 'qwe1', login: 'qwe1' }
                ],
                options: {
                    addToDropdown: 'agencies',
                    newCampBelonging: '',
                    newCampSelfType: ''
                }
            }, false);
            sandbox.spy(block, 'trigger');
            sandbox.clock.tick(500);
            block.findBlockInside('select').val('qwe1');
            sandbox.clock.tick(500);

            expect(block).to.triggerEvent('agency-change', 'qwe1');
        });
    });
});
