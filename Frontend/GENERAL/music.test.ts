import { addHttpsIfNotExists, isPattern } from './music';

describe('addHttpsIfNotExists', () => {
    const correctUrl = 'https://example.com';

    it('Should replace http with https', () => {
        expect(addHttpsIfNotExists('http://example.com')).toBe(correctUrl);
    });

    it('Should add protocol if not exists', () => {
        expect(addHttpsIfNotExists('example.com')).toBe(correctUrl);
    });

    it('Should replace // with https', () => {
        expect(addHttpsIfNotExists('//example.com')).toBe(correctUrl);
    });

    it('Should do nothing when https exists', () => {
        expect(addHttpsIfNotExists('example.com')).toBe(correctUrl);
    });
});

describe('isPattern', () => {
    it('Should return true for patterns', () => {
        expect(
            isPattern(
                'avatars.yandex.net/get-music-misc/29541/mix.5ef32553116e5f39459f9fa4.background-image.1592993172438/%%',
            ),
        ).toBe(true);
        expect(
            isPattern(
                'avatars.yandex.net/get-music-user-playlist/70586/460140864.1000.38823ru/%%?1564407139519',
            ),
        ).toBe(true);
        expect(
            isPattern('avatars.yandex.net/get-music-content/2358262/96b65bec.a.11919802-1/%%'),
        ).toBe(true);
    });

    it('Should return false for non-patterns', () => {
        expect(
            isPattern(
                'https://avatars.yandex.net/get-music-content/2358262/96b65bec.a.11919802-1/200x200',
            ),
        ).toBe(false);
        expect(
            isPattern(
                'https://avatars.mds.yandex.net/get-dialogs/1676983/e8fc2fac983d783839cc/orig',
            ),
        ).toBe(false);
    });
});
