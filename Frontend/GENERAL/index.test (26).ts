import { makeBannerSrcSet, makeMusicSrcSetForCard, makeStationMiniSrcSet, makeStationSrcSet } from '.';

describe('makeBannerSrcSet', () => {
    it('Should make correct srcset', () => {
        expect(
            makeBannerSrcSet(
                'https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/orig',
            ),
        ).toBe(
            'https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-banner-image-x1, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-banner-image-x2 2x, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-banner-image-x3 3x, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-banner-image-x4 4x',
        );
    });
});

describe('makeStationMiniSrcSet', () => {
    it('Should make correct srcset', () => {
        expect(
            makeStationMiniSrcSet(
                'https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/orig',
            ),
        ).toBe(
            'https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-station-mini-banner-image-x1, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-station-mini-banner-image-x2 2x, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-station-mini-banner-image-x3 3x, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-station-mini-banner-image-x4 4x',
        );
    });
});

describe('makeStationSrcSet', () => {
    it('Should make correct srcset', () => {
        expect(
            makeStationSrcSet(
                'https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/orig',
            ),
        ).toBe(
            'https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-station-banner-image-x1, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-station-banner-image-x2 2x, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-station-banner-image-x3 3x, https://avatars.mds.yandex.net/get-dialogs/1530877/5f05958a4807117c4c09/my-alice-station-banner-image-x4 4x',
        );
    });
});

describe('makeMusicSrcSet', () => {
    it('Should make correct srcset', () => {
        expect(
            makeMusicSrcSetForCard(
                'avatars.yandex.net/get-music-misc/29541/mix.5ef32553116e5f39459f9fa4.background-image.1592993172438/%%',
            ),
        ).toBe(
            'https://avatars.yandex.net/get-music-misc/29541/mix.5ef32553116e5f39459f9fa4.background-image.1592993172438/100x100, https://avatars.yandex.net/get-music-misc/29541/mix.5ef32553116e5f39459f9fa4.background-image.1592993172438/200x200 2x, https://avatars.yandex.net/get-music-misc/29541/mix.5ef32553116e5f39459f9fa4.background-image.1592993172438/300x300 3x, https://avatars.yandex.net/get-music-misc/29541/mix.5ef32553116e5f39459f9fa4.background-image.1592993172438/400x400 4x',
        );
    });
});
