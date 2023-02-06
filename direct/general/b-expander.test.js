describe('b-expander', function() {

    var block,
        sandbox;

    function createBlock(options) {
        options || (options = {});

        block = u.getInitedBlock({
            block: 'b-expander',
            mods: options.mods,
            title: options.title,
            subtitle: options.subtitle,
            body: options.body
        });
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    describe('Инициализация', function() {

        before(function() {
            createBlock({
                title: 'title',
                subtitle: 'subtitle',
                body: 'body'
            });
        });

        after(function() {
            destructBlock();
        });

        it('Должен быть заголовок', function() {
            expect(block.elem('title').text()).to.be.eq('title');
        });

        it('Должен быть подзаголовок', function() {
            expect(block.elem('subtitle').text()).to.be.eq('subtitle');
        });

        it('Должен быть контент', function() {
            expect(block.elem('body').text()).to.be.eq('body');
        });

    });

    describe('Поведение', function() {

        beforeEach(function() {
            createBlock({
                title: 'title',
                subtitle: 'subtitle',
                body: 'body'
            });
        });

        afterEach(function() {
            destructBlock();
        });

        describe('Без модификатора open', function() {

            it('При нажатии на шапку, должен выставиться модификатор', function() {
                block.elem('header').click();

                expect(block).to.haveMod('open', 'yes');
            });

        });

        describe('С модификатором open_yes', function() {


            it('При нажатии на шапку, должен удалиться модификатор', function() {
                block.setMod('open', 'yes');

                block.elem('header').click();

                expect(block).to.not.haveMod('open', 'yes');
            });

        });

    });

    describe('Создание блока с модификатором open_yes', function() {

        beforeEach(function() {
            sandbox = sinon.sandbox.create();

            createBlock({
                mods: {
                    open: 'yes'
                },
                title: 'title',
                subtitle: 'subtitle',
                body: 'body'
            });

        });

        afterEach(function() {
            destructBlock();
        });

        it('Должен быть виден body', function() {
            expect(block.elem('body').is(':visible')).to.be.true;
        });

        it('При нажатии на шапку, должен удалиться модификатор', function() {
            block.elem('header').click();

            expect(block).to.not.haveMod('open', 'yes');
        });

    });

    describe('Методы', function() {

        beforeEach(function() {
            createBlock({
                title: 'title',
                subtitle: 'subtitle',
                body: 'body'
            });
        });

        afterEach(function() {
            destructBlock();
        });

        describe('setSubtitle', function() {

            it('Меняет подзаголовок', function() {
                block.setSubtitle('Subtitle new');

                expect(block.findElem('subtitle').text()).to.be.eq('Subtitle new');
            });

        });

        describe('setBody', function() {

            it('Меняет контент', function() {
                block.setBody('Body new');

                expect(block.findElem('body').text()).to.be.eq('Body new');
            });

        });

    });

});
