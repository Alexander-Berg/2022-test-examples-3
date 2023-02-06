import * as clearConfig from '../clearConfig';

describe('frontendConfig', () => {
    describe('clearConfig', () => {
        it('clear', () => {
            expect(clearConfig(require('./configFromBunker.json'))).toEqual(require('./clearedConfig.json'));
        });
    });
});
