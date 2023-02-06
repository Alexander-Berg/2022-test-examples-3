describe('b-video-extension', function() {
    var block,
        sandbox,
        createBlock = function(ctx) {
            var block;

            ctx = ctx || {};

            block = u.createBlock({
                block: 'b-video-extension',
                video: ctx.video,
                strategy: ctx.strategy || {}
            }, {
                inject: true
            });

            return block;
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
    });

    afterEach(function() {
        block.destruct && block.destruct();
        sandbox.restore();
    });

    describe('Метод switchToEdit работает корректно', function() {
        var videoData = { id: 123, name: "test", resource_type: "creative" };
        beforeEach(function() {
            block = createBlock();

            block.switchToEdit(videoData);
            sandbox.clock.tick(300);
        });

        it('Появляется ссылка на видео', function() {
            expect(block.findElem('video-link').is(':visible')).to.be.equal(true);
        });

        it('Текст ссылки соответсвует названию видео', function() {
            expect(block.findElem('video-link').text()).to.be.equal(videoData.name);
        });

        it('Кнопка удаления видео доступна', function() {
            expect(block.findElem('delete').is(':visible')).to.be.equal(true);
        });

        it('Селектор в режиме редактирования', function() {
            expect(block.findBlockInside('selector', 'button').domElem.text()).to.be.equal('Изменить');
        });
    });

    describe('Метод switchToAdd работает корректно', function() {
        beforeEach(function() {
            block = createBlock();

            block.switchToAdd();
            sandbox.clock.tick(300);
        });

        it('Ссылка на видео не отображается', function() {
            expect(block.findElem('video-link').is(':visible')).to.be.equal(false);
        });

        it('Кнопка удаления видео недоступна', function() {
            expect(block.findElem('delete').is(':visible')).to.be.equal(false);
        });

        it('Селектор в режиме добавления', function() {
            expect(block.findBlockInside('selector', 'button').domElem.text()).to.be.equal('Добавить');
        });
    });

    describe('Cобытия', function() {
        var observable;

        beforeEach(function() {
            observable = new $.observable();
            observable.show = function() {};
            sandbox.stub(BEM.DOM.blocks['b-banner-storage-frame'], 'create').callsFake(function() {
                return observable;
            });
            block = createBlock({ video: { id: 123, name: "test", resource_type: "creative" } });
        });

        it('Клик на кнопку удалить триггерит событие delete', function() {
            expect(block).to.triggerEvent('delete', function() {
                block.findElem('delete').trigger('click');
            });
        });

        it('Клик на ссылку триггерит событие show', function() {
            expect(block).to.triggerEvent('show', function() {
                block.findElem('video-link').trigger('click');
            });
        });

        it('При событии select от конструктора, триггерит событие add', function() {
            block.findBlockInside('b-video-creative-selector').trigger('select', 'recent');

            expect(block).to.triggerEvent('add', function() {
                observable.trigger('select', [{ name: 'test', id: '123' }]);
            });
        });

    });
});
