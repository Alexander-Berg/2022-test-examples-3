xdescribe('Daria.vMessageHead', function() {
    beforeEach(function() {
        this._origLayout = Daria.layout;
        Daria.layout = '2pane';

        this.params = {ids: '123'};
        this.bMessageHead = ns.View.create('message-body', this.params);

        this.hMessageBody = ns.Model.get('message-body');
        this.hMessage = ns.Model.get('message');

        this.sinon.stub(Jane.watcher, 'set');
    });

    afterEach(function() {
        Daria.layout = this._origLayout;
    });
});
