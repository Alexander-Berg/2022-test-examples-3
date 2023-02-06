const AliceClient = require('./uniproxy/client');
const locations = require('./uniproxy/locations');
const { getHost } = require('./host');
const asserts = require('./asserts');

global.assert = require('chai').assert;

beforeEach(async function() {
    this.client = new AliceClient(await getHost(), this.currentTest.ctx.surface);
    this.locations = locations;
    this.asserts = asserts.getAsserts(this.client);

    await this.client.init();
});

afterEach(async function() {
    await this.client.close();
});
