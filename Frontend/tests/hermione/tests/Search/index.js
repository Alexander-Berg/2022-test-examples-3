const beforeEachCallback = require('../../utils/beforeEachCallback');

describe('Search', async function() {
    beforeEach(beforeEachCallback);

    it('Header', require('./checkHeader'));
    it('Search', require('./checkSearch'));
    it('ServiceChange', require('./checkServiceChange'));
});
