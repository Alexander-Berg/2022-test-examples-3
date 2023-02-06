describe('b-banner-adv-alert2__warnings', function() {

    describe('Содержание элемента в зависимости от входных данных', function() {

        var sandbox,
            block,
            constStub;

        beforeEach(function() {
            sandbox = sinon.sandbox.create();
            constStub = sandbox.stub(u, 'consts');

            constStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
        });

        afterEach(function() {
            block.destruct();
            sandbox.restore();
        });

        it('Содержит елемент warn', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    alcohol: 1
                }
            }, false);

            expect(block.elem('warn').length).to.be.gt(0);
        });

        it('Не содержит елемент warn, если он общий', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    age: 18
                }
            }, false);

            expect(block.elem('warn').length).to.be.eq(0);
        });

        it('Содержит елемент baby-food, если он есть в value и пользователь может редактировать изменения', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    baby_food: '10'
                },
                can:{
                    edit: true
                }
            }, false);

            expect(block.elem('baby-food').length).to.be.gt(0);
        });

        it('Не содержит елемент baby-food, если он есть в value и пользователь не может его менять', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    baby_food: '10'
                }
            }, false);

            expect(block.elem('baby-food').length).to.be.eq(0);
        });

    });
});
