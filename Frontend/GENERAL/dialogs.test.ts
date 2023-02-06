import { isDialogsUrl } from './dialogs';

describe('isDialogsUrl', () => {
    it('should return true for correct', () => {
        expect(
            isDialogsUrl(
                'https://avatars.mds.yandex.net/get-dialogs/758954/a22fd3e7f536e9816a56/orig',
            ),
        ).toBe(true);
    });

    it('should return false for incorrect', () => {
        expect(
            isDialogsUrl(
                'https://avatars.yandex.net/get-music-content/2358262/96b65bec.a.11919802-1/orig',
            ),
        ).toBe(false);
    });
});
