describe('Daria.mDimensions', function() {

    describe('Инициализация ->', function() {

        it('должен инициализировать в валидном состоянии', function() {
            expect(ns.Model.get('dimensions').isValid()).to.be.equal(true);
        });

        it('должен повесить обработчик onresize', function() {
            this.sinon.stub($.fn, 'on');
            ns.Model.get('dimensions');

            expect($.fn.on).to.be.calledWith('resize');
        });

    });

    describe('onResize ->', function() {

        beforeEach(function() {
            this.model = ns.Model.get('dimensions');

            this.sinon.spy(this.model, 'clean');
            this.sinon.spy(this.model, 'trigger');

            var event = $.Event('resize');
            event.originalEvent = $.Event('resize');
            $(window).trigger(event);

        });

        it('должен сбросить состояние', function() {
            expect(this.model.clean).to.have.callCount(1);
        });

        it('должен сбросить событие "daria:mDimensions:resize"', function() {
            expect(this.model.trigger).to.be.calledWith('resize');
        });

    });

    getGetterSuite('getDocumentHeight', 'height');
    getGetterSuite('getDocumentWidth', 'width');

    getGetterSuite('getWindowHeight', 'height');
    getGetterSuite('getWindowWidth', 'width');

    function getGetterSuite(method, dimension) {

        describe('#' + method, function() {

            beforeEach(function() {
                this.model = ns.Model.get('dimensions');
                this.sinon.stub($.fn, dimension).returns(1);
            });

            it('должен вернуть значение', function() {
                expect(this.model[method]()).to.be.equal(1);
            });

            it('должен рассчитать значение, если нет', function() {
                this.model[method]();
                expect($.fn[dimension]).to.have.callCount(1);
            });

            it('не должен рассчитать значение, если оно есть', function() {
                this.model[method]();
                this.model[method]();
                expect($.fn[dimension]).to.have.callCount(1);
            });

        });

    }

});
