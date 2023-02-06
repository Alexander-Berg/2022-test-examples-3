describe('EventsTimeQueue', function() {
    beforeEach(function () {
        this.capacity = 5;
        this.time = 60000;

        this.eventsTimeQueue = new Daria.MOPS.OperationFailNotificationManager.EventsTimeQueue(
            this.capacity, this.time
        );

        this.sinon.stub(window.Date, 'now');
    });

    describe('#hasMaxEventsForTimeIntereval', function() {
        beforeEach(function() {
            this.now = 1523902939156;

            window.Date.now.returns(this.now)
        });

        it('Если нет событий, то должен вернуть false', function() {
            expect(this.eventsTimeQueue.hasMaxEventsForTimeInterval()).to.eql(false);
        });

        it('Если событий меньше, чем вместимость, то должен вернуть false', function() {
            this.eventsTimeQueue._events = [
                this.now - 3000, this.now, this.now + 60001
            ];

            expect(this.eventsTimeQueue.hasMaxEventsForTimeInterval()).to.eql(false);
        });

        it('Если событий больше, чем вместимость, но не достаточно с интералом меньше установленного времени, ' +
            'то должен вернуть false',
            function() {
                this.eventsTimeQueue._events = [
                    this.now - 61000, this.now - 60001, this.now - 3000, this.now, this.now + 50000
                ];

                expect(this.eventsTimeQueue.hasMaxEventsForTimeInterval()).to.eql(false);
            }
        );

        it('Если событий с нужным интервалом времени достаточно, то должен вернуть true', function() {
            this.eventsTimeQueue._events = [
                this.now - 59999, this.now - 40000, this.now - 3000, this.now - 1000, this.now, this.now + 3000
            ];

            expect(this.eventsTimeQueue.hasMaxEventsForTimeInterval()).to.eql(true);
        });
    });

    describe('#registerEvent', function() {
        it('Должен добавить timestamp в очередь', function() {
            window.Date.now.returns('test timestamp');

            this.eventsTimeQueue.registerEvent();

            expect(this.eventsTimeQueue._events).to.eql([ 'test timestamp' ]);
        });

        it('Должен оставлять только последние capacity*2 timestamp`ов', function() {
            var timestamps = [
                'timestamp1', 'timestamp2', 'timestamp3', 'timestamp4', 'timestamp5',
                'timestamp6', 'timestamp7', 'timestamp8', 'timestamp9', 'timestamp10'
            ];

            timestamps.forEach(function(t) {
                window.Date.now.returns(t);
                this.eventsTimeQueue.registerEvent();
            }, this);

            expect(this.eventsTimeQueue._events).to.eql(timestamps);
        });

        it('Если timestamp`ов больше, чем capacity*2, то должен урезать массив timestamp`ов до 5', function() {
            var timestamps = [
                'timestamp1', 'timestamp2', 'timestamp3', 'timestamp4', 'timestamp5',
                'timestamp6', 'timestamp7', 'timestamp8', 'timestamp9', 'timestamp10', 'timestamp11'
            ];

            timestamps.forEach(function(t) {
                window.Date.now.returns(t);
                this.eventsTimeQueue.registerEvent();
            }, this);

            expect(this.eventsTimeQueue._events).to.eql(timestamps.slice(6));
        });
    });
});
