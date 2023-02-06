describe('b-rotate-buttons', function() {
    var size = 13,
        context,
        block,
        buttons,
        flags;

    before(function(done) {
        var elem = u
            .getDOMTree({ block: 'b-rotate-buttons' })
            .appendTo(document.body)
            .css({ visibility: 'hidden', position: 'absolute', top: 0 });

        BEM.DOM.init(elem, function(ctx) {
            context = ctx;

            block = context.bem('b-rotate-buttons');

            buttons = {
                back: block.findBlockOn('back', 'button'),
                next: block.findBlockOn('next', 'button')
            };

            flags = { back: false, next: false };

            block.on('back next', function(e) {
                flags[e.type] = true;
            });

            done();
        });
    });

    after(function() {
        block.destruct();
    });

    it('При клике вперед идём вперед', function(done) {
        buttons.next.domElem.click();

        block.afterCurrentEvent(function() {
            expect(flags.next).to.be.equal(true);
            flags.next = false;

            done();
        });
    });

    it('При клике назад идём назад', function(done) {
        buttons.back.domElem.click();

        block.afterCurrentEvent(function() {
            expect(flags.back).to.be.equal(true);
            flags.back = false;

            done();
        });
    });

    it('При вызове метода goNext() идём вперед', function(done) {
        block.goNext();

        block.afterCurrentEvent(function() {
            expect(flags.next).to.be.equal(true);
            flags.next = false;

            done();
        });
    });

    it('При вызове метода goBack() идём назад', function(done) {
        block.goBack();

        block.afterCurrentEvent(function() {
            expect(flags.back).to.be.equal(true);
            flags.back = false;

            done();
        });
    });

    describe('Проверка ограничений (размерность ' + size + '):', function() {
        it('кнопка "назад" выключена', function() {
            block.setSize(size);

            expect(buttons.back.hasMod('disabled', 'yes')).to.be.equal(true);
        });

        it('кнопка "вперёд" активна', function() {
            expect(buttons.next.hasMod('disabled', 'yes')).to.be.equal(false);
        });

        [
            { disabled: { key: 'next', text: 'вперёд'}, active: { key: 'back', text: 'назад' } },
            { disabled: { key: 'back', text: 'назад' }, active: { key: 'next', text: 'вперёд'} }
        ].forEach(function(o) {
            describe('после ' + size + ' переходов ' + o.disabled.text, function() {
                it('кнопка "' + o.disabled.text + '" выключена', function(done) {
                    var button = buttons[o.disabled.key];

                    for (var i = 0; i < size; i++) {
                        button.domElem.click();
                    }

                    block.afterCurrentEvent(function() {
                        expect(button.hasMod('disabled', 'yes')).to.be.equal(true);

                        done();
                    });
                });

                it('кнопка "' + o.active.text + '" активна', function() {
                    expect(buttons[o.active.key].hasMod('disabled', 'yes')).to.be.equal(false);
                });
            });

            describe('при выставлении блоку _disabled_yes', function() {
                it('кнопка "' + o.disabled.text + '" выключена', function(done) {
                    block.setMod('disabled', 'yes');

                    block.afterCurrentEvent(function() {
                        expect(buttons[o.disabled.key].hasMod('disabled', 'yes')).to.be.equal(true);

                        done();
                    });
                });

                it('кнопка ' + o.active.text + ' выключена', function() {
                    expect(buttons[o.active.key].hasMod('disabled', 'yes')).to.be.equal(true);
                });
            });

            describe('при снятии с блока _disabled_yes', function() {
                it('кнопка "' + o.disabled.text + '" выключена', function(done) {
                    block.delMod('disabled');

                    block.afterCurrentEvent(function() {
                        expect(buttons[o.disabled.key].hasMod('disabled', 'yes')).to.be.equal(true);

                        done();
                    });
                });

                it('кнопка ' + o.active.text + ' активна', function() {
                    expect(buttons[o.active.key].hasMod('disabled', 'yes')).to.be.equal(false);
                });
            });
        });
    });

    describe('Проверка снятия ограничений (размерность -1):', function() {
        it('кнопка "назад" активна', function() {
            block.setSize(-1);

            expect(buttons.back.hasMod('disabled', 'yes')).to.be.equal(false);
        });

        it('кнопка "вперёд" активна', function() {
            expect(buttons.next.hasMod('disabled', 'yes')).to.be.equal(false);
        });
    });

});
