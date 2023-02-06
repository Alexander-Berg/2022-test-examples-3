import { assert } from 'chai';
import { dateFormatter } from '../.';

describe('date formatter', () => {
    it('should format simple date', () => {
        assert.equal(dateFormatter('2020-06-20'), '20 июня 2020');
        assert.equal(dateFormatter('2010-12-31'), '31 декабря 2010');
        assert.equal(dateFormatter('2020-01-01'), '1 января 2020');
    });

    it('should format date in short format', () => {
        assert.equal(dateFormatter('2020-06-20', true), '20 июн 2020');
        assert.equal(dateFormatter('2010-12-31', true), '31 дек 2010');
        assert.equal(dateFormatter('2020-01-01', true), '1 янв 2020');
    });
});
