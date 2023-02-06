beforeEach(function() {
    this.sinon = sinon.sandbox.create({
        useFakeServer: true
    });

    this.sinon.spy(ns, 'renderString');
    this.sinon.spy(ns, 'renderNode');
    this.sinon.stub(ns.history, 'pushState');
    this.sinon.stub(ns.history, 'replaceState');
    this.sinon.stub(ns.router, 'URL_FIRST_SYMBOL').value('/');
    this.sinon.stub(ns.page, 'HOME_HASH').value('/');
    this.sinon.stub(ns.page, '_go').callsFake(function(url, historyAction) {
        return [ url, historyAction ];
    });

    this.sinon.stub(ns.Update.prototype, 'applyTemplate').callsFake(function(tree) {
        return ns.renderString(tree, null, '');
    });

    this.sinon.stub(ns.log, 'exception').callsFake(function(a, b, c) {
        console.error('ns.log.exception', a, b.message || b, b.stack, c);
    });
});

afterEach(function() {
    this.sinon.restore();
    ns.reset();
});
