describe('Daria.ComposeComponents.confirmations', function() {

    beforeEach(function() {
        this.component = Daria.ComposeComponents.confirmations;
        this._originalConfirmations = this.component._confirmations;
    });

    afterEach(function() {
         this.component._confirmations = this._originalConfirmations;
    });

    describe('#addConfirmation', function() {

        beforeEach(function() {
            this.component._confirmations = {};
            this.func = no.false;
            this.name = "test";

            this.component.addConfirmation(this.name, this.func);
        });

        it('Должен добавить подтверждение в коллекцию', function() {
            expect(this.component._confirmations[this.name]).to.be.equal(this.func);
        });
    });

    describe('#getConfirmation', function() {

        beforeEach(function() {
            this.component._confirmations = {
                "test-name": no.false,
                "test-name-2": no.true
            };
        });

        it('Должен вернуть функцию', function() {
            expect(this.component.getConfirmation("test-name")).to.be.a('function');
        });

        it('Должен вернуть соответствующую проверку', function() {
            expect(this.component.getConfirmation("test-name-2")).to.be.equal(no.true);
        });
    });

    describe('#getConfirmations', function() {

        it('Должен вернуть массив', function() {
            expect(this.component.getConfirmations()).to.be.an('array');
        });

        it('Элемент массива должен быть значением в объекте', function() {
            this.component._confirmations = {
                "1": no.false
            };

            expect(this.component.getConfirmations()[0]).to.be.equal(no.false);
        });

        it('Должен вернуть все подтверждения', function() {
            this.component._confirmations = {
                "1": no.false,
                "2": no.false,
                "3": no.false,
                "4": no.false,
                "5": no.false
            };

            expect(this.component.getConfirmations().length).to.be.equal(5);
        });
    });
});
