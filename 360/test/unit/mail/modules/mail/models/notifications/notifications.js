describe('Daria.mNotifications', function() {

    describe('#addNotification', function() {

        beforeEach(function() {
            this.mNotifications = ns.Model.get('notifications');
            this.sinon.stub(this.mNotifications, '_addNotification');
            this.sinon.stub(this.mNotifications, '_tryRemoveLastNotification');

            this.mNotificationsState = ns.Model.get('notifications-state');
            this.sinon.stub(this.mNotificationsState, 'isExpanded');
        });

        it('Должен добавить нотификацию в очередь, если список развернут', function() {
            this.mNotificationsState.isExpanded.returns(true);

            expect(this.mNotifications.get('.queue').length).to.be.equal(0);

            this.mNotifications.addNotification({ type: 'message' });

            expect(this.mNotifications.get('.queue').length).to.be.equal(1);
            expect(this.mNotifications._addNotification).to.have.callCount(0);
        });

        it('Не должен добавлять нотификацию в список, если последняя VIP, а пришла не VIP', function() {
            this.mNotificationsState.isExpanded.returns(false);

            this.mNotifications.insert(
                ns.Model.get('notifications-item', { id: 1 }).setData({ vip: true })
            );

            this.mNotifications.addNotification({ vip: false });

            expect(this.mNotifications._addNotification).to.have.callCount(0);
        });

        it('Должен добавить нотификацию в список, если последняя VIP и пришла VIP', function() {
            this.mNotificationsState.isExpanded.returns(false);

            this.mNotifications.insert(
                ns.Model.get('notifications-item', { id: 1 }).setData({ vip: true })
            );

            this.mNotifications.addNotification({ vip: true });

            expect(this.mNotifications._addNotification).to.have.callCount(1);
        });

        it('Должен добавить нотификацию в список, если последня не VIP', function() {
            this.mNotificationsState.isExpanded.returns(false);

            this.mNotifications.insert(
                ns.Model.get('notifications-item', { id: 1 }).setData({ vip: false })
            );

            this.mNotifications.addNotification({ vip: false });

            expect(this.mNotifications._addNotification).to.have.callCount(1);
        });

    });

    describe('#insertFromQueue', function() {

        beforeEach(function() {
            this.mNotifications = ns.Model.get('notifications');
            this.sinon.stub(this.mNotifications, '_tryRemoveLastNotification');
        });

        it('Не должен вставлять нотификации из очереди, если она пустая', function() {
            expect(this.mNotifications.get('.queue').length).to.be.equal(0);
            expect(this.mNotifications.models.length).to.be.equal(0);

            this.mNotifications.insertFromQueue();

            expect(this.mNotifications.get('.queue').length).to.be.equal(0);
            expect(this.mNotifications.models.length).to.be.equal(0);
        });

        it('Должен вставить нотификации из очереди в основной список', function() {
            this.mNotifications.setData({
                queue: [
                    {
                        type: 'message'
                    },
                    {
                        type: 'message'
                    }
                ]
            });
            expect(this.mNotifications.models.length).to.be.equal(0);

            this.mNotifications.insertFromQueue();

            expect(this.mNotifications.models.length).to.be.equal(2);
            expect(this.mNotifications.get('.queue').length).to.be.equal(0);
        });

    });

});
