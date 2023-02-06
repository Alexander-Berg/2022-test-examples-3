'use strict';

const { convertToMobileFids, convertFromMobileFids } = require('./convert-backup-settings');

test('convertToMobileFids: should just return fids if no withTabs', () => {
    const result = convertToMobileFids({}, {
        tabs: [ 'relevant' ],
        fids: [ '1', '2', '3' ]
    });
    expect(result).toEqual({ fids: [ '1', '2', '3' ] });
});

test('convertToMobileFids: should convert tabs if no withTabs: 1', () => {
    const result = convertToMobileFids({ withTabs: '1' }, {
        tabs: [ 'relevant' ],
        fids: [ '1', '2', '3' ]
    });
    expect(result).toEqual({ fids: [ '-10', '1', '2', '3' ] });
});

test('convertFromMobileFids: should not convert tabs if no withTabs: 1', () => {
    const result = convertFromMobileFids({
        fids: [ '-10', '1', '2', '3' ]
    });
    expect(result).toEqual({ fids: [ '-10', '1', '2', '3' ] });
});

test('convertFromMobileFids: should convert tabs if no withTabs: 1', () => {
    const result = convertFromMobileFids({
        withTabs: '1',
        fids: [ '-10', '1', '2', '3' ]
    });
    expect(result).toEqual({ tabs: [ 'relevant' ], fids: [ '1', '2', '3' ] });
});
