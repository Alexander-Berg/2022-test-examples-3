describe('Daria.Yabble', function() {

    describe('#pasteValue', function() {
        beforeEach(function() {
            this.spyPasteValue = this.sinon.spy(Daria.Yabble.prototype, 'pasteValue');
            this.sinon.stub(Daria.Recipients, 'add');
        });

        it('не должен вызваться если создали ябл без данных', function() {
            new Daria.Yabble();
            expect(this.spyPasteValue.callCount).to.be.equal(0);
        });

        it('должен вызваться, если создали ябл с данными', function() {
            new Daria.Yabble({
                'email': 'test@ya.ru'
            });
            expect(this.spyPasteValue.callCount).to.be.equal(1);
        });

        it('должен вызвать Daria.Recipients.add если переданы даныне о контакте', function() {
            var params = {
                'email': 'test@ya.ru',
                'name': 'test'
            };

            new Daria.Yabble(params);

            expect(Daria.Recipients.add).to.be.calledWithExactly({
                'email': params.email,
                'name': params.name,
                'href': false,
                'size': 20,
                'className': 'mail-Yabble__avatar'
            });
        });

        it('должен вызвать Daria.Recipients.add с аватаром группы, если нет email и переданны контакты', function() {
            var params = {
                'contacts': []
            };

            new Daria.Yabble(params);

            expect(Daria.Recipients.add).to.be.calledWithExactly({
                'href': false,
                'size': 20,
                'className': 'mail-Yabble__avatar'
            });
        });

        it('должен вызвать Daria.Recipients.add для контакта без данных, если данных нет', function() {
            var params = {
                'blabla': 'blabla'
            };

            new Daria.Yabble(params);

            expect(Daria.Recipients.add).to.be.calledWithExactly({
                'href': false,
                'size': 20,
                'className': 'mail-Yabble__avatar'
            });
        });
    });
});
