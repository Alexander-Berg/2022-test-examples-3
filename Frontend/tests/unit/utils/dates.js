const { isValidTimestamp, unixTimestampToDate } = require('../../../src/server/utils/dates');

describe('utils/dates', () => {
    it('isValidTimestamp дожен проверять, что timestamp корректный', () => {
        assert.isFalse(isValidTimestamp('timestamp'));
        assert.isTrue(isValidTimestamp('1486501200'));
        assert.isTrue(isValidTimestamp(1486501200));
        assert.isFalse(isValidTimestamp(undefined));
        assert.isFalse(isValidTimestamp(''));
        assert.isFalse(isValidTimestamp());
    });

    it('unixTimestampToDate дожен преобразовывать unix timestamp в {Date}', () => {
        assert.equal(unixTimestampToDate(1455138000).getTime(), new Date('2016-02-10T21:00Z').getTime());
        assert.equal(unixTimestampToDate('1455138000').getTime(), new Date('2016-02-10T21:00Z').getTime());
    });
});
