describe('b-date-input', function() {
    var dateInput,
        bemjson = {
            block: 'b-date-input',
            name: 'start_date',
            value: '1990-03-28',
            dateFormat: 'YYYY-MM-DD',
            viewFormat: 'DD MM YYYY'
        };

    beforeEach(function() {
        dateInput = BEM.DOM
            .init($(BEMHTML.apply(bemjson)))
            .appendTo('body')
            .bem('b-date-input');
    });

    afterEach(function() {
        BEM.DOM.destruct(dateInput.domElem);
    });

    describe('.onSetMod', function() {
        describe('js', function() {
            it('должен выставить форматированное значение в инпут', function() {
                expect(dateInput.findBlockInside('input').val()).to.be.eql('28 03 1990');
            });
        });

        describe('disabled', function() {
            it('должен деактивировать внутренний инпут, если сам был деактивирован', function() {
                dateInput.setMod('disabled', 'yes');
                expect(dateInput.findBlockInside('input').hasMod('disabled')).to.be.eql(true);
            });
            it('должен активировать внутренний инпут, если сам был активирован', function() {
                dateInput.setMod('disabled', 'yes');
                dateInput.setMod('disabled', '');
                expect(dateInput.findBlockInside('input').hasMod('disabled')).to.be.eql(false);
            });
        });
    });

    describe('.val()', function() {
        it('должен устанавливать значение внутреннему элемент, форматируя его', function() {
            dateInput.val('1999-09-11');
            expect(dateInput.findBlockInside('input').val()).to.be.eql('11 09 1999');
        });

        it('должен устанавливать значение в скрытый инпут без форматирования', function() {
            dateInput.val('1999-09-11');
            expect(dateInput.elem('hidden').val()).to.be.eql('1999-09-11');
        });

        it('должен возвращать значение, если параметр не задан', function() {
            expect(dateInput.val()).to.be.eql('1990-03-28');
        });
    });

});
