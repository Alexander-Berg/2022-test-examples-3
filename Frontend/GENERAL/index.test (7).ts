import { patchPlayerConfig, PatchPlayerConfigParams, patchQuery } from './index';

describe('IMS.patch-player-with-linker', () => {
    describe('patchPlayerConfig', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        let params: PatchPlayerConfigParams<any>;

        beforeEach(() => {
            params = { config: {} };
        });

        it('returns arg.config', () => {
            expect(patchPlayerConfig(params)).toBe(params.config);
        });

        it('make config.flags to be an object', () => {
            expect(patchPlayerConfig(params).flags).toEqual({});
        });

        it('sets config.flags into parsed arg.linker.flags', () => {
            params = { config: {}, linker: { flags: '{"test": "my value" }' } };

            expect(patchPlayerConfig(params).flags).toEqual({ test: 'my value' });
        });

        it('adds flags into config.flags if present', () => {
            params = { config: { flags: { existed: 'flag' } }, linker: { flags: '{"test": "my value" }' } };

            expect(patchPlayerConfig(params).flags).toEqual({ existed: 'flag', test: 'my value' });
        });

        it('does not change config.flags if arg.linker.flags is invalid json', () => {
            params = { config: { flags: { existed: 'flag' } }, linker: { flags: '{"test\': "my value" }' } };

            expect(patchPlayerConfig(params).flags).toEqual({ existed: 'flag' });
        });

        it('make config.flags to be an object even if linker.flags is invalid', () => {
            params = { config: {}, linker: { flags: '{"test\': "my value" }' } };

            expect(patchPlayerConfig(params).flags).toEqual({});
        });

        it('overrides values from config.flags', () => {
            params = { config: { flags: { existed: 'flag' } }, linker: { flags: '{"existed": "new" }' } };

            expect(patchPlayerConfig(params).flags).toEqual({ existed: 'new' });
        });

        it('does nothing with config.source if additionalParams is missed', () => {
            params = { config: { source: {} }, linker: { slots: '222,0,1' }, slots: '333,0,1' };

            expect(patchPlayerConfig(params).source).toEqual({});
        });

        it('sets config.source.additionalParams.slots into string', () => {
            params = { config: { source: { additionalParams: {} } } };

            expect(patchPlayerConfig(params).source).toEqual({ additionalParams: { slots: '' } });
        });

        it('concats all slots values', () => {
            params = {
                config: { source: { additionalParams: { slots: '111,0,1' } } },
                linker: { slots: '222,0,1' },
                slots: '333,0,1',
            };

            expect(patchPlayerConfig(params).source).toEqual({ additionalParams: { slots: '111,0,1;222,0,1;333,0,1' } });
        });

        it('skips missed slots values while concat', () => {
            expect(patchPlayerConfig({
                config: { element: '', source: { streams: [], additionalParams: {} } },
                linker: { slots: '222,0,1' },
                slots: '333,0,1',
            }).source.additionalParams).toEqual({ slots: '222,0,1;333,0,1' });

            expect(patchPlayerConfig({
                config: { element: '', source: { streams: [], additionalParams: { slots: '111,0,1' } } },
                linker: {},
                slots: '333,0,1',
            }).source.additionalParams).toEqual({ slots: '111,0,1;333,0,1' });

            expect(patchPlayerConfig({
                config: { element: '', source: { streams: [], additionalParams: { slots: '111,0,1' } } },
                linker: { slots: '222,0,1' },
            }).source.additionalParams).toEqual({ slots: '111,0,1;222,0,1' });
        });
    });

    describe('patchQuery', () => {
        it('returns arg.query', () => {
            const params = { query: {} };

            expect(patchQuery(params)).toBe(params.query);
        });

        it('sets query.mb_flags into parsed linker.flags', () => {
            const params = { query: {}, linker: { flags: '{"some":"123;","value":10}' } };

            expect(patchQuery(params).mb_flags).toBe('some=%22123%3B%22;value=10');
        });

        it('sets query.mb_flags into empty string if linker.flags is empty object', () => {
            const params = { query: {}, linker: { flags: '{}' } };

            expect(patchQuery(params).mb_flags).toBe('');
        });

        it('sets query.mb_flags into empty string if linker.flags is missed', () => {
            const params = { query: {}, linker: {} };

            expect(patchQuery(params).mb_flags).toBe('');
        });

        it('sets query.mb_flags into empt string if linker is missed', () => {
            const params = { query: {} };

            expect(patchQuery(params).mb_flags).toBe('');
        });

        it('sets query.slots into string', () => {
            const params = { query: {} };

            expect(patchQuery(params).slots).toBe('');
        });

        it('concats all slots values', () => {
            const params = {
                query: { slots: '111,0,1' },
                linker: { slots: '222,0,1' },
                slots: '333,0,1',
            };

            expect(patchQuery(params).slots).toBe('111,0,1;222,0,1;333,0,1');
        });

        it('skips missed slots values while concat', () => {
            expect(patchQuery({
                query: {},
                linker: { slots: '222,0,1' },
                slots: '333,0,1',
            }).slots).toBe('222,0,1;333,0,1');

            expect(patchQuery({
                query: { slots: '111,0,1' },
                linker: {},
                slots: '333,0,1',
            }).slots).toBe('111,0,1;333,0,1');

            expect(patchQuery({
                query: { slots: '111,0,1' },
                linker: { slots: '222,0,1' },
            }).slots).toBe('111,0,1;222,0,1');
        });
    });
});
