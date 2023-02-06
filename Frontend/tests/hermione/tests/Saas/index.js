const beforeEachCallback = require('../../utils/beforeEachCallback');

describe('Saas', async function() {
    beforeEach(beforeEachCallback);

    it('Header', require('./checkHeader'));
});
