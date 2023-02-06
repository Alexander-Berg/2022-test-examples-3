describe('b-warning-message', function() {

    describe('Содержание блока в зависимости от входных данных', function() {

        var sandbox,
            block;

        beforeEach(function() {
            sandbox = sinon.sandbox.create();
        });

        afterEach(function() {
            block.destruct();
            sandbox.restore();
        });

        it('Должен содержать текст', function() {
            block = u.getInitedBlock({
                block: 'b-warning-message',
                content: [
                    'Текст'
                ]
            });

            expect($('.b-warning-message__content').text()).to.be.eq('Текст');
        });

        it('Должен содержать иконку', function() {
            block = u.getInitedBlock({
                block: 'b-warning-message',
                icon: {
                    block: 'b-icon',
                    mods: { 'size-12': 'notice' }
                },
                content: [
                    'Текст'
                ]
            });

            expect($('.b-warning-message__icon').length).to.be.eq(1);
        });

        it('Должен содержать мод icon_yes', function() {
            block = u.getInitedBlock({
                block: 'b-warning-message',
                icon: {
                    block: 'b-icon',
                    mods: { 'size-12': 'notice' }
                },
                content: [
                    'Текст'
                ]
            });

            expect(block).to.haveMod('icon', 'yes');
        });

        it('Должен содержать мод theme_red если мод theme не задан', function() {
            block = u.getInitedBlock({
                block: 'b-warning-message',
                icon: {
                    block: 'b-icon',
                    mods: { 'size-12': 'notice' }
                },
                content: [
                    'Текст'
                ]
            });

            expect(block).to.haveMod('theme', 'red');
        });

        it('Должен выставить модификатор theme_info', function() {
            block = u.getInitedBlock({
                block: 'b-warning-message',
                mods: { theme: 'info' },
                icon: {
                    block: 'b-icon',
                    mods: { 'size-12': 'notice' }
                },
                content: [
                    'Текст'
                ]
            });

            expect(block).to.haveMod('theme', 'info');
        });
    });
});
