import { applyPatches } from '../applyPatches';

const base = {
    idmSystem: 'modadverttest',
    directDomain: 'test-direct.yandex.ru',
    loupeDomain: 'loupe-testing.yandex-team.ru',
};

describe('config/applyPatches', () => {
    it('applies patch for test', () => {
        expect(applyPatches(base)).toEqual(base);
    });

    it('applies patch for production', () => {
        // todo
        const mockWindow = { location: { href: 'https://modadvert.yandex-team.ru' } };
        jest.fn({ window: { ...window, mockWindow } });

        expect(applyPatches(base)).toEqual(base);
    });

    it('return base if its not test and prod', () => {
        const mockWindow = { location: { href: 'https://modadvert-stage.yandex-team.ru' } };
        jest.fn({ window: { ...window, mockWindow } });
        expect(applyPatches(base)).toEqual(base);
    });
});
