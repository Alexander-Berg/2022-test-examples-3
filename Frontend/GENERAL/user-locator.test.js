describe('user-locator', function() {
    var block = BEM.blocks['user-locator'],
        sandbox;

    beforeEach(function() {
        sandbox = sinon.createSandbox();
    });

    afterEach(function() {
        sandbox.restore();
    });

    it('should exist', function() {
        assert.ok(block, 'Block user-locator does not exist');
    });
});
