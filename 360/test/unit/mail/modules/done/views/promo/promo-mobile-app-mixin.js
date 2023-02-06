describe('Daria.vPromoMobileAppMixin', function() {
    beforeEach(function() {
        this.view = ns.View.create('promo-mobile-app-mixin');
    });

    describe('#cleanPhoneNumber', function() {
        it('Должен превратить \'123 abc 456-def-789\' в 123456789', function() {
            expect(this.view.cleanPhoneNumber('123 abc 456-def-789')).to.be.eql('123456789');
        });
    });

    describe('#_getPhoneNumber', function() {
        it('Должен вернуть только набор цифр, очищенный от любого формата', function() {
            this.view.nbPhoneNumber = {
                getValue: function() {
                    return '123 abc 456-def-789';
                }
            };

            expect(this.view._getPhoneNumber()).to.be.eql('123456789');
        });
    });
});
