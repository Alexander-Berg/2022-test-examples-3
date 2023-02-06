describe('$.fn.startEmulateInputEvent', function() {
    beforeEach(function() {
        this.$nodes = $('<div />');
        this.$nodes.add($('<div />'));
    });

    it('должен создать Daria.InputEventEmulator для каждого элемента, если был передан селектор текстового поля', function() {
        this.$nodes.startEmulateInputEvent('.some-input');

        this.$nodes.each(function() {
            expect($(this).data('inputEmulator') instanceof Daria.InputEventEmulator).to.be.equal(true);
        });
    });

    it('должен не создавать Daria.InputEventEmulator для каждого элемента, если не был передан селектор текстового поля', function() {
        this.$nodes.startEmulateInputEvent();

        this.$nodes.each(function() {
            expect(Boolean($(this).data('inputEmulator'))).to.be.equal(false);
        });
    });

});

describe('$.fn.stopEmulateInputEvent', function() {
    beforeEach(function() {
        this.$nodes = $('<div />');
        this.$nodes.add($('<div />'));
        this.$nodes.startEmulateInputEvent('.some-input');
    });

    it('должен уничтожить эмулятор input события на переданных элементах', function() {
        var that = this;
        this.inputEmulators = [];
        this.$nodes.each(function() {
            var inputEmulator = $(this).data('inputEmulator');
            that.inputEmulators.push(inputEmulator);
            that.sinon.stub(inputEmulator, 'destroy');
        });
        this.$nodes.stopEmulateInputEvent();

        this.inputEmulators.forEach(function(inputEmulator) {
            expect(inputEmulator.destroy).to.have.callCount(1);
        });
    });

    it('не должен вызывать эксепшен, если эмуляторов input события не было создано на элементе или они были удалены ', function() {
        var that = this;
        expect(function() {
            that.$nodes.stopEmulateInputEvent();
            that.$nodes.stopEmulateInputEvent();
        }).to.not.throw();
    });

});

describe('Daria.InputEventEmulator', function() {
    beforeEach(function() {
        this.$node = $('<div />');
        this.$input = $('<input class="some-input" type="text" value="" />');
        this.$node.append(this.$input);

        this.inputEmulator = new Daria.InputEventEmulator(this.$node, '.some-input');
    });

    describe('#init', function() {

        beforeEach(function() {
            this.sinon.stub(this.$node, 'on').returns(this.$node);
        });

        it('не должен подписываться на события, если отсутствует селектор отслеживаемых текстовых полей', function() {
            var inputEmulator = new Daria.InputEventEmulator(this.$node);
            inputEmulator.init();

            expect(this.$node.on).to.have.callCount(0);
        });

        it('должен подписаться на события focusin и focusout, если при создании передан селектор отслеживаемых текстовых полей', function() {
            var inputEmulator = new Daria.InputEventEmulator(this.$node, '.some-input');
            var firstCall = this.$node.on.withArgs('focusin.inputEmulator', '.some-input');
            var secondCall = this.$node.on.withArgs('focusout.inputEmulator', '.some-input');

            inputEmulator.init();

            expect(firstCall.calledOnce).to.be.equal(true);
            expect(secondCall.calledOnce).to.be.equal(true);
        });

    });

    describe('#destroy', function() {

        it('должен остановить эмуляцию события input', function() {
            this.sinon.stub(this.inputEmulator, 'stopEmulate');
            this.inputEmulator.destroy();

            expect(this.inputEmulator.stopEmulate).to.have.callCount(1);
        });

        it('должен отписать события от корневой ноды', function() {
            this.sinon.stub(this.$node, 'off');
            this.inputEmulator.destroy();

            expect(this.$node.off).to.be.calledWithExactly('.inputEmulator');
        });

    });

    describe('#startEmulate', function() {

        beforeEach(function() {
            this.startEvent = $.Event('focusin', {currentTarget: this.$input[0]});
            this.sinon.spy(this.inputEmulator, 'triggerInputEvent');
        });

        it('должен запомнить текстовый элемент, изменение которого будут отслеживаться', function() {
            this.inputEmulator.startEmulate(this.startEvent);

            expect(this.inputEmulator.$target[0]).to.be.equal(this.$input[0]);
        });

        it('должен запомнить текущее значение текстового поля', function() {
            this.$input.val('test');
            this.inputEmulator.startEmulate(this.startEvent);

            expect(this.inputEmulator.currentValue).to.be.equal('test');
        });

        ['keydown', 'keyup', 'selectionchange'].forEach(function(eventName) {
            it('должен запустить процесс эмуляции input, если на текстовом поле произошло событие ' + eventName, function() {
                this.inputEmulator.startEmulate(this.startEvent);

                var event = $.Event(eventName, {currentTarget: this.$input[0]});
                if (eventName !== 'selectionchange') {
                    this.$input.trigger(eventName, event);
                } else {
                    $(document).trigger('selectionchange', event);
                }

                expect(this.inputEmulator.triggerInputEvent).to.have.callCount(1);
            });
        });

    });

    describe('#stopEmulate', function() {

        beforeEach(function() {
            this.sinon.spy(this.inputEmulator, 'triggerInputEvent');
        });

        ['keydown', 'keyup', 'selectionchange'].forEach(function(eventName) {
            it('должен отключить процесс эмуляции input. при этом событие ' + eventName + ' текстового поля не вызывает input событие', function() {
                this.inputEmulator.stopEmulate();

                var event = $.Event(eventName, {currentTarget: this.$input[0]});
                if (eventName !== 'selectionchange') {
                    this.$input.trigger(eventName, event);
                } else {
                    $(document).trigger('selectionchange', event);
                }

                expect(this.inputEmulator.triggerInputEvent).to.have.callCount(0);
            });
        });

        it('должен обнолить предыдущее значение текстового поля', function() {
            this.inputEmulator.currentValue = 'test value';
            this.inputEmulator.stopEmulate();

            expect(this.inputEmulator.currentValue).to.be.equal('');
        });

    });

    describe('#triggerInputEvent', function() {

        beforeEach(function() {
            this.sinon.spy(this.$input, 'trigger');
            this.inputEmulator.$target = this.$input;
        });

        it('не должен вызывать событие input, если текстовое поле не добавлено', function() {
            this.inputEmulator.$target = null;
            this.inputEmulator.triggerInputEvent();

            expect(this.$input.trigger).to.have.callCount(0);
        });

        it('не должен вызывать событие, если текстовое поле не поменяло значение', function() {
            this.$input.val('test');
            this.inputEmulator.currentValue = 'test';
            this.inputEmulator.triggerInputEvent();

            expect(this.$input.trigger).to.have.callCount(0);
        });

        it('должен вызывать input событие у отслеживаемого текстового поля', function() {
            this.inputEmulator.currentValue = 'test';
            this.inputEmulator.triggerInputEvent();

            expect(this.$input.trigger).to.be.calledWith('input');
        });

        it('должен записать текущее значение поля ввода', function() {
            this.inputEmulator.currentValue = '';
            this.$input.val('test');
            this.inputEmulator.triggerInputEvent();

            expect(this.inputEmulator.currentValue).to.be.equal('test');
        });

    });

});
