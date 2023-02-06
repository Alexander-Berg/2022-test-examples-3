const { version } = require('./const');

describe('version', () => {
    it('Should be same as package.version', () => {
        expect(version).toStrictEqual(require('../../../package.json').version);
    });
});
