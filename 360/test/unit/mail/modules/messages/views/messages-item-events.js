describe('Daria.vMessagesItemEvents', function() {

    beforeEach(function() {
        var viewParams = {ids: '1'};
        this.mMessage = ns.Model.get('message', viewParams);
        setModelsByMock('save-event-by-ics');

        this.view = ns.View.create('messages-item-events', viewParams);

        var stubModel = this.sinon.stub(this.view, 'getModel');
        stubModel.withArgs('message').returns(this.mMessage);
    });

    describe('#_changeEventRequest', function() {

        beforeEach(function() {
            this.mSaveEventByIcs = ns.Model.get('save-event-by-ics', this.mMessage.params);
            this.eventInfo = this.mSaveEventByIcs.getEventInfo();

            this.sinon.stub(this.mSaveEventByIcs, 'getEventInfo').returns(this.eventInfo);

            this.sinon.stub(this.view, 'updateEventInfo');
            this.sinon.stub(this.mMessage, 'toggleMark');

            this.isNew = this.sinon.stub(this.mMessage, 'isNew');
            this.isNew.returns(true);
        });

        it('должна проставляться непрочитанность письма', function() {
            this.view._changeEventRequest([this.mSaveEventByIcs]);

            expect(this.mMessage.toggleMark.called).to.be.equal(true);
        });

        it('не должна проставляться непрочитанность письма, если письмо уже прочитано', function() {
            this.isNew.returns(false);
            this.view._changeEventRequest([this.mSaveEventByIcs]);

            expect(this.mMessage.toggleMark.called).to.be.equal(false);
        });

        it('должны обновляться данные о встречи', function() {
            this.view._changeEventRequest([this.mSaveEventByIcs]);

            expect(this.view.updateEventInfo).to.be.calledWithExactly(this.eventInfo);
        });

        it('не должна проставляться непрочитанность письма, если нет данных о событии', function() {
            this.mSaveEventByIcs.getEventInfo.returns(null);
            this.view._changeEventRequest([this.mSaveEventByIcs]);

            expect(this.mMessage.toggleMark.called).to.be.equal(false);
        });

        it('не должны обновляться данные о встречи, если нет данных о событии', function() {
            this.mSaveEventByIcs.getEventInfo.returns(null);

            this.view._changeEventRequest([this.mSaveEventByIcs]);

            expect(this.view.updateEventInfo).to.have.callCount(0);
        });
    });
});
