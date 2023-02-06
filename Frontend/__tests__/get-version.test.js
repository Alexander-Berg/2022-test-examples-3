const getVersion = require('../get-version');

describe('#getVersion', () => {
    let user;
    let vcs;

    beforeEach(() => {
        user = 'TEST_USER';
        vcs = {
            branch: '',
        };
    });

    it('should get version for development', () => {
        expect(getVersion('development', user, vcs, '')).toBe(`local-${user}`);
    });

    it('should get version for pull request', () => {
        vcs.pull = 'some-pull';

        expect(getVersion('production', user, vcs, '')).toBe(`pull-${vcs.pull}`);
    });

    it('should get version for git release', () => {
        vcs.branch = 'release/messenger/v2.0.0';

        expect(getVersion('production', user, vcs, '')).toBe('2.0.0');
    });

    it('should get version for arc release', () => {
        vcs.branch = 'releases/frontend/messenger/v2.0.0';

        expect(getVersion('production', user, vcs, '')).toBe('2.0.0');
    });
});
