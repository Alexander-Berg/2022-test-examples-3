import { ABTSplitter, ABTInfo, ABTRawConfig } from '.';

function makeHeadersByInfo(info: Partial<ABTInfo>): Record<string, string> {
    if (!info.configs) {
        throw new Error('Param `configs` is required');
    }

    return {
        'x-yandex-expconfigversion': String(info.version || '169'),
        'x-yandex-randomuid': String(info.randomUid || ''),
        'x-yandex-expflags': (info.configs as ABTRawConfig[]).map(raw => Buffer.from(JSON.stringify(raw), 'ascii').toString('base64')).join(','),
        'x-yandex-expboxes': info.boxes || '',
        'x-yandex-expboxes-crypted': String(info.experiments || ''),
    };
}

describe('Splitter', () => {
    it('should use the right context and correctly parse flags', () => {
        const splitter = new ABTSplitter({ service: 'svc', handler: 'SVC' });
        const configs = [{
            HANDLER: 'SVC',
            CONTEXT: {
                SVC: { flags: ['expflag=goodvalue', 'bool=true', 'valid'], testid: ['1'] },
                NOTSVC: { flags: ['expflag=badvalue', 'invalid'], testid: ['2'] },
            },
            // 'desktop && ((device.BrowserName ne "MSIE" || device.BrowserVersion gt "10"))'
            CONDITION: 'UNUSED',
            RESTRICTIONS: [{ services: 'svc' }],
        }];
        const experiments = 'SOME THRING';

        const info = splitter.parseInfo(makeHeadersByInfo({ configs, experiments }));

        expect(info).toMatchObject({ experiments });
        expect(info.configs).toEqual(configs);
        expect(info.flags).toEqual({ expflag: 'goodvalue', bool: true, valid: true });
    });
});
