import { getFlags } from '../lib';

describe('#parseFrontendConfig', () => {
    describe('#flag', () => {
        it('default flag', () => {
            const config = [
                { name: 'flag', value: '2' },
            ];

            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '2' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '2' });
            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '2' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '2' });
        });

        it('internal flag', () => {
            const config = [
                { name: 'flag', value: '2' },
                { name: 'flag', value: '3', only_yandex_net: true },
            ];

            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '2' });
            expect(getFlags(config, { page: 'bar', serviceId: 10, isYandexNet: true })).toEqual({ flag: '3' });
        });

        it('multi flags', () => {
            const config = [
                { name: 'flag', value: '1' },
                { name: 'flag2', value: '2' },
                { name: 'flag3', value: '3' },
            ];

            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '1', flag2: '2', flag3: '3' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '1', flag2: '2', flag3: '3' });
            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '1', flag2: '2', flag3: '3' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '1', flag2: '2', flag3: '3' });
        });

        it('YENV flags', () => {
            const config = [
                { name: 'flag', value: '2' },
                { name: 'flag', value: '3', YENV: 'testing' },
            ];

            expect(getFlags(config, { page: 'bar', serviceId: 10, yenv: 'production' })).toEqual({ flag: '2' });
            expect(getFlags(config, { page: 'bar', serviceId: 11, yenv: 'development' })).toEqual({ flag: '2' });
            expect(getFlags(config, { page: 'foo', serviceId: 12, yenv: 'testing' })).toEqual({ flag: '3' });
        });

        it('without flag', () => {
            const config = [];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({});
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({});
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({});
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({});
        });

        it('flag for enabled build', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', enabled_builds: ['foo'] },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'foo', serviceId: 12 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 12 })).toEqual({ flag: '' });
        });

        it('flag for disabled build', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', disabled_builds: ['foo'] },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 12 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 12 })).toEqual({ flag: '333' });
        });

        it('flag for enabled service', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', enabled_services: [10] },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 12 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 12 })).toEqual({ flag: '' });
        });

        it('flag for disabled service', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', disabled_services: [10] },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'foo', serviceId: 12 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 12 })).toEqual({ flag: '333' });
        });

        it('flag for enabled build and enabled service', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', enabled_builds: ['foo'], enabled_services: [10] },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 12 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 12 })).toEqual({ flag: '' });
        });

        it('flag for enabled build and enabled service with YENV', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', enabled_builds: ['foo'], enabled_services: [10], YENV: 'testing' },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10, yenv: 'production' })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 10, yenv: 'testing' })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'foo', serviceId: 11, yenv: 'production' })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 11, yenv: 'testing' })).toEqual({ flag: '' });
        });

        it('flag for disabled build and enabled service', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', disabled_builds: ['foo'], enabled_services: [10] },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 12 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 12 })).toEqual({ flag: '' });
        });

        it('flag for enabled build and disabled service', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', enabled_builds: ['foo'], disabled_services: [10] },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'foo', serviceId: 12 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 12 })).toEqual({ flag: '' });
        });

        it('flag for disabled build and disabled service', () => {
            const config = [
                { name: 'flag', value: '' },
                { name: 'flag', value: '333', disabled_builds: ['foo'], disabled_services: [10] },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 11 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'foo', serviceId: 12 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 10 })).toEqual({ flag: '' });
            expect(getFlags(config, { page: 'bar', serviceId: 11 })).toEqual({ flag: '333' });
            expect(getFlags(config, { page: 'bar', serviceId: 12 })).toEqual({ flag: '333' });
        });

        it('flag for version', () => {
            const config = [
                { name: 'flag', value: '1' },
                {
                    name: 'flag',
                    value: '2',
                    version: '>=1.2.x',
                },
            ];

            expect(getFlags(config, { page: 'foo', serviceId: 0, version: '1.1.0', yenv: 'production' })).toEqual({ flag: '1' });
            expect(getFlags(config, { page: 'foo', serviceId: 0, version: '1.2.0', yenv: 'production' })).toEqual({ flag: '2' });
            expect(getFlags(config, { page: 'foo', serviceId: 0, version: '1.2.3', yenv: 'production' })).toEqual({ flag: '2' });
            expect(getFlags(config, { page: 'foo', serviceId: 0, version: '1.2.3', yenv: 'production' })).toEqual({ flag: '2' });
        });
    });
});
