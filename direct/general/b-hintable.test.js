describe('b-hintable', function() {
    var block,
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('Поведение блока в зависимости от входных данных', function() {

        it('Должен показать popup при наведении на block', function() {
            block = u.getInitedBlock({
                block: 'link',
                content: 'Link',
                mix: [
                    {
                        block: 'b-hintable',
                        js: {
                            hint: 'Текст'
                        }
                    }
                ]
            });

            block.domElem.trigger('mouseenter');
            sandbox.clock.tick(500);

            expect($('.popup_visibility_visible').length).to.be.eq(1);
        });

        it('Должен показать подсказку с текстом', function() {
            block = u.getInitedBlock({
                block: 'link',
                content: 'Link',
                mix: [
                    {
                        block: 'b-hintable',
                        js: {
                            hint: 'Текст'
                        }
                    }
                ]
            });

            block.domElem.trigger('mouseenter');
            sandbox.clock.tick(500);

            expect($('.b-hintable__hint-content').text()).to.be.eq('Текст');
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
                            block: 'b-hintable',
                            js: true
                        }
                    ]
                });

                block.findBlockOn('b-hintable').setHintContent('Текст');
                block.domElem.trigger('mouseenter');
                sandbox.clock.tick(500);;

                expect($('.b-hintable__hint-content').text()).to.be.eq('Текст');
            });

            it('Должен поменять содержимое подсказки', function() {
                block = u.getInitedBlock({
                    block: 'link',
                    content: 'Link',
                    mix: [
                        {
                            block: 'b-hintable',
                            js: {
                                hint: 'Текст'
                            }
                        }
                    ]
                });

                block.findBlockOn('b-hintable').setHintContent('Текст2');
                block.domElem.trigger('mouseenter');
                sandbox.clock.tick(500);

                expect($('.b-hintable__hint-content').text()).to.be.eq('Текст2');
            });
        })
    });
});
