describe('Daria.vLayoutSwitchPopup', function() {
    beforeEach(function() {
        this.compactMode = Daria.CompactMode;
        this.vLayoutSwitchPopup = ns.View.create('layout-switch-popup');
        return this.vLayoutSwitchPopup.update();
    });

    describe('#toggleCompactMode', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.CompactMode, 'toggleCompact');
            this.sinon.stub(Daria.CompactMode, 'isCompactHeader');
            this.sinon.stub($.fn, 'removeClass');
            this.sinon.stub(ns.page, 'go');
            this.sinon.stub(ns.page, 'currentUrl').value('#categories');
            this.vLayoutSwitchPopup.nbPopup = {
                close: this.sinon.stub()
            };
        });

        it('должен вызвать toggleCompact в CompactMode', function() {
            this.vLayoutSwitchPopup.toggleCompactMode();

            expect(this.compactMode.toggleCompact).to.have.callCount(1);
        });

        it('должен удалить класс узкой шапки если выключаем режим компактного меню', function() {
            Daria.CompactMode.isCompactHeader.returns(false);
            this.vLayoutSwitchPopup.toggleCompactMode();

            expect($.fn.removeClass).to.have.callCount(1);
            expect($.fn.removeClass).to.be.calledWith('mail-Page_minified');
        });

        it('должен закрыть попап', function() {
            this.vLayoutSwitchPopup.toggleCompactMode();

            expect(this.vLayoutSwitchPopup.nbPopup.close).to.have.callCount(1);
        });

        it('должен обновить приложение', function() {
            this.vLayoutSwitchPopup.toggleCompactMode();

            expect(ns.page.go).to.have.callCount(1);
        });
    });

    describe('#toggleCompactHeader', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.CompactMode, 'toggleCompactHeader');
            this.sinon.stub(Daria.CompactMode, 'isCompactHeader');
            this.sinon.stub($.fn, 'removeClass');
            this.sinon.stub(ns.page, 'go');
            this.sinon.stub(ns.page, 'currentUrl').value('#categories');
            this.vLayoutSwitchPopup.nbPopup = {
                close: this.sinon.stub()
            };
        });

        it('должен вызвать toggleCompactHeader в CompactMode', function() {
            this.vLayoutSwitchPopup.toggleCompactHeader();

            expect(this.compactMode.toggleCompactHeader).to.have.callCount(1);
        });

        it('должен удалить класс узкой шапки если выключаем режим компактного меню', function() {
            Daria.CompactMode.isCompactHeader.returns(false);
            this.vLayoutSwitchPopup.toggleCompactHeader();

            expect($.fn.removeClass).to.have.callCount(1);
            expect($.fn.removeClass).to.be.calledWith('mail-Page_minified');
        });

        it('должен закрыть попап', function() {
            this.vLayoutSwitchPopup.toggleCompactHeader();

            expect(this.vLayoutSwitchPopup.nbPopup.close).to.have.callCount(1);
        });

        it('должен обновить приложение', function() {
            this.vLayoutSwitchPopup.toggleCompactHeader();

            expect(ns.page.go).to.have.callCount(1);
        });
    });
});
