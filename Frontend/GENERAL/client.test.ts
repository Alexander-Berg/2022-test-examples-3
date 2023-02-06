import { getPlatformParams } from './client';

describe('client config', () => {
    describe('getPlatformParams', () => {
        it('должен подставлять дефолтные значения в необязательным параметрах', () => {
            const platformParams = getPlatformParams('desktop', {
                supportedBrowsers: [
                    'chrome',
                ],
            });

            expect(platformParams).toMatchSnapshot();
        });
    });
});
