const beforeEachCallback = require('../../utils/beforeEachCallback');

describe('Excel', async function() {
    beforeEach(beforeEachCallback);

    it('Header', require('./checkHeader'));
});
