import { IAdapterContext } from '@yandex-turbo/types/AdapterContext';
import { getImageSrcHeight, suggestLcAliasByHeight } from '../suggest-lc-alias';
import { getRetinaScale } from '../retinaScale';

// @ts-ignore
import { isAvatars } from '../../../../../core/utils/avatars/index';

function getDefaultContext() {
    return {
        assets: {
            getStore: () => ({
                getState: () => ({
                    edc: {
                        isEdcMode: false,
                    }
                })
            })
        }
    } as IAdapterContext;
}

describe('Check suggest-lc-alias', () => {
    const screenHeight = 550;

    describe('getImageSrcHeight', () => {
        it('should work with base src', () => {
            const baseUrl = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b';
            const context = getDefaultContext();
            const url = getImageSrcHeight(baseUrl, screenHeight, context);
            const expectedUrl = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b/lc_desktop_992px_r16x9_pd20';

            expect(url).toBe(expectedUrl);
        });

        it('should work with base src with slash in end', () => {
            const baseUrl = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b/';
            const context = getDefaultContext();
            const url = getImageSrcHeight(baseUrl, screenHeight, context);
            const expectedUrl = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b/lc_desktop_992px_r16x9_pd20';

            expect(url).toBe(expectedUrl);
        });

        it('should work with base src with orig', () => {
            const baseUrl = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b/orig';
            const context = getDefaultContext();
            const url = getImageSrcHeight(baseUrl, screenHeight, context);
            const expectedUrl = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b/lc_desktop_992px_r16x9_pd20';

            expect(url).toBe(expectedUrl);
        });

        it('should return url if url is not avatars', () => {
            const inputUrl = 'https://yandex.ru/screenshot/orig';
            const context = getDefaultContext();
            const url = getImageSrcHeight(inputUrl, screenHeight, context);

            expect(url).toBe(inputUrl);
        });

        it('should work with flag isDisabledCompressImage', () => {
            const baseUrl = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b';
            const context = getDefaultContext();
            const isDisabledCompressImage = true;
            const url = getImageSrcHeight(baseUrl, screenHeight, context, isDisabledCompressImage);
            const expectedUrl = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b/orig';

            expect(url).toBe(expectedUrl);
        });
    });

    describe('suggestLcAliasByHeight', () => {
        it('should return alias for height = 100 where retinaScale is 1', () => {
            const height = 100;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('max_g360_c6_r1x1_pd10');
        });

        it('should return alias for height = 100 where retinaScale is 2', () => {
            const height = 100;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('max_g360_c6_r1x1_pd20');
        });

        it('should return alias for height = 150 where retinaScale is 1', () => {
            const height = 150;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('max_g360_c12_r16x9_pd10');
        });

        it('should return alias for height = 150 where retinaScale is 2', () => {
            const height = 150;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('max_g360_c12_r16x9_pd20');
        });

        it('should return alias for height = 200 where retinaScale is 1', () => {
            const height = 200;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('max_g480_c12_r16x9_pd10');
        });

        it('should return alias for height = 200 where retinaScale is 2', () => {
            const height = 200;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('max_g480_c12_r16x9_pd20');
        });

        it('should return alias for height = 300 where retinaScale is 1', () => {
            const height = 300;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_mobile_580px_r16x9_pd10');
        });

        it('should return alias for height = 300 where retinaScale is 2', () => {
            const height = 300;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_mobile_580px_r16x9_pd20');
        });

        it('should return alias for height = 350 where retinaScale is 1', () => {
            const height = 350;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_mobile_680px_r16x9_pd10');
        });

        it('should return alias for height = 350 where retinaScale is 2', () => {
            const height = 350;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_mobile_680px_r16x9_pd20');
        });

        it('should return alias for height = 400 where retinaScale is 1', () => {
            const height = 400;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_mobile_750px_r16x9_pd10');
        });

        it('should return alias for height = 400 where retinaScale is 2', () => {
            const height = 400;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_mobile_750px_r16x9_pd20');
        });

        it('should return alias for height = 450 where retinaScale is 1', () => {
            const height = 450;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_mobile_850px_r16x9_pd10');
        });

        it('should return alias for height = 450 where retinaScale is 2', () => {
            const height = 450;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_mobile_850px_r16x9_pd20');
        });

        it('should return alias for height = 500 where retinaScale is 1', () => {
            const height = 500;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_desktop_940px_r16x9_pd10');
        });

        it('should return alias for height = 500 where retinaScale is 2', () => {
            const height = 500;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_desktop_940px_r16x9_pd20');
        });

        it('should return alias for height = 550 where retinaScale is 1', () => {
            const height = 550;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_desktop_992px_r16x9_pd10');
        });

        it('should return alias for height = 550 where retinaScale is 2', () => {
            const height = 550;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_desktop_992px_r16x9_pd20');
        });

        it('should return alias for height = 600 where retinaScale is 1', () => {
            const height = 600;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_desktop_1200px_r16x9_pd10');
        });

        it('should return alias for height = 600 where retinaScale is 2', () => {
            const height = 600;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_desktop_1200px_r16x9_pd20');
        });

        it('should return alias for height = 800 where retinaScale is 1', () => {
            const height = 800;
            const retinaScale = 0;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_desktop_1920px_r16x9_pd10');
        });

        it('should return alias for height = 800 where retinaScale is 2', () => {
            const height = 800;
            const retinaScale = 1;
            const alias = suggestLcAliasByHeight(height, retinaScale);

            expect(alias).toBe('lc_desktop_1920px_r16x9_pd20');
        });
    });

    describe('isAvatars', () => {
        it('should return true when avatar url', () => {
            const url = 'https://avatars.mds.yandex.net/get-turbo/1505928/2a0000016836daee3ee7d60c44cca211b89b';

            expect(isAvatars(url)).toBe(true);
        });

        it('should return false when url is not avatar', () => {
            const url = 'https://yandex.ru/screenshot/orig';

            expect(isAvatars(url)).toBe(false);
        });
    });

    describe('getRetinaScale', () => {
        it('should return 0', () => {
            const context = { } as IAdapterContext;
            const retinaScale = getRetinaScale(context, true);

            expect(retinaScale).toBe(1);
        });

        it('should return 1', () => {
            const context = {
                reqdata: {
                    ycookie: {
                        yp: {
                            szm: '2:1440x900:1440x780',
                        },
                    },
                },
            } as unknown as IAdapterContext;

            const retinaScale = getRetinaScale(context, true);

            expect(retinaScale).toBe(1);
        });
    });
});
