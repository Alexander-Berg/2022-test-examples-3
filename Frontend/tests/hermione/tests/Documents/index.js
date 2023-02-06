const beforeEachCallback = require('../../utils/beforeEachCallback');

describe('Documents', async function() {
    beforeEach(beforeEachCallback);

    it('Header', require('./checkHeader'));
});
