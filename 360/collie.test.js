'use strict';

const collieHelper = require('./collie.js');

test('convert params to contact', () => {
    const params = {
        first_name: 'f',
        last_name: 'l',
        middle_name: 'm',
        b_day: '1',
        b_month1: '2',
        b_year: '3',
        descr: '',
        mail_addr: 'e',
        tel_list: '0'
    };

    const result = collieHelper.convertParamsToContact(params);
    expect(result).toEqual({
        name: { first: 'f', last: 'l', middle: 'm' },
        birthdate: { day: 1, month: 2, year: 3 },
        description: '',
        emails: [ 'e' ],
        phones: [ '0' ]
    });
});

test('convert params to contact. Arrays', () => {
    const params = {
        mail_addr: [ 'e', 'x' ],
        tel_list: [ '0', '1' ]
    };

    const result = collieHelper.convertParamsToContact(params);
    expect(result).toEqual({
        emails: [ 'e', 'x' ],
        phones: [ '0', '1' ]
    });
});

test('convert params to contact. Empty', () => {
    const result = collieHelper.convertParamsToContact({});
    expect(result).toEqual({});
});

test('cast mcid', () => {
    const result = collieHelper.castMcid([ '1', '1.1', '2', '2.3' ], 'a', 'b');
    expect(result).toEqual({
        a: [ '1.1', '2.3' ],
        b: [ '1', '2' ]
    });
});

test('cast mcid no emails', () => {
    const result = collieHelper.castMcid([ '1', '3', '2' ], 'a', 'b');
    expect(result).toEqual({
        b: [ '1', '3', '2' ]
    });
});

test('cast mcid no contacts', () => {
    const result = collieHelper.castMcid([ '1.1', '2.1', '2.3' ], 'a', 'b');
    expect(result).toEqual({
        a: [ '1.1', '2.1', '2.3' ]
    });
});

test('cast empty mcid', () => {
    const result = collieHelper.castMcid(undefined, 'a', 'b');
    expect(result).toEqual({});
});
