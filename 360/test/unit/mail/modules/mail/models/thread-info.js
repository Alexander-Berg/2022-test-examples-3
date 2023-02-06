describe('Daria.mThreadInfo', function() {
    beforeEach(function() {
        this.mThreadInfo = ns.Model.get('thread-info', { thread_id: 't1' });
        this.mMessage = ns.Model.get('message', { ids: 't1' });

        this.sinon.stub(this.mThreadInfo, 'getRequestParams').returns({ thread_id: 't1' });
        this.sinon.stub(ns, 'http').returns(new Vow.Promise());
    });

    describe('#request', function() {
        it('Если не 3pane, то не подгружаем тредные данные', function() {
            this.sinon.stub(Daria, 'is3pane').returns(false);

            this.mThreadInfo.request();
            expect(ns.http.called).to.be.equal(false);
        });

        it('Если 3pane и не фейковый тред, то не подгружаем тредные данные', function() {
            this.sinon.stub(Daria, 'is3pane').returns(true);
            this.sinon.stub(this.mMessage, 'get').withArgs('.FAKE_THREAD_DATA').returns(false);

            this.mThreadInfo.request();
            expect(ns.http.called).to.be.equal(false);
        });

        it('Если 3pane и фейковый тред, то подгружаем тредные данные', function() {
            this.sinon.stub(Daria, 'is3pane').returns(true);
            this.sinon.stub(this.mMessage, 'get').withArgs('.FAKE_THREAD_DATA').returns(true);

            this.mThreadInfo.request();
            expect(ns.http.called).to.be.equal(true);
        });
    });
});
