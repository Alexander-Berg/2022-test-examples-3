describe('Daria.vDone', function() {
    beforeEach(function() {
        this.mDone = ns.Model.get('done');

        this.view = ns.View.create('done');
        this.sinon.stub(this.view, 'getModel')
            .withArgs('done').returns(this.mDone);

        this.sinon.stubMethods(this.view, [
            'metrika'
        ]);
    });

    describe('#onHtmlInit', function() {
        beforeEach(function() {
            this.sinon.stub(nb, 'init');
            this.view.onHtmlInit();
        });

        it('Инициализирует наноострова', function() {
            expect(nb.init).to.have.callCount(1);
        });
    });

    describe('#onHtmlDestroy', function() {
        beforeEach(function() {
            this.sinon.stub(nb, 'destroy');
            this.view.onHtmlDestroy();
        });

        it('Деинициализирует наноострова', function() {
            expect(nb.destroy).to.have.callCount(1);
        });

    });

    describe('#onShow', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.view, [
                'startRedirectTimeout',
                'showDirect'
            ]);

            this.view.onShow();
        });

        it('Должен запустить таймер на редирект', function() {
            expect(this.view.startRedirectTimeout).to.have.callCount(1);
        });
    });

    describe('#onPageBeforeLoad', function() {
        beforeEach(function() {
            this.sinon.stub(this.mDone, 'destroy');
            this.sinon.stub(this.view, 'invalidate');
            this.sinon.stub(this.view, 'stopRedirectTimeout');

            this.view.onPageBeforeLoad('ns-page-before-load', [ { 'page': 'done' }, { 'page': 'inbox' } ]);
        });

        it('Уничтожает модель mDone', function() {
            expect(this.mDone.destroy).to.have.callCount(1);
        });

        it('Должен инвалидировать вид', function() {
            expect(this.view.invalidate).to.have.callCount(1);
        });

        it('Должен остановать таймер на редирект', function() {
            expect(this.view.stopRedirectTimeout).to.have.callCount(1);
        });
    });
});

