const { parseExperiments } = require('../../helpers/abt.js');
const { encode } = require('../../helpers/base64.js');

const key = new Buffer('dGVzdA==', 'base64');

const nonDiskExperiment = encode(
    JSON.stringify([
        {
            HANDLER: 'REPORT',
            CONTEXT: {}
        }
    ])
);

const diskExperiment = encode(
    JSON.stringify([
        {
            HANDLER: 'DISK',
            CONTEXT: {
                DISK: {
                    flags: ['dv_history_exp']
                }
            }
        }
    ])
);

const diskExperimentWithData = encode(
    JSON.stringify([
        {
            HANDLER: 'DISK',
            CONTEXT: {
                DISK: {
                    flags: ['download_folder_exp'],
                    data: [
                        {
                            size: '123'
                        }
                    ]
                }
            }
        }
    ])
);

const diskExperimentWithFlagsAndData = encode(
    JSON.stringify([
        {
            HANDLER: 'DISK',
            CONTEXT: {
                DISK: {
                    flags: ['download_folder_exp', 'dv_history_exp'],
                    data: [
                        {
                            size: '123'
                        }
                    ]
                }
            }
        }
    ])
);

describe('uaas helper', () => {
    it('не должен найти дисковые флаги', () => {
        const expboxes = '76082,0,0';
        const expflags = nonDiskExperiment;

        expect(parseExperiments(expboxes, expflags, key)).toEqual({
            boxes: [
                '76082,0,0'
            ],
            metrika: 'hyyCOeOobDtnSqNG6cTd_A',
            ids: ['76082'],
            experiments: [
                [
                    {
                        HANDLER: 'REPORT',
                        CONTEXT: {}
                    }
                ]
            ],
            diskFlags: {}
        });
    });
    it('должен найти дисковые флаги', () => {
        const expboxes = '76082,0,0;76844,0,1';
        const expflags = [nonDiskExperiment, diskExperiment].join(',');

        expect(parseExperiments(expboxes, expflags, key)).toEqual({
            boxes: [
                '76082,0,0',
                '76844,0,1'
            ],
            metrika: 'hyyCOeOobDt4_LFxw-WIn7MelH77t_mr',
            ids: ['76082', '76844'],
            experiments: [
                [
                    {
                        HANDLER: 'REPORT',
                        CONTEXT: {}
                    }
                ],
                [{ HANDLER: 'DISK', CONTEXT: { DISK: { flags: ['dv_history_exp'] } } }]
            ],
            diskFlags: { dv_history_exp: true }
        });
    });
    it('должен найти данные эксперимента', () => {
        const expboxes = '76082,0,0';
        const expflags = diskExperimentWithData;

        expect(parseExperiments(expboxes, expflags, key)).toEqual({
            boxes: [
                '76082,0,0'
            ],
            metrika: 'hyyCOeOobDtnSqNG6cTd_A',
            ids: ['76082'],
            experiments: [
                [{ HANDLER: 'DISK', CONTEXT: { DISK: { data: [{ size: '123' }], flags: ['download_folder_exp'] } } }]
            ],
            diskFlags: { download_folder_exp: { size: '123' } }
        });
    });
    it('должен правильно обработать 2 эксперимента, один из которых с данными', () => {
        const expboxes = '76082,0,0;76844,0,1';
        const expflags = [diskExperiment, diskExperimentWithData].join(',');

        expect(parseExperiments(expboxes, expflags, key)).toEqual({
            boxes: [
                '76082,0,0',
                '76844,0,1'
            ],
            metrika: 'hyyCOeOobDt4_LFxw-WIn7MelH77t_mr',
            ids: ['76082', '76844'],
            experiments: [
                [{ HANDLER: 'DISK', CONTEXT: { DISK: { flags: ['dv_history_exp'] } } }],
                [{ HANDLER: 'DISK', CONTEXT: { DISK: { data: [{ size: '123' }], flags: ['download_folder_exp'] } } }]
            ],
            diskFlags: { dv_history_exp: true, download_folder_exp: { size: '123' } }
        });
    });
    it('должен правильно обработать эксперимент с 2 флагами, один из которых с данными', () => {
        const expboxes = '76082,0,0;76844,0,1';
        const expflags = diskExperimentWithFlagsAndData;

        expect(parseExperiments(expboxes, expflags, key)).toEqual({
            boxes: [
                '76082,0,0',
                '76844,0,1'
            ],
            metrika: 'hyyCOeOobDt4_LFxw-WIn7MelH77t_mr',
            ids: ['76082', '76844'],
            experiments: [[{
                HANDLER: 'DISK',
                CONTEXT: {
                    DISK: {
                        data: [{ size: '123' }],
                        flags: ['download_folder_exp', 'dv_history_exp']
                    }
                }
            }]],
            diskFlags: { dv_history_exp: true, download_folder_exp: { size: '123' } }
        });
    });
    it('не должен упасть от неправильного JSON', () => {
        const expboxes = '76082,0,0';
        const expflags = 'rubbish';

        expect(parseExperiments(expboxes, expflags, key)).toEqual({
            boxes: ['76082,0,0'],
            metrika: 'hyyCOeOobDtnSqNG6cTd_A',
            ids: ['76082'],
            experiments: [[]],
            diskFlags: {}
        });
    });
});
