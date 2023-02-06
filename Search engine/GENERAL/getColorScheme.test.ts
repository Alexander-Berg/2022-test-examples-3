import {Dictionary} from 'ts-essentials';

import getColorScheme, {Line} from './getColorScheme';

const LINES = [
    {key: '100588,9236,default,images-corsa-unity-10', colorGroup: 'BLUE'},
    {key: '100588,9236,fresh,images-corsa-unity-10', colorGroup: 'BLUE'},
    {key: '100588,9238,default,images-corsa-unity-10', colorGroup: 'RED'},
    {key: '100588,9240,default,images-corsa-unity-10', colorGroup: 'RED'},
    {key: '101995,7048,default,basic_params_move_50km', colorGroup: 'GREY'},
    {
        key: '101995,7048,default,basic_params_move_50km.error',
        colorGroup: 'GREY',
    },
    {key: '101251,9986,default,BNA-queries', colorGroup: 'OTHER'},
    {key: '101251,9986,default,MRR-1tv-owner-5', colorGroup: 'OTHER'},
];

const EXPECTED_COLORS = {
    '100588,9236,default,images-corsa-unity-10': '#0000ff',
    '100588,9236,fresh,images-corsa-unity-10': '#000082',
    '100588,9238,default,images-corsa-unity-10': '#ff0000',
    '100588,9240,default,images-corsa-unity-10': '#820000',
    '101251,9986,default,BNA-queries': '#00c87d',
    '101251,9986,default,MRR-1tv-owner-5': '#004b00',
    '101995,7048,default,basic_params_move_50km': '#282828',
    '101995,7048,default,basic_params_move_50km.error': '#919191',
};

const SIMILAR_LINES = [
    {key: 'am', a: 0.4},
    {key: 'android', a: 0.4},
    {key: 'beak', a: 0.4},
];

const SIMILAR_EXPECTED_COLORS = {
    am: '#ffff0066',
    android: '#ffffc866',
    beak: '#37ff0066',
};

describe('Get color scheme', () => {
    test('for lines from RED, BLUE, GREY, OTHER color groups', () => {
        expect(getColorScheme(LINES)).toEqual(EXPECTED_COLORS);
    });

    test('after select another line', () => {
        const lines = [
            ...LINES,
            {
                key: '101251,9986,default,MRR-1tv-owner-5.error',
                colorGroup: 'OTHER',
            },
        ];
        const currentColors = getColorScheme(lines);
        const expectedColors = {
            ...EXPECTED_COLORS,
            '101251,9986,default,MRR-1tv-owner-5.error': '#7dc800',
        };
        expect(getColorScheme(lines, currentColors)).toEqual(expectedColors);
    });

    test('after select line and deselect another line', () => {
        let lines = [
            ...LINES,
            {key: '100588,9236,default,_404-binary-5', colorGroup: 'BLUE'},
        ];
        const currentColors = getColorScheme(lines);
        lines = lines.slice(1);

        const expectedColors = {
            '100588,9236,default,_404-binary-5': '#0000ff',
            '100588,9236,fresh,images-corsa-unity-10': '#007dff',
            '100588,9238,default,images-corsa-unity-10': '#ff0000',
            '100588,9240,default,images-corsa-unity-10': '#820000',
            '101251,9986,default,BNA-queries': '#00c87d',
            '101251,9986,default,MRR-1tv-owner-5': '#004b00',
            '101995,7048,default,basic_params_move_50km': '#282828',
            '101995,7048,default,basic_params_move_50km.error': '#919191',
        };
        expect(getColorScheme(lines, currentColors)).toEqual(expectedColors);
    });

    test('for similar group', () => {
        expect(getColorScheme(SIMILAR_LINES)).toEqual(SIMILAR_EXPECTED_COLORS);
    });

    test('if empty currentColors', () => {
        const currentColors: Dictionary<string, string> = {};
        expect(getColorScheme(LINES, currentColors)).toEqual(EXPECTED_COLORS);
    });

    test('if lines list is empty and without currentColors', () => {
        const lines: Line[] = [];
        const expectedColors = {};
        expect(getColorScheme(lines)).toEqual(expectedColors);
    });

    test('if lines list and currentColors are empty', () => {
        const lines: Line[] = [];
        const currentColors = {};
        const expectedColors = {};
        expect(getColorScheme(lines, currentColors)).toEqual(expectedColors);
    });

    test('after deselect line', () => {
        const currentColors = getColorScheme(LINES);
        const lines = LINES.slice(1);
        const expectedColors = {
            '100588,9236,fresh,images-corsa-unity-10': '#000082',
            '100588,9238,default,images-corsa-unity-10': '#ff0000',
            '100588,9240,default,images-corsa-unity-10': '#820000',
            '101251,9986,default,BNA-queries': '#00c87d',
            '101251,9986,default,MRR-1tv-owner-5': '#004b00',
            '101995,7048,default,basic_params_move_50km': '#282828',
            '101995,7048,default,basic_params_move_50km.error': '#919191',
        };
        expect(getColorScheme(lines, currentColors)).toEqual(expectedColors);
    });

    test('after select first line', () => {
        const lines: Line[] = [];
        const currentColors = getColorScheme(lines);
        lines.push({
            key: '100588,9236,default,images-corsa-unity-10',
            colorGroup: 'BLUE',
        });
        const expectedColors = {
            '100588,9236,default,images-corsa-unity-10': '#0000ff',
        };
        expect(getColorScheme(lines, currentColors)).toEqual(expectedColors);
    });

    test('if all lines in current colors', () => {
        const currentColors = EXPECTED_COLORS;
        expect(getColorScheme(LINES, currentColors)).toEqual(EXPECTED_COLORS);
    });
});
