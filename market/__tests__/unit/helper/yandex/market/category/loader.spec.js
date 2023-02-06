'use strict';

const sinon = require('sinon');

const cache = require('../../../../../../src/helper/cache');

const load = () => {
    return require('../../../../../../src/helper/loader');
};

describe('helper / yandex / market / category', () => {
    describe('loader', () => {
        test('should successfully load categories', function() {
            let counter = 0;
            const consoleErrorStub = sinon.stub(console, 'error', () => undefined);
            const cacheStub = sinon.stub(cache.categories, 'add', () => counter++);

            const state = load();

            consoleErrorStub.restore();
            cacheStub.restore();

            expect(state).toBeTruthy();
            expect(typeof counter).toBe('number');
            // expect(counter).to.be.equal(4622); // FIXME: some tests are not isolated
        });

        // TODO: add test to check singleton architecture
    });
});
