describe('Daria.vMessageHeadGoToThread', function() {
    beforeEach(function() {
       this.view = ns.View.create('message-head-go-to-thread', {ids: '12344'});
       const mMessage = ns.Model.get('message', {ids: '12344'});
       this.sinon.stubGetModel(this.view, [ mMessage ]);
    });
    describe('#getGoToThreadLink', function() {
        it('Формируем ссылку на тред', function() {
            this.mMessage.setData({
                tid: 't12345',
                fid: '1'
            });

            this.sinon.stub(ns.router, 'generateUrl');
            this.view.getGoToThreadLink();
            expect(ns.router.generateUrl).calledWith('messages', {current_folder: '1', 'thread_id': 't12345'});
        })
    });
});
