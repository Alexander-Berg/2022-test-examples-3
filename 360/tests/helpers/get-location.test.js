const getLocation = require('../../helpers/get-location.js');

const mockHostname = jest.fn().mockReturnValue('man');
jest.mock('os', () => ({
    hostname: () => mockHostname()
}));

describe('get location', () => {
    it('returns deploy dc if ok', async() => {
        process.env.DEPLOY_NODE_DC = 'vla';
        expect(getLocation()).toBe('vla');
        delete process.env.DEPLOY_NODE_DC;
    });

    it('returns first location if deploy dc not ok', async() => {
        process.env.DEPLOY_NODE_DC = 'boo';
        expect(getLocation()).toBe('sas');
        delete process.env.DEPLOY_NODE_DC;
    });

    it('returns hostname location if ok', async() => {
        mockHostname.mockReturnValue('foo.myt.bar');
        expect(getLocation()).toBe('myt');
    });

    it('returns forst location if hostname not ok', async() => {
        mockHostname.mockReturnValue('foo.bar');
        expect(getLocation()).toBe('sas');
    });
});
