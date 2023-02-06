describe('Daria.vPreserveScrollPositionMixin', function() {
    beforeEach(function() {
        this.view = ns.View.create('preserve-scroll-position-mixin');
        this.view.$node = $('<div/>');
        this.sinon.stub(this.view.$node, 'offset').returns({ top: 100 });
    });

    describe('#_saveScrollPosition', function() {
        beforeEach(function() {
            this.sinon.stub(this.view._$document, 'scrollTop').returns(10);
        });

        describe('флаг _shouldPreserveScrollPosition не взведён', function() {
            it('ничего не делает', function() {
                this.view._shouldPreserveScrollPosition = false;
                this.view._saveScrollPosition();

                expect(this.view._offsetTop).to.be.equal(null);
            });
        });

        describe('флаг _shouldPreserveScrollPosition взведён', function() {
            it('сохраняет значение отступа ноды вида сверху', function() {
                this.view._shouldPreserveScrollPosition = true;
                this.view._saveScrollPosition();

                expect(this.view._offsetTop).to.not.be.equal(null);
            });

            it('отступ вычисляется относительно верха вьюпорта', function() {
                this.view._shouldPreserveScrollPosition = true;
                this.view._saveScrollPosition();

                expect(this.view._offsetTop).to.be.equal(90);
            });
        });
    });

    describe('_restoreScrollPosition', function() {
        beforeEach(function() {
            this.sinon.stub(this.view._$document, 'scrollTop');
        });

        it('ничего не делает, если _offsetTop равен null', function() {
            this.view._restoreScrollPosition();

            expect(this.view._$document.scrollTop).to.have.callCount(0);
        });

        it('обновляет скролл позицию документа, если она была сохранена ранее', function() {
            this.view._offsetTop = 33;
            this.view._restoreScrollPosition();

            expect(this.view._$document.scrollTop).to.have.callCount(1);
        });

        it('скролл документа вычисляется исходя из текущего отступа ноды и сохранённого отступа ноды ' +
            'относительно вьюпорта', function() {
            this.view._offsetTop = 90;
            this.view._restoreScrollPosition();

            expect(this.view._$document.scrollTop).to.be.calledWith(10);
        });
    });
});
