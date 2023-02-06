describe('i-request_type_ajax', function() {
    it('should generate unique callback names', function() {
        var ajax1 = new BEM.blocks['i-request_type_ajax']();
        var ajax2 = new BEM.blocks['i-request_type_ajax']();

        BEM.decl({ block: 'i-request-test-block', baseBlock: 'i-request_type_ajax' }, {
            get: function() {
            }
        });
        var ajax3 = new BEM.blocks['i-request-test-block']();

        // все вызовы jsonpCallback() должны идти подряд
        var cb1 = ajax1.params.jsonpCallback();
        var cb2 = ajax1.params.jsonpCallback(); // вызов из того же экземпляра класса
        var cb3 = ajax2.params.jsonpCallback(); // вызов из другого экземпляра класса
        var cb4 = ajax3.params.jsonpCallback(); // вызов из экземпляра класса-потомка

        var uniqNames = {};
        assert.equal(Object.keys(uniqNames).length, 0, 'Sanity check');
        uniqNames[cb1] = 'cb1';
        uniqNames[cb2] = 'cb2';
        uniqNames[cb3] = 'cb3';
        uniqNames[cb4] = 'cb4';
        assert.equal(
            Object.keys(uniqNames).length,
            4,
            'Callbacks for consequent JSONP should have unique names: object ' +
            JSON.stringify(uniqNames) + ' should have 4 entries'
        );
    });
});
