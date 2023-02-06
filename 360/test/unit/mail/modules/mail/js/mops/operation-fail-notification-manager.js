describe('OperationFailNotificationManager', function() {
    beforeEach(function() {
        this.notificationManager = new Daria.MOPS.OperationFailNotificationManager();
    });

    describe('#registerOperationFail', function() {
        beforeEach(function() {
            this.sinon.stub(this.notificationManager, '_registerOperationFail');
            this.sinon.stub(this.notificationManager, '_shouldShowNotification');
            this.sinon.stub(this.notificationManager, 'showNotification');
        });

        it('Если не нужно показывать нотификацию, то должен увеличеть счётчик фейлов операций', function() {
            this.notificationManager._shouldShowNotification.returns(false);

            this.notificationManager.registerOperationFail('move', { count: 1, ids: [ 123 ] });

            expect(this.notificationManager._registerOperationFail).has.callCount(1);
            expect(this.notificationManager._shouldShowNotification)
                .to.have.been.calledWith({ count: 1, ids: [ 123 ] });
            expect(this.notificationManager.showNotification).has.callCount(0);
        });

        it('Если нужно показывать нотификацию, то должен увеличеть счётчик фейлов операций и ' +
            'счётчик показа нотификаций',
            function() {
                const operationParams = { count: 5, ids: [ 1, 2, 3, 4, 5 ] };

                this.notificationManager._shouldShowNotification.returns(true);

                this.notificationManager.registerOperationFail('move', operationParams);

                expect(this.notificationManager._registerOperationFail).has.callCount(1);
                expect(this.notificationManager._shouldShowNotification)
                    .to.have.been.calledWith(operationParams);
                expect(this.notificationManager.showNotification).has.callCount(1);
                expect(this.notificationManager.showNotification).to.have.been.calledWith('move', operationParams);
            }
        );
    });

    describe('#showNotification', function() {
        beforeEach(function() {
            this.sinon.stub(this.notificationManager, '_setNotificationShow');
            this.sinon.stub(Daria.Statusline, 'show');
            this.sinon.stub(this.notificationManager, '_logNotificationShow');
        });

        it('Должен проставить, что нотификация показалась', function() {
            this.notificationManager.showNotification('move', { count: 123 });

            expect(this.notificationManager._setNotificationShow).has.callCount(1);
        });

        it('Должен вызвать метод Daria.Statusline.show', function() {
            this.notificationManager.showNotification('move', { count: 123 });

            expect(Daria.Statusline.show).has.callCount(1);
            expect(Daria.Statusline.show).to.have.been.calledWith(this.sinon.match({
                name: 'mops_failed',
                speed: 'fast',
                hideOnTimeout: true,
                timeout: 20000 // 20 сек
            }));
        });

        it('Должен залогировать показ нотификации', function() {
            this.notificationManager.showNotification('move', { count: 123 });

            expect(this.notificationManager._logNotificationShow).has.callCount(1);
            expect(this.notificationManager._logNotificationShow).to.have.been.calledWith('move', { count: 123 });
        })
    });

    describe('#_shouldShowNotification', function() {
        beforeEach(function() {
            this.sinon.stub(this.notificationManager._failOperationsQueue, 'hasMaxEventsForTimeInterval')
                .returns(false);
        });

        it('Если кол-во зафейленных операций превосходит порог, то показываем нотификацию', function() {
            this.notificationManager._failOperationsQueue.hasMaxEventsForTimeInterval.returns(true);
            expect(this.notificationManager._shouldShowNotification({ count: 1 })).to.equal(true);
        });

        it('Если операция была с папкой, то показываем нотификацию', function() {
            expect(this.notificationManager._shouldShowNotification({ fid: '123' })).to.equal(true);
        });

        it('Если операция была с табом, то показываем нотификацию', function() {
            expect(this.notificationManager._shouldShowNotification({ tabId: 'news' })).to.equal(true);
        });

        it('Если есть fid и count в параметрах, то приоритетным считается count, при показе нотификации', function() {
            expect(this.notificationManager._shouldShowNotification({ count: 1, fid: '123' })).to.equal(false);
        });

        it('Если есть tabId и count в параметрах, то приоритетным считается count, при показе нотификации', function() {
            expect(this.notificationManager._shouldShowNotification({ count: 1, tabId: 'news' })).to.equal(false);
        });

        it('Если кол-во сообщений в одной зафейленной операции превосходит порог, то показываем нотификацию',
            function() {
                var msgsThreshold = this.notificationManager._threshold.messages;

                expect(this.notificationManager._shouldShowNotification({ count: msgsThreshold }))
                    .to.equal(true);
            }
        );

        it('Если кол-во зафейленных операций превосходит порог и ' +
            'кол-во сообщений в зафейленной операции превосходит порог, но нотификацию уже показывали, ' +
            'то не показываем повторно',
            function() {
                var thresholdNotifications = this.notificationManager._threshold.notifications;
                var msgsThreshold = this.notificationManager._threshold.messages;

                this.notificationManager._failOperationsQueue.hasMaxEventsForTimeInterval.returns(true);
                this.notificationManager._notificationShowCount = thresholdNotifications;

                expect(this.notificationManager._shouldShowNotification({ count: msgsThreshold })).to.equal(false);
            }
        );
        it('Если кол-во зафейленных операций и кол-во сообщений не превосходит порог, то не показываем нотификацию',
            function() {
                expect(this.notificationManager._shouldShowNotification()).to.equal(false);
            }
        );
    });

    describe('#_logNotificationShow', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.ErrorLog, 'send');
        });

        it('Должен вызвать Jane.ErrorLog.send c правильными параметрами', function() {
            this.notificationManager._logNotificationShow('move', { count: 5 });

            expect(Jane.ErrorLog.send).has.callCount(1);
            expect(Jane.ErrorLog.send).to.have.been.calledWith({
                type: 'mops_fail_notification_show',
                action: 'move',
                msgsCount: 5
            });
        });

        it('Если вызвали метод с fid, то должен залогировать fid', function() {
            this.notificationManager._logNotificationShow('move', { count: 5, fid: '123' });

            expect(Jane.ErrorLog.send).has.callCount(1);
            expect(Jane.ErrorLog.send).to.have.been.calledWith({
                type: 'mops_fail_notification_show',
                action: 'move',
                fid: '123'
            });
        });

        it('Если вызвали метод с tabId, то должен залогировать tabId', function() {
            this.notificationManager._logNotificationShow('move', { count: 5, fid: '123', tabId: 'news' });

            expect(Jane.ErrorLog.send).has.callCount(1);
            expect(Jane.ErrorLog.send).to.have.been.calledWith({
                type: 'mops_fail_notification_show',
                action: 'move',
                fid: '123',
                tabId: 'news'
            });
        });

        it('Если вызвали метод без аргументов, то метод должен правильно залогировать такую ситуацию', function() {
            this.notificationManager._logNotificationShow();

            expect(Jane.ErrorLog.send).has.callCount(1);
            expect(Jane.ErrorLog.send).to.have.been.calledWith({
                type: 'mops_fail_notification_show',
                action: 'unknown',
                msgsCount: 'unknown'
            });
        });
    });

    describe('#_registerOperationFail', function() {
        it('Должен вызвать this.notificationManager._failOperationsQueue.registerEvent', function() {
            this.sinon.stub(this.notificationManager._failOperationsQueue, 'registerEvent');

            this.notificationManager._registerOperationFail();

            expect(this.notificationManager._failOperationsQueue.registerEvent).has.callCount(1);
        });
    });

    describe('#_resetNotificationShow', function() {
        it('Должен обнулить счётчик показа нотификаций и таймер нотификаций', function() {
            this.notificationManager._notificationShowCount = 1;
            this.notificationManager._resetNotificationTimer = 'fake timer';

            this.notificationManager._resetNotificationShow();

            expect(this.notificationManager._notificationShowCount).to.equal(0);
            expect(this.notificationManager._resetNotificationTimer).to.equal(null);
        });
    });

    describe('#_setNotificationShow', function() {
        beforeEach(function() {
            this.clock = this.sinon.useFakeTimers();

            this.sinon.stub(this.notificationManager, '_resetNotificationShow');
        });

        it('Должен увеличить счётчик нотификаций на 1', function() {
            this.notificationManager._setNotificationShow();

            expect(this.notificationManager._notificationShowCount).to.equal(1);
        });

        it('Если таймер есть, то не должен обовлять этот таймер', function() {
            this.notificationManager._resetNotificationTimer = 'fake timer';

            this.notificationManager._setNotificationShow();

            expect(this.notificationManager._resetNotificationTimer).to.equal('fake timer');
        });

        it('Если таймера нет, то должен создать таймер', function() {
            this.notificationManager._setNotificationShow();

            expect(this.notificationManager._resetNotificationTimer).to.not.equal(null);
        });

        it('Через минуту должен вызвать #_resetNotificationShow', function() {
            this.notificationManager._setNotificationShow();

            expect(this.notificationManager._resetNotificationShow).has.callCount(0);
            this.clock.next();
            expect(this.notificationManager._resetNotificationShow).has.callCount(1);
        });
    });
});
