describe('Daria.vComposeFrom', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-from');
    });

    describe('FIELD_NAME', function() {
        it('Должен быть `from_name`', function() {
            expect(Object.getPrototypeOf(this.view).FIELD_NAME).to.be.equal('from_name');
        });

        it('Должен зависеть от Daria.mComposeState для работы с фокусом', function() {
            expect(this.view.getModel('compose-state')).to.be.ok;
        });
    });

    describe('#shouldResizeByFakeField', function() {
        it('Должен вернуть true, если есть Modernizr.ie11', function() {
            Modernizr.ie11 = true;
            expect(this.view.shouldResizeByFakeField()).to.equal(true);
        });

        it('Должен вернуть true, если есть Modernizr.edge', function() {
            Modernizr.edge = '16.1.146';
            expect(this.view.shouldResizeByFakeField()).to.equal(true);
        });

        it('Должен вернуть false, если нет и Modernizr.ie11 и Modernizr.edge', function() {
            Modernizr.ie11 = false;
            Modernizr.edge = '';
            expect(this.view.shouldResizeByFakeField()).to.equal(false);
        });
    });
});
