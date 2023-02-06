describe('Daria.Yabbles (aka YabbleSet)', function() {

    describe('#onkeypress', function() {

        beforeEach(function() {
            this.clock = this.sinon.useFakeTimers();

            this.yabbles = new Daria.Yabbles($('<div/>'));
            this.sinon.stub(this.yabbles, 'add').returns({ focus: function() {} });

            this.generateKeyPress = function(key, currentValue) {
                var e = {
                    which: key,
                    preventDefault: function() {}
                };

                var yabble = new Daria.Yabble();
                yabble.focused = true;
                this.sinon.stub(yabble, 'userVal').returns(currentValue);

                this.yabbles.onkeypress(e, yabble);
            };
        });

        afterEach(function() {
            this.clock.restore();
        });

        it('Введён валидный email и нажимается " " - должен быть создан ябл', function() {
            this.generateKeyPress(Daria.Yabble.KEY.SPACE, 'human@mail.com');
            this.clock.tick(100);
            expect(this.yabbles.add).to.have.callCount(1);
        });

        it('Введён НЕвалидный email и нажимается " " - ябл не должен создаваться', function() {
            this.generateKeyPress(Daria.Yabble.KEY.SPACE, 'human@mail.c');
            this.clock.tick(100);
            expect(this.yabbles.add).to.have.callCount(0);
        });

    });

});
