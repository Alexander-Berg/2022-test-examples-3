import { parse, RegistrationPlate } from './vehiclePlates';

describe('Lib. vehiclePlates.', () => {
    describe('#taxiPlatesFormat', () => {
        [
            ['x999xx99', { plate: 'x999xx99', type: RegistrationPlate.Car, parts: ['x', '999', 'xx', '99'] }],
            ['x999xx999', { plate: 'x999xx999', type: RegistrationPlate.Car, parts: ['x', '999', 'xx', '999'] }],
            ['X999XX999', { plate: 'X999XX999', type: RegistrationPlate.Car, parts: ['x', '999', 'xx', '999'] }],
            ['x999Xx99', { plate: 'x999Xx99', type: RegistrationPlate.Car, parts: ['x', '999', 'xx', '99'] }],
            ['xx99999', { plate: 'xx99999', type: RegistrationPlate.Taxi, parts: ['xx', '999', '99'] }],
            ['xx999999', { plate: 'xx999999', type: RegistrationPlate.Taxi, parts: ['xx', '999', '999'] }],
            ['XX999999', { plate: 'XX999999', type: RegistrationPlate.Taxi, parts: ['xx', '999', '999'] }],
        ].forEach(([actual, expected]) => {
            it(`should return formatted plate for "${actual}"`, () => {
                expect(parse(actual)).toEqual(expected);
            });
        });

        [
            '9999xx',
            '',
            'xxx',
            '9999'
        ].forEach(plate => {
            it(`should not formatted "${plate}" registration plate`, () => {
                expect(parse(plate)).toEqual({ plate, type: RegistrationPlate.Unknown, parts: [] });
            });
        });
    });
});
