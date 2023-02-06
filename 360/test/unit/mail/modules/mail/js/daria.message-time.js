describe('Daria.MessageTime', function() {
    describe('#getFormattedTime', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'now');
            this.sinon.stub(Daria.MessageTime, '_getUserTZ').returns(0);
        });

        it('Должен отдавать даты в формате "сегодня", если письмо пришло сегодня', function() {
            // Сейчас 2015-11-26 15:00
            var userNow = 1448550000000;
            // Письмо пришло 2015-11-26 10:00
            var receivedDateMessage = 1448532000000;

            Daria.now.returns(userNow);

            expect(Daria.MessageTime.getFormattedTime(receivedDateMessage)).to.be.eql({
                attach: '10:00',
                list: '10:00',
                message: 'сегодня в 10:00',
                title: undefined,
                full: '26 ноября в 10:00',
                isRelative: true
            });
        });

        it('Должен отдавать даты в формате "год", если письмо пришло в другом году', function() {
            // Сейчас 2015-11-26 15:00
            var userNow = 1448550000000;
            // Письмо пришло 2014-11-26 10:00
            var receivedDateMessage = 1416996000000;

            Daria.now.returns(userNow);

            expect(Daria.MessageTime.getFormattedTime(receivedDateMessage)).to.be.eql({
                attach: '26 ноя',
                list: '26.11.14',
                message: '26.11.14 в 10:00',
                title: '26 ноября 2014 г. в 10:00',
                full: '26 ноября 2014 г. в 10:00',
                isRelative: false
            });
        });

        it('Должен отдавать даты в формате "месяц", если письмо пришло не сегодня и в этом году', function() {
            // Сейчас 2015-11-26 15:00
            var userNow = 1448550000000;
            // Письмо пришло 2015-11-22 10:00
            var receivedDateMessage = 1448186400000;

            Daria.now.returns(userNow);

            expect(Daria.MessageTime.getFormattedTime(receivedDateMessage)).to.be.eql({
                attach: '22 ноя',
                list: '22 ноя',
                message: '22 ноября в 10:00',
                title: '22 ноября в 10:00',
                full: '22 ноября в 10:00',
                isRelative: false
            });
        });
    });
});
