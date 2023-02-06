describe('Daria.vQuickReplyForm', function() {
    beforeEach(function() {
        this.mMessageDraft = ns.Model.get('message-draft').setData({});
        this.mQuickReplyState = ns.Model.get('quick-reply-state').setData({});
        this.view = ns.View.create('quick-reply-form', { 'ids': '1', 'qrIds': '2' });

        var stubModels = this.sinon.stub(this.view, 'getModel');
        stubModels.withArgs('message-draft').returns(this.mMessageDraft);
        stubModels.withArgs('quick-reply-state').returns(this.mQuickReplyState);

        this.sinon.stub(this.mQuickReplyState, 'isReplyAll').returns(true);

        this.view.vCompose = ns.View.create('quick-reply');
        this.view.vCompose.$node = $('<div />');
        this.view.vCompose.node = this.view.vCompose.$node[0];

        this.view.$node = $('<div />');
        this.view.node = this.view.$node[0];

        this.sinon.stub(ns.events, 'trigger');
    });

    afterEach(function() {
        delete this.view.$node;
        delete this.view.node;
        this.view.destroy();
    });

    describe('#getComposeParams', function() {
        beforeEach(function() {
            this.sinon.stub(ns, 'forcedRequest').withArgs('message-draft')
                .returns(new $.Deferred().resolve([ this.mMessageDraft ]).promise());
        });

        xit('Если есть черновик, должен веруть параметры с черновиком без ответа', function() {
            this.sinon.stub(this.mMessageDraft, 'getDraftMid').returns('3');
            return this.view.getComposeParams().then(function(params) {
                expect(params).to.have.keys('ids', 'qrIds', '_cuid');
                expect(params.ids).to.be.equal('3');
                expect(params.qrIds).to.be.equal('1');
            });
        });

        it('Если нет черновика, должен вернуть параметры с ответом на письмо', function() {
            return this.view.getComposeParams().then(function(params) {
                expect(params).to.have.keys('ids', 'qrIds', '_cuid', 'oper');
                expect(params.ids).to.be.equal('1');
                expect(params.qrIds).to.be.equal('1');
                expect(params.oper).to.be.equal('reply-all');
            });
        });

        it('Если нет черновика и отвечаем одному, должен вернуть параметр "oper" равным "reply"', function() {
            this.mQuickReplyState.isReplyAll.returns(false);
            return this.view.getComposeParams().then(function(params) {
                expect(params.oper).to.be.equal('reply');
            });
        });
    });

    describe('#onHide', function() {
        beforeEach(function() {
            this.stubDestroy = this.sinon.stub(this.view.vCompose, 'destroy');
            this.view.onHide();
        });

        it('Должен уничтожить вид композа', function() {
            expect(this.stubDestroy).to.have.callCount(1);
        });
    });

    describe('#onAfterRender', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.sinon.stub(this.view.node, 'appendChild');
            this.sinon.stub(Jane.DOM, 'scrollIntoView');
            this.sinon.stub(this.mQuickReplyState, 'setIfChanged');

            this.view.onAfterRender();
        });

        it('Должен установить признак фактического показа формы QR', function() {
            expect(this.mQuickReplyState.setIfChanged).to.be.calledWithExactly('.formIsShown', true);
        });

        /*
        it('Должен подскролить к форме QR', function() {
            expect(Jane.DOM.scrollIntoView).to.be.calledWithExactly(this.view.node, {
                'block': 'end',
                'behavior': 'smooth'
            });
        });
        */
    });
});

