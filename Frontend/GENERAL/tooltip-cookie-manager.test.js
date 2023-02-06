describe('tooltip-cookie-manager', function() {
    var block,
        stubs,
        ypCookie,
        now = 1469438259415,
        timestamp = Math.round(now / 1000),
        yCookie = BEM.blocks['i-cookie'].get();

    before(function() {
        stubs = stubObjectMethods(yCookie, {
            ypRead: function() {
                return ypCookie;
            },

            yp: function(name, value, timestamp) {
                ypCookie[name] = {
                    value: value,
                    timestamp: timestamp
                };
            }
        });
    });

    beforeEach(function() {
        ypCookie = {};
        stubs.init();
        sinon.stub(Date, 'now').returns(now);
        block = BEM.blocks['tooltip-cookie-manager'].get();
    });

    afterEach(function() {
        stubs.restore();
        Date.now.restore();
    });

    it('should create correct stltp subcookie record from arguments', function() {
        block.setCookie('fid', 'promo-item', 1);
        assert.equal('fid_promo-item_1_' + timestamp, ypCookie.stltp.value);
    });

    it('should correct rewrite existing stltp subcookie', function() {
        ypCookie.stltp = {};
        ypCookie.stltp.value = 'fid3_promo-item3_10_1466604324:fid_promo-item_1_1466604324';
        block.setCookie('fid', 'promo-item', 2);
        assert.equal('fid3_promo-item3_10_1466604324:fid_promo-item_2_' + timestamp, ypCookie.stltp.value);
    });
});
