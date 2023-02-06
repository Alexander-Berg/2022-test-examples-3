xdescribe('Daria.vLabels', function() {

    describe('#_logClickAfterGo', function() {

        beforeEach(function() {
            this.vLabels = ns.View.create('labels');
            this.sinon.stub(ns.action, 'run');
            this.sinon.stub(Jane.ErrorLog, 'send');
        });

        it('Должен вызвать экшен с ивентом Label_unread, если label = unread', function() {
            this.vLabels._logClickAfterGo('unread');

            expect(ns.action.run).to.be.calledWith('trigger-hotkeys-promo-bubble', {
                eventName: 'Label_unread'
            });
        });

        it('Должен вызвать экшен с ивентом Label_important, если label = important', function() {
            this.vLabels._logClickAfterGo('important');

            expect(ns.action.run).to.be.calledWith('trigger-hotkeys-promo-bubble', {
                eventName: 'Label_important'
            });
        });

        it('Не должен вызвать экшен, если label = attachments', function() {
            this.vLabels._logClickAfterGo('attachments');

            expect(ns.action.run).to.have.callCount(0);
        });

    });

    describe('#_onToggleUnusedLink', function() {

        beforeEach(function() {
            this.vLabels = ns.View.create('labels');
            this.vLabels.$userLabelsNode = $();

            this.sinon.stub(this.vLabels.$userLabelsNode, 'toggleClass').value($.noop);
            this.sinon.stub(ns.events, 'trigger');
        });

        it('shoud call toggleClass method with argument "jane-nav-column-user-labels_hidden-empty"', function() {
            this.vLabels._onToggleUnusedLink();
            expect(this.vLabels.$userLabelsNode.toggleClass).to.be.calledWithExactly('jane-nav-column-user-labels_hidden-empty');
        });

        it('shoud call Jane.events.trigger method with argument "shortcuts.update-labels-list"', function() {
            this.vLabels._onToggleUnusedLink();
            expect(ns.events.trigger).to.be.calledWithExactly('shortcuts.update-labels-list');
        });
    });

});
