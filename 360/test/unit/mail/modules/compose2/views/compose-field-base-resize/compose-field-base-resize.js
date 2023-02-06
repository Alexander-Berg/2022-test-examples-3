describe('Daria.vComposeFieldBaseResize', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-field-base-resize');
        this.view.FIELD_NAME = 'to';
        this.view.getControllerNode = no.nop;
        this.view.areYabblesDisabled = no.true;
        this.view._$resizeInputController = $('<div />');

        this.mComposeMessage = ns.Model.get('compose-message');
        this.mComposeMessage.setData({});

        this.sinon.stub(this.view, 'getModel')
            .withArgs('compose-message')
            .returns(this.mComposeMessage);

        this.sinon.stub(ns.page.current, 'page').value('compose2');

        this.sinon.stub(this.view._$resizeInputController, 'on');
        this.sinon.stub(this.view._$resizeInputController, 'off');
    });

    describe('#resizeStart', function() {
        it('должны вызвать методы подписки на события и инициализации линейки', function() {
            this.sinon.stub(this.view, '_resizeBindEvents');
            this.sinon.stub(this.view, 'getControllerNode');
            this.sinon.stub(this.view, '_resizeMeasurerInit');

            this.view.resizeStart();

            expect(this.view._resizeBindEvents).to.have.callCount(1);
            expect(this.view.getControllerNode).to.have.callCount(1);
            expect(this.view._resizeMeasurerInit).to.have.callCount(1);
        });

        it('если включены яблы, то подписка на события и инициализация не выполняетя', function() {
            this.sinon.stub(this.view, '_resizeBindEvents');
            this.sinon.stub(this.view, 'getControllerNode');
            this.sinon.stub(this.view, '_resizeMeasurerInit');
            this.sinon.stub(this.view, 'areYabblesDisabled').returns(false);

            this.view.resizeStart();

            expect(this.view._resizeBindEvents.callCount).to.be.equal(0);
            expect(this.view.getControllerNode.callCount).to.be.equal(0);
            expect(this.view._resizeMeasurerInit.callCount).to.be.equal(0);
        });
    });

    describe('#resizeStop', function() {
        it('долдны вызвать методы отписки от событий и дестроя линейки', function() {
            this.sinon.stub(this.view, '_resizeUnbindEvents');
            this.sinon.stub(this.view, '_resizeMeasurerDestroy');

            this.view.resizeStop();

            expect(this.view._resizeUnbindEvents).to.have.callCount(1);
            expect(this.view._resizeMeasurerDestroy).to.have.callCount(1);
        });

        it('если включены яблы, то отписка от событий и дестрой линейки не вызывается', function() {
            this.sinon.stub(this.view, '_resizeUnbindEvents');
            this.sinon.stub(this.view, '_resizeMeasurerDestroy');
            this.sinon.stub(this.view, 'areYabblesDisabled').returns(false);

            this.view.resizeStop();

            expect(this.view._resizeUnbindEvents.callCount).to.be.equal(0);
            expect(this.view._resizeMeasurerDestroy.callCount).to.be.equal(0);
        });
    });

    describe('#_resizeBindEvents', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'on');
            this.sinon.stub(this.view, 'on');
        });

        it('должны подписаться на событие изменения значения поля', function() {
            this.view._resizeBindEvents();
            expect(this.view.on).to.be.calledWithExactly('ns-view-show', this.view._resizeCheck);
            expect(this.mComposeMessage.on).to.be.calledWithExactly('ns-model-changed.to', this.view._resizeCheckDebounce);
        });

        it('должны подписаться на событие нажатия клавиши в поле ввода', function() {
            this.view._resizeBindEvents();
            expect(this.view._$resizeInputController.on).to.be.calledWithExactly('keypress', this.view._resizeKeypressPrevent);
        });
    });

    describe('#_resizeUnbindEvents', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'off');
            this.sinon.stub(this.view, 'off');
        });

        it('должны отписаться от событий изменения значения поля', function() {
            this.view._resizeUnbindEvents();
            expect(this.view.off).to.be.calledWithExactly('ns-view-show', this.view._resizeCheck);
            expect(this.mComposeMessage.off).to.be.calledWithExactly('ns-model-changed.to', this.view._resizeCheckDebounce);
        });

        it('должны отписаться на событие нажатия клавиши в поле ввода', function() {
            this.view._resizeUnbindEvents();
            expect(this.view._$resizeInputController.off).to.be.calledWithExactly('keypress', this.view._resizeKeypressPrevent);
        });
    });

    describe('#_resizeKeypressPrevent', function() {
        it('должен отменить событие нажатия клавиши Enter', function() {
            var event = $.Event('keypress', { which: Jane.Common.keyCode.ENTER });
            this.sinon.stub(event, 'preventDefault');

            this.view._resizeKeypressPrevent(event);
            expect(event.preventDefault).to.have.callCount(1);
        });
    });

    describe('#_resizeCheck', function() {
        beforeEach(function() {
            this.view._$resizeInputController = $('<textarea style="width: 10px;"></textarea>').appendTo('body');
            this.view._$resizeMeasurer = $('<div>test</div>').appendTo('body');
            this.view._resizeMeasurerHeight = this.view._$resizeMeasurer.height();
            this.view.getFieldData = _.noop;
        });

        afterEach(function() {
            this.view._$resizeInputController.remove();
            this.view._$resizeMeasurer.remove();
        });

        it('восстанавливает высоту в 1 строку, если не определена высота строки', function() {
            this.view._resizeMeasurerHeight = 0;
            var spyHeight = this.sinon.spy(this.view._$resizeInputController, 'height');
            var spyToggleClass = this.sinon.spy(this.view._$resizeInputController, 'toggleClass');

            this.view._resizeCheck();

            expect(spyHeight).to.be.calledWithExactly('auto');
            expect(spyToggleClass).to.be.calledWithExactly('is-multiline', false);
        });

        it('если содержимое поля не влазит в 1 строку, выполняется увеличение высоты поля ввод', function() {
            this.sinon.stub(this.view, 'getFieldData').returns('1 2');
            var spyHeight = this.sinon.spy(this.view._$resizeInputController, 'height');
            var spyToggleClass = this.sinon.spy(this.view._$resizeInputController, 'toggleClass');

            this.view._resizeCheck();

            expect(spyHeight).to.be.calledWithExactly(this.view._resizeMeasurerHeight * 2);
            expect(spyToggleClass).to.be.calledWithExactly('is-multiline', false);
        });

        it('если строк больше 3, то появляется скрол, количество строк редактора остается 3', function() {
            this.sinon.stub(this.view, 'getFieldData').returns('1 2 3 4');
            var spyHeight = this.sinon.spy(this.view._$resizeInputController, 'height');
            var spyToggleClass = this.sinon.spy(this.view._$resizeInputController, 'toggleClass');

            this.view._resizeCheck();

            expect(spyHeight).to.be.calledWithExactly(this.view._resizeMeasurerHeight * 3);
            expect(spyToggleClass).to.be.calledWithExactly('is-multiline', true);
        });
    });
});

