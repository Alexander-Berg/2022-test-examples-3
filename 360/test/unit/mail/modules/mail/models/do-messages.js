describe('Daria.mDoMessages', function() {

    beforeEach(function() {
        this.sinon.stub(ns, 'forcedRequest');
    });

    it('должен запросить модели сообщений, если операция notspam', function() {
        var model = ns.Model.get('do-messages', {
            ids: ['1', '2'],
            action: 'notspam'
        });

        model.setData({});

        expect(ns.forcedRequest).to.be.calledWith([
            ns.Model.get('message', {ids: '1'}),
            ns.Model.get('message', {ids: '2'})
        ]);
    });

    it('не должен запросить модели сообщений, если операция не notspam', function() {
        var model = ns.Model.get('do-messages', {
            ids: ['1', '2'],
            action: 'mark'
        });

        model.setData({});

        expect(ns.forcedRequest).to.have.callCount(0);
    });

});
