import shorten, {tokenize, dropWordsInLines, dropWords} from './shorten';

describe('Shorten', () => {
    test('Shorten image cron names', () => {
        const input = [
            'IMAGES_VALIDATE G.IMAGES_VALIDATE default _404-binary-5',
            'IMAGES_VALIDATE Y.IMAGES_VALIDATE без БР default _404-binary-5',
            'IMAGES_KPI G.IMAGES_KPI default _404-binary-5',
            'IMAGES_KPI Y.IMAGES_KPI без БР default _404-binary-5',
        ];
        const output = [
            'VALIDATE G',
            'VALIDATE Y.без БР',
            'KPI G',
            'KPI Y.без БР',
        ];
        expect(shorten(input)).toEqual(output);
    });

    test('Shorten image cron names with subset', () => {
        const input = [
            'Y.Web.World.KPI (201810)',
            'Y.Web.World.KPI Antiall_RKN_off (201810)',
            'Y.Web.World.KPI Antiall_off (201810)',
            'Y.Web.World.KPI Antispam_off (201810)',
        ];
        const output = ['Y', 'Antiall_RKN_off', 'Antiall_off', 'Antispam_off'];
        expect(shorten(input)).toEqual(output);
    });

    test('Shorten single line', () => {
        const input = [
            'IMAGES_VALIDATE G.IMAGES_VALIDATE default _404-binary-5',
        ];
        const output = ['IMAGES_VALIDATE G.default _404-binary-5'];
        expect(shorten(input)).toEqual(output);
    });

    test('Drop words in lines', () => {
        const input = [
            ['IMAGES', 'VALIDATE', 'G', 'default', '404', 'binary', '5'],
            ['IMAGES', 'VALIDATE', 'Y', 'default', '404', 'binary', '5'],
            ['IMAGES', 'KPI', 'G', 'default', '404', 'binary', '5'],
            ['IMAGES', 'KPI', 'Y', 'default', '404', 'binary', '5'],
        ];
        const output = [
            ['VALIDATE', 'G'],
            ['VALIDATE', 'Y'],
            ['KPI', 'G'],
            ['KPI', 'Y'],
        ];
        expect(dropWordsInLines(input)).toEqual(output);
    });

    test('Drop words', () => {
        const input = 'IMAGES_VALIDATE G.IMAGES/VALIDATE default _404-binary-5';
        const output = [
            'IMAGES_',
            'VALIDATE ',
            'G.',
            'default ',
            '_404-',
            'binary-',
            '5',
        ];

        expect(dropWords(input)).toEqual(output);
    });

    test('Tokenize', () => {
        const input =
            'IMAGES_VALIDATE   G.IMAGES_VALIDATE default _404-binary-5';
        const output = [
            'IMAGES_',
            'VALIDATE ',
            'G.',
            'IMAGES_',
            'VALIDATE ',
            'default ',
            '_404-',
            'binary-',
            '5',
        ];

        expect(tokenize(input)).toEqual(output);
    });

    test('Tokenize cyrillic', () => {
        const input = 'IMAGES_VALIDATE имаджес без.бр';
        const output = ['IMAGES_', 'VALIDATE ', 'имаджес ', 'без.', 'бр'];

        expect(tokenize(input)).toEqual(output);
    });
});
