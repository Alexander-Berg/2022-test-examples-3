describe('collector-settings', function() {

    beforeEach(function() {
        this.proto = Daria.Collectors.Settings.prototype;
    });

    describe('#updateNoDeleteMsgOption', function() {
        beforeEach(function() {
            this.getControl = this.sinon.stub(this.proto, 'getControl');
            this.getControlValue = this.sinon.stub(this.proto, 'getControlValue');

            this.getControlValue.returns('pop');
            this.getControl
                .withArgs('server')
                .returns({
                    getValue: function() {
                        return 'pop.test.ru';
                    }
                });

            this.proto.node = $('<div><div class="js-not-delete-msg-option"></div></div>');
            this.$nodeBlock = $('.js-not-delete-msg-option', this.proto.node);
        });

        it('Должен показать опцию, если все условия не прошли', function() {
            this.proto.updateNoDeleteMsgOption();

            expect(this.$nodeBlock.hasClass('g-hidden')).to.be.equal(false);
        });

        it('Должен скрыть опцию, если протокол IMAP', function() {
            this.getControlValue.returns('imap');
            this.proto.updateNoDeleteMsgOption();

            expect(this.$nodeBlock.hasClass('g-hidden')).to.be.equal(true);
        });

        it('Должен скрыть опцию, если сервер POP наш', function() {
            this.getControl
                .withArgs('server')
                .returns({
                    getValue: function() {
                        return 'pop.yandex.ru';
                    }
                });
            this.proto.updateNoDeleteMsgOption();

            expect(this.$nodeBlock.hasClass('g-hidden')).to.be.equal(true);
        });
    });

});

