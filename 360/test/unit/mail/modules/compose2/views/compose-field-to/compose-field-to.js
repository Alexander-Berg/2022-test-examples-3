describe('Daria.vComposeFieldTo', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-field-to');
        this.view.$node = $('<div><div class="js-cc-link-compose"></div><div class="js-bcc-link-compose"></div></div>');
        this.parentView = ns.View.infoLite('compose-field-base');
    });

    describe('#onShow', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'resizeStart');
            this.sinon.stub(this.view, 'suggestStart');
            this.sinon.stub(this.view, 'yabbleStart');
            this.sinon.stub(this.parentView.methods, 'onShow');
            this.view.onShow();
        });

        it('должен вызвать старт ресайзера', function() {
            expect(this.view.resizeStart).to.have.callCount(1);
        });

        it('должен вызвать старт саджеста', function() {
            expect(this.view.suggestStart).to.have.callCount(1);
        });

        it('должен вызвать старт яблов', function() {
            expect(this.view.yabbleStart).to.have.callCount(1);
        });

        it('должен вызвать onShow предка', function() {
            expect(this.parentView.methods.onShow).to.have.callCount(1);
        });

        it('старт яблов должен быть вызван после старта саджеста', function() {
            expect(this.view.yabbleStart).to.be.calledAfter(this.view.suggestStart);
        });
    });

    describe('#onHide', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'resizeStop');
            this.sinon.stub(this.view, 'suggestStop');
            this.sinon.stub(this.view, 'yabbleStop');
            this.sinon.stub(this.parentView.methods, 'onHide');
            this.view.onHide();
        });

        it('должен вызвать стоп ресайзера', function() {
            expect(this.view.resizeStop).to.have.callCount(1);
        });

        it('должен вызвать стоп саджеста', function() {
            expect(this.view.suggestStop).to.have.callCount(1);
        });

        it('должен вызвать стоп яблов', function() {
            expect(this.view.yabbleStop).to.have.callCount(1);
        });

        it('должен вызвать onHide предка', function() {
            expect(this.parentView.methods.onHide).to.have.callCount(1);
        });

        it('стоп яблов должен быть вызван после стопа саджеста', function() {
            expect(this.view.yabbleStop).to.be.calledAfter(this.view.suggestStop);
        });
    });

    describe('FIELD_NAME', function() {
        it('Должен быть `to`', function() {
            expect(Object.getPrototypeOf(this.view).FIELD_NAME).to.be.equal('to');
        });

        it('Должен зависеть от Daria.mComposeState для работы с фокусом', function() {
            expect(this.view.getModel('compose-state')).to.be.ok;
        });
    });
});
