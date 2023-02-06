describe('serp-metrika', function() {
    var counter,
        location,
        serpMetrika;

    beforeEach(function() {
        location = BEM.blocks.location.getInstance();
        serpMetrika = buildDomBlock('serp-metrika', {
            block: 'serp-metrika',
            js: { counter: 'Test' }
        });

        window.yaCounterTest = { hit: sinon.stub() };
        counter = window.yaCounterTest;
    });

    afterEach(function() {
        window.yaCounterTest = undefined;
        serpMetrika.params = {};

        BEM.DOM.destruct(serpMetrika.domElem);
    });

    it('counter.hit must be called with appropiate arguments', function() {
        location.trigger('change', { referer: 'referer', url: 'url' });
        assert.calledWith(counter.hit, 'url', { title: document.title, referer: 'referer' });
    });
});
