describe('b-hintable-popup', function() {
    var block,
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        // отключаю анимацию в jquery, иначе закрытие попаппа после его открытия не отловить о_0
        $.fx.off = true;
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
        // возвращаю анимацию в jquery
        $.fx.off = false;
    });

    describe('Поведение блока в зависимости от входных данных', function() {

        it('Должен показать popup при клике на block', function() {
            block = u.getInitedBlock({
                block: 'link',
                content: 'Link',
                mix: [
                    {
                        block: 'b-hintable-popup',
                        js: {
                            hint: 'Текст'
                        }
                    }
                ]
            });

            block.domElem.click();
            sandbox.clock.tick(500);

            expect($('.popup_visibility_visible').length).to.be.eq(1);
        });

        it('Должен спрятать popup при повторном клике на block', function() {
            block = u.getInitedBlock({
                block: 'link',
                content: 'Link',
                mix: [
                    {
                        block: 'b-hintable-popup',
                        js: {
                            hint: 'Текст'
                        }
                    }
                ]
            });

            block.domElem.click();
            sandbox.clock.tick(500);
            block.domElem.click();
            sandbox.clock.tick(500);

            expect($('.popup_visibility_visible').length).to.be.eq(0);
        });

        it('Должен показать подсказку с текстом', function() {
            block = u.getInitedBlock({
                block: 'link',
                content: 'Link',
                mix: [
                    {
                        block: 'b-hintable-popup',
                        js: {
                            hint: 'Текст'
                        }
                    }
                ]
            });

            block.domElem.click();
            sandbox.clock.tick(500);

            expect($('.popup__content').text()).to.be.eq('Текст');
        });
    });

    describe('Статические методы', function() {

        describe('setHintContent', function() {

            it('Должен установить содержимое подсказки', function() {
                block = u.getInitedBlock({
                    block: 'link',
                    content: 'Link',
                    mix: [
                        {
                            block: 'b-hintable-popup',
                            js: true
                        }
                    ]
                });

                block.findBlockOn('b-hintable-popup').setHintContent('Текст');
                block.domElem.click();
                sandbox.clock.tick(500);

                expect($('.popup__content').text()).to.be.eq('Текст');
            });

            it('Должен поменять содержимое подсказки', function() {
                block = u.getInitedBlock({
                    block: 'link',
                    content: 'Link',
                    mix: [
                        {
                            block: 'b-hintable-popup',
                            js: {
                                hint: 'Текст'
                            }
                        }
                    ]
                });

                block.findBlockOn('b-hintable-popup').setHintContent('Текст2');
                block.domElem.click();
                sandbox.clock.tick(500);

                expect($('.popup__content').text()).to.be.eq('Текст2');
            });
        })
    });
});
