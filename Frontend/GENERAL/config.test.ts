import { getConfig, IProjectConfig } from './config';

describe('config', () => {
    describe('getConfig', () => {
        it('должен формировать URl к зафриженной статике без учета номера PR и коммита', () => {
            process.env.npm_package_name = '@yandex-int/test-pkg';

            const config: IProjectConfig = {
                version: 'v1.0.1',
                s3: {
                    usePublicUrl: false,
                    endpoint: '',
                    accessKeyId: 'test123',
                    secretAccessKey: 'test567',
                },
            };

            expect(getConfig('testing', config)).toMatchSnapshot();
            expect(getConfig('production', config)).toMatchSnapshot();
        });
    });
});
