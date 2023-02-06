describe('b-banner-adv-alert2__edit-panel', function() {

    describe('Содержание элемента в зависимости от входных данных', function() {

        var sandbox,
            block,
            constStub;

        beforeEach(function() {
            sandbox = sinon.sandbox.create();
            constStub = sandbox.stub(u, 'consts');
            constStub.withArgs('AD_WARNINGS').returns({
                age: {
                    is_common_warn: true,
                    variants: [18, 16, 12, 6, 0],
                    'default': 18
                },
                alcohol: {
                    long_text: 'Чрезмерное потребление вредно.',
                    short_text: 'алкоголь'
                }
            });
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    baby_food: 10
                },
                can: { // Возможности пользователя
                    addRemove: true, // Добавлять или удалять предупреждения
                    edit: true // Редактировать выставленные флаги
                }
            }, false);
            block._addEditPanel();
        });

        afterEach(function() {
            block.destruct();
            sandbox.restore();
        });

        it('Содержит предупреждения согласно AD_WARNINGS (не общие)', function() {
            expect(block.findElem('select-item').length).to.be.eq(1);
        });

    });

});
