describeBlock('i-device-info', function(block) {
    var data = stubData('experiments');

    it('should return js: true by default', function() {
        assert.isTrue(block(data).js);
    });

    describe('with cgi param', function() {
        var FORCE_SZM_COOKIE = 'FORCE_SZM_COOKIE';

        beforeEach(function() {
            data.expFlags['force_szm_cookie'] = FORCE_SZM_COOKIE;
        });

        // Должен пробросить на клиент значение флага force_szm_cookie
        it('should give value of flag "force_szm_cookie" in result object', function() {
            assert.deepEqual(block(data).js, { forcedSzm: FORCE_SZM_COOKIE });
        });
    });
});
