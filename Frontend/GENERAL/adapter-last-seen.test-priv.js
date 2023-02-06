describeBlock('adapter-last-seen__time', function(block) {
    let userTime;

    beforeEach(function() {
        userTime = '2018-06-25T23:13:26+0300';
    });

    describe('should return correct time format', function() {
        it('Были сегодня', function() {
            const timestamp = Number(new Date('Mon Jun 25 2018 13:13:26 GMT+0300 (MSK)'));
            const lastSeenText = block(timestamp, userTime);
            assert.equal(lastSeenText, 'Были сегодня');
        });

        it('Были вчера: начало суток', function() {
            const timestamp = Number(new Date('Mon Jun 24 2018 00:13:26 GMT+0300 (MSK)'));
            const lastSeenText = block(timestamp, userTime);
            assert.equal(lastSeenText, 'Были вчера');
        });

        it('Были вчера: конец суток', function() {
            const timestamp = Number(new Date('Mon Jun 24 2018 23:13:26 GMT+0300 (MSK)'));
            const lastSeenText = block(timestamp, userTime);
            assert.equal(lastSeenText, 'Были вчера');
        });

        it('Были 5 янв', function() {
            const timestamp = Number(new Date('Mon Jan 05 2018 23:13:26 GMT+0300 (MSK)'));
            const lastSeenText = block(timestamp, userTime);
            assert.equal(lastSeenText, 'Были 5 янв');
        });

        it('Были в 2017', function() {
            const timestamp = Number(new Date('Mon Jun 25 2017 23:13:26 GMT+0300 (MSK)'));
            const lastSeenText = block(timestamp, userTime);
            assert.equal(lastSeenText, 'Были в 2017');
        });
    });
});
