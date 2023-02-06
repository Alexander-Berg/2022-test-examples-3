describe('sound.message', function() {
    beforeEach(function() {
        this.soundMock = this.sinon.mock(Daria.Sound);
    });

    describe("При получении нового сообщения", function() {

        beforeEach(function() {
            this.params = {params: {}};
        });

        it("Должен играть стандартный звук", function() {
            this.soundMock.expects('play').withArgs('message');
            ns.action.run('sound.message', this.params);

            this.soundMock.verify();
        });
    });

    describe("При изменении звуковой настройки", function() {
        beforeEach(function() {
            this.params = {params: {'setup': 'true'}};

            this.nbMock = this.sinon.mock(nb);
        });

        it("Если ее включают, то должен играться стандартный звук", function() {
            var block = { isChecked: function() {}};
            var blockMock = this.sinon.mock(block);


            this.nbMock.expects('$block').returns(block);
            blockMock.expects('isChecked').returns(false);

            this.soundMock.expects('play').withArgs('message');
            ns.action.run('sound.message', this.params);

            this.soundMock.verify();
        });

        it("Если ее выключают, то звук играть не должен", function() {
            var block = { isChecked: function() {}};
            var blockMock = this.sinon.mock(block);

            this.nbMock.expects('$block').returns(block);
            blockMock.expects('isChecked').returns(false);

            ns.action.run('sound.message', this.params);

            this.soundMock.verify();
        });
    });
});
