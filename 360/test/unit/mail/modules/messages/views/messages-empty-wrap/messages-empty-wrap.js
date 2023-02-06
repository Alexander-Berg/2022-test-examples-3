describe('Daria.vMessagesEmptyWrap', function() {

    beforeEach(function() {
        this.params = {current_folder: '1'};
        this.mMessages = ns.Model.get('messages', this.params).setData({
            message: []
        });

        this.view = ns.View.create('messages-empty-wrap', this.params);
        this.sinon.stub(this.view, 'forceUpdate');

        ns.Model.get('tabs').setData([
            { id: 'relevant', title: 'test-title', description: 'test-description' }
        ]);
    });

    describe('patchLayout', function() {

        it('должен вернуть "layout-messages-empty", если список не пустой', function() {
            this.sinon.stub(this.mMessages, 'isEmptyList').returns(false);
            expect(this.view.patchLayout()).to.be.equal('layout-messages-empty');
        });

        it('должен вернуть "layout-messages-empty-view", если список пустой', function() {
            this.sinon.stub(this.mMessages, 'isEmptyList').returns(true);
            expect(this.view.patchLayout()).to.be.equal('layout-messages-empty-view');
        });

    });

    describe('Перерисовки при изменении mMessages ->', function() {

        describe('Был пустой ->', function() {

            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', this.params).setData({
                    message: []
                });
            });

            it('должен вызвать перерисовку, если появились письма', function() {
                return this.view.update().then(function() {
                    this.mMessages = ns.Model.get('messages', this.params).setData({
                        message: [
                            {mid: '1', tid: 't1'}
                        ]
                    });

                    expect(this.view.forceUpdate).to.have.callCount(1);
                }, null, this);
            });

            it('не должен вызвать перерисовку, если письма не появились', function() {
                return this.view.update().then(function() {
                    this.mMessages = ns.Model.get('messages', this.params).setData({
                        message: []
                    });

                    expect(this.view.forceUpdate).to.have.callCount(0);
                }, null, this);
            });

        });

        describe('Был непустой ->', function() {

            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', this.params).setData({
                    message: [
                        {mid: '1', tid: 't1'}
                    ]
                });
            });

            it('должен вызвать перерисовку, если список стал пустым', function() {
                return this.view.update().then(function() {
                    this.mMessages = ns.Model.get('messages', this.params).setData({
                        message: []
                    });

                    expect(this.view.forceUpdate).to.have.callCount(1);
                }, null, this);
            });

            it('не должен вызвать перерисовку, если список остался непустым', function() {
                return this.view.update().then(function() {
                    this.mMessages = ns.Model.get('messages', this.params).setData({
                        message: [
                            {mid: '1', tid: 't1'},
                            {mid: '2', tid: 't2'}
                        ]
                    });

                    expect(this.view.forceUpdate).to.have.callCount(0);
                }, null, this);
            });

        });

    });

});
