import { GogolRow } from '../riverbank-web-server/adapters/mapGogol';
import { strmRow } from '../riverbank-web-server/adapters/mapStrm';
import { ErrorCodes } from '../riverbank-web-server/diagnostics/codesToDiagnosis';
import { GogolDiagnostics, StrmDiagnostics } from '../riverbank-web-server/diagnostics/diagnosticCenter';

describe('Diagnostics platlists', () => {
    test('451', () => {
        expect(StrmDiagnostics.getCodesToDiagnosis([
            {
                strm_status: '451',
                request_path: '.m3u8',
            },
        ] as strmRow[]).map(val => val.code)).toContain(ErrorCodes.PLAYLISTS_01);
    });
    test('403', () => {
        expect(StrmDiagnostics.getCodesToDiagnosis([
            {
                strm_status: '403',
                request_path: '.m3u8',
            },
        ] as strmRow[]).map(val => val.code)).toContain(ErrorCodes.PLAYLISTS_02);
    });
    test('Random 4xx', () => {
        expect(StrmDiagnostics.getCodesToDiagnosis([
            {
                strm_status: '444',
                request_path: '.m3u8',
            },
        ] as strmRow[]).map(val => val.code)).toContain(ErrorCodes.PLAYLISTS_03);
    });
});

describe('Diagnostics segments', () => {
    test('403', () => {
        expect(StrmDiagnostics.getCodesToDiagnosis([
            {
                strm_status: '403',
                request_path: '.ts',
            },
        ] as strmRow[]).map(val => val.code)).toContain(ErrorCodes.SEGMENTS_01);
    });
    test('500', () => {
        expect(StrmDiagnostics.getCodesToDiagnosis([
            {
                strm_status: '500',
                request_path: '.ts',
            },
        ] as strmRow[]).map(val => val.code)).toContain(ErrorCodes.SEGMENTS_02);
    });
    test('No errors', () => {
        expect(StrmDiagnostics.getCodesToDiagnosis([
            {
                strm_status: '200',
                request_path: '.ts',
            },
            {
                strm_status: '200',
                request_path: '.m3u8',
            },
            {
                strm_status: '200',
                request_path: '.ts',
            },
        ] as strmRow[]).length).toEqual(0);
    });
});

describe('Diagnostics Gogol Events', () => {
    test('manifestLoadTimeout', () => {
        expect(GogolDiagnostics.getCodesToDiagnosis([
            {
                gogol_eventName: 'manifestLoadTimeOut',
            },
        ] as GogolRow[]).map(val => val.code)).toContain(ErrorCodes.GOGOL_EVENT_01);
    });
    test('LevelLoadTimeout', () => {
        expect(GogolDiagnostics.getCodesToDiagnosis([
            {
                gogol_eventName: 'LevelLoadTimeout',
            },
        ] as GogolRow[]).map(val => val.code)).toContain(ErrorCodes.GOGOL_EVENT_02);
    });
    test('LevelLoadError', () => {
        expect(GogolDiagnostics.getCodesToDiagnosis([
            {
                gogol_eventName: 'LevelLoadError',
            },
        ] as GogolRow[]).map(val => val.code)).toContain(ErrorCodes.GOGOL_EVENT_03);
    });
    test('ManifestLoadError', () => {
        expect(GogolDiagnostics.getCodesToDiagnosis([
            {
                gogol_eventName: 'ManifestLoadError',
            },
        ] as GogolRow[]).map(val => val.code)).toContain(ErrorCodes.GOGOL_EVENT_04);
    });
    // test('DrmError', () => {
    //     expect(GogolDiagnostics.getCodesToDiagnosis([
    //         {
    //             gogol_eventName: 'DrmError',
    //         },
    //     ] as GogolRow[]).map(val => val.code)).toContain(ErrorCodes.GOGOL_EVENT_05);
    // });
    // test('Stalled Error', () => {
    //     expect(GogolDiagnostics.getCodesToDiagnosis([
    //         {
    //             gogol_eventName: 'Stalled',
    //             gogol_clientTimestamp: '0',
    //         },
    //         {
    //             gogol_eventName: 'StalledEnd',
    //             gogol_clientTimestamp: '3001',
    //         },
    //     ] as GogolRow[]).map(val => val.code)).toContain(ErrorCodes.GOGOL_EVENT_06);
    //     expect(GogolDiagnostics.getCodesToDiagnosis([
    //         {
    //             gogol_eventName: 'Stalled',
    //             gogol_clientTimestamp: '0',
    //         },
    //         {
    //             gogol_eventName: 'StalledEnd',
    //             gogol_clientTimestamp: '2900',
    //         },
    //     ] as GogolRow[]).map(val => val.code).length).toEqual(0);
    // });
});
