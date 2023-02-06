describe('b-banner-adv-alert2__baby-food', function() {

    describe('Содержание элемента в зависимости от входных данных', function() {

        var sandbox,
            block,
            constStub;

        beforeEach(function() {
            sandbox = sinon.sandbox.create();
            constStub = sandbox.stub(u, 'consts');
            constStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
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
        });

        afterEach(function() {
            block.destruct();
            sandbox.restore();
        });

        it('Содержит елемент baby-food-value', function() {
            expect(block.elem('baby-food-value').length).to.be.gt(0);
        });

    });
    describe('Тесты на взаимодействие с popup', function() {
        var sandbox,
            block,
            constStub;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({
                useFakeTimers: true
            });
            constStub = sandbox.stub(u, 'consts');
            constStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
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
        });

        afterEach(function() {
            block.destruct();
            sandbox.restore();
        });

        it('Должен показать popup после нажатия на baby-food-value', function() {
            $('.b-banner-adv-alert2__baby-food .link').click();
            sandbox.clock.tick(500);
            expect($('.popup.popup_visibility_visible').length).to.be.eq(1);

            $('.b-banner-adv-alert2__baby-food .link').click();
            sandbox.clock.tick(1500);
        });

        it('Должен вызвать block.changeValue() при изменении baby-food-value', function() {
            sandbox.stub(block, 'changeValue').callsFake(function() {});

            $('.b-banner-adv-alert2__baby-food .link').click();
            sandbox.clock.tick(1500);

            $('.popup.popup_visibility_visible .link').first().click();
            sandbox.clock.tick(500);

            expect(block.changeValue.calledWith('baby_food')).to.be.true;

        });

        it('Должен спрятать popup после выбора', function() {
            sandbox.stub(block, 'changeValue').callsFake(function() {});

            $('.b-banner-adv-alert2__baby-food .link').click();
            sandbox.clock.tick(500);

            var popup = block.elemInstance('baby-food')._popup,
                spyPopupHide = sandbox.stub(popup, 'hide').callsFake(function() {});

            $('.popup.popup_visibility_visible .link').first().click();
            sandbox.clock.tick(1500);

            expect(spyPopupHide.called).to.be.true;

            BEM.DOM.destruct(popup.domElem);
        });

    });
});
