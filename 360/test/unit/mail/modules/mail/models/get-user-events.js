describe('Daria.mUserEvents', function() {
    beforeEach(function() {
        this.sinon.stub(ns.Model.get('account-information'), 'getData').returns({
            timezone: 'Europe/Moscow',
            tz_offset: -60
        });
        this.mGetUserEvents = ns.Model.get('get-user-events');

        this.getISODate = function(date, postfix) {
            if (!postfix) {
                postfix = '';
            }
            // Преобразование в ISO8601
            date = Jane.Date.format('%Date_iso', date) + postfix;
            return date;
        };
        this.event1 = {
            startTs: this.getISODate(new Date(2014, 3, 4, 9, 0, 0, 0)),
            endTs: this.getISODate(new Date(2014, 3, 4, 10, 0, 0, 0))
        };
        this.event2 = {
            startTs: this.getISODate(new Date(2014, 3, 4, 12, 30, 0, 0)),
            endTs: this.getISODate(new Date(2014, 3, 5, 13, 0, 0, 0))
        };
        this.resultData = {
            events: [
                this.event1,
                this.event2
            ]
        };
    });

    afterEach(function() {
        delete this.resultData;
        delete this.mGetUserEvents;
        delete this.event1;
        delete this.event2;
    });

    describe('#onsetcache', function() {
        beforeEach(function() {
            this.sinon.stub(this.mGetUserEvents, 'createGroupsOfEvents');
        });

        it('Должен запустить парсинг событий', function() {
            this.mGetUserEvents.setData(this.resultData);

            expect(this.mGetUserEvents.createGroupsOfEvents).to.have.callCount(1);
        });

        it('Должен сформировать массив контейнеров событий', function() {
            var events = this.resultData.events;
            this.mGetUserEvents.setData(this.resultData);

            expect(this.mGetUserEvents.createGroupsOfEvents.getCall(0).args[0]).to.be.equal(events);
        });

        it('Должен отправить сообщение об ошибке если нет массива событий', function() {
            this.sinon.stub(Jane.ErrorLog, 'send');
            this.mGetUserEvents.setData({});

            expect(Jane.ErrorLog.send).to.have.callCount(1);
        });
    });

    describe('#createGroupsOfEvents', function() {
        it('Должно создать группу для каждого события, если события идут друг за другом и не пересекаются', function() {
            var event1 = {
                startTs: Jane.Date.format('%Date_iso', new Date(2016, 6, 25, 0, 0, 0, 0)),
                endTs: Jane.Date.format('%Date_iso', new Date(2016, 6, 25, 0, 30, 0, 0))
            };

            var event2 = {
                startTs: Jane.Date.format('%Date_iso', new Date(2016, 6, 25, 0, 30, 0, 0)),
                endTs: Jane.Date.format('%Date_iso', new Date(2016, 6, 25, 1, 0, 0, 0))
            };

            var groups = this.mGetUserEvents.createGroupsOfEvents([ event1, event2 ]);

            expect(groups.length).to.be.equal(2);
            expect(groups[0].startDate.toISOString()).to.be.eql(event1.startTs);
            expect(groups[0].endDate.toISOString()).to.be.eql(event1.endTs);
            expect(groups[1].startDate.toISOString()).to.be.eql(event2.startTs);
            expect(groups[1].endDate.toISOString()).to.be.eql(event2.endTs);
        });

        it('Группа короче _minimalViewInterval минут отображается как _minimalViewInterval минут', function() {
            var event = {
                startTs: Jane.Date.format('%Date_iso', new Date(2016, 6, 25, 0, 0, 0, 0)),
                endTs: Jane.Date.format('%Date_iso', new Date(2016, 6, 25, 0, 2, 0, 0))
            };

            var groups = this.mGetUserEvents.createGroupsOfEvents([ event ]);
            expect(groups[0].viewIntervalInRange).to.be.eql(this.mGetUserEvents._minimalViewInterval);
        });
    });
});
