// import * as React from 'react';
// import { shallow } from 'enzyme';
import AirportsInfoComparator from './component';
// import { Dict } from '../../../../types';

describe('AirportsInfoComparator component', () => {
    //Not actual in current step
    // describe('Compare two tables', () => {
    //
    //     it('Compare tables with wrong car_number', () => {
    //         const CAR_NUMBERS_1 = [
    //             {car_number: null},
    //         ];
    //
    //         const CAR_NUMBERS_2 = [
    //             {car_number_wrong_key: 'x309ox799'},
    //         ];
    //
    //         const EXPECTED_RESULT: { common: Dict<any>[], diff: { table1: Dict<any>[], table2: Dict<any>[] } } = {
    //             common: [],
    //             diff: {table1: [{car_number: null}], table2: [{car_number_wrong_key: 'x309ox799'}]}
    //         };
    //
    //         const wrapper: any = shallow(<AirportsInfoComparator/>);
    //         expect(wrapper.instance().compareTables(CAR_NUMBERS_1, CAR_NUMBERS_2)).toEqual(EXPECTED_RESULT);
    //     });
    //
    //     it('Compare equal tables', () => {
    //         const CAR_NUMBERS_1 = [
    //             {car_number: 'x309ox799'},
    //             {car_number: 'y983yx750'},
    //             {car_number: 'b161xt750'}
    //         ];
    //
    //         const CAR_NUMBERS_2 = [
    //             {car_number: 'x309ox799'},
    //             {car_number: 'y983yx750'},
    //             {car_number: 'b161xt750'}
    //         ];
    //
    //         const EXPECTED_RESULT: { common: Dict<any>[], diff: { table1: Dict<any>[], table2: Dict<any>[] } } = {
    //             common: [{car_number: 'x309ox799'},
    //                 {car_number: 'y983yx750'},
    //                 {car_number: 'b161xt750'}],
    //             diff: {table1: [], table2: []}
    //         };
    //
    //         const wrapper: any = shallow(<AirportsInfoComparator/>);
    //         expect(wrapper.instance().compareTables(CAR_NUMBERS_1, CAR_NUMBERS_2)).toEqual(EXPECTED_RESULT);
    //     });
    //
    //     it('Compare not equal tables', () => {
    //         const CAR_NUMBERS_1 = [
    //             {car_number: 'x309ox799'},
    //             {car_number: 'b161xt750'}
    //         ];
    //
    //         const CAR_NUMBERS_2 = [
    //             {car_number: 'x309ox799'},
    //             {car_number: 'y983yx750'},
    //         ];
    //
    //         const EXPECTED_RESULT: { common: Dict<any>[], diff: { table1: Dict<any>[], table2: Dict<any>[] } } = {
    //             common: [{car_number: 'x309ox799'}],
    //             diff: {table1: [{car_number: 'b161xt750'}], table2: [{car_number: 'y983yx750'}]}
    //         };
    //
    //         const wrapper: any = shallow(<AirportsInfoComparator/>);
    //         expect(wrapper.instance().compareTables(CAR_NUMBERS_1, CAR_NUMBERS_2)).toEqual(EXPECTED_RESULT);
    //     });
    // });

    describe('Validate car number', () => {

        describe('Validate not string car number', () => {

            it('null', () => {
                expect(AirportsInfoComparator.isCarNumberValid(null)).toEqual({
                    isValid: false,
                    errors: ['Номер не является строкой'],
                });
            });

            it('undefined', () => {
                expect(AirportsInfoComparator.isCarNumberValid(undefined)).toEqual({
                    isValid: false,
                    errors: ['Номер не является строкой'],
                });
            });

            it('numeric 0', () => {
                expect(AirportsInfoComparator.isCarNumberValid(0)).toEqual({
                    isValid: false,
                    errors: ['Номер не является строкой'],
                });
            });

            it('numeric', () => {
                const NUMERIC_NUMBER = 300;

                expect(AirportsInfoComparator.isCarNumberValid(NUMERIC_NUMBER)).toEqual({
                    isValid: false,
                    errors: ['Номер не является строкой'],
                });
            });

            it('array', () => {
                expect(AirportsInfoComparator.isCarNumberValid(['x309ox799'])).toEqual({
                    isValid: false,
                    errors: ['Номер не является строкой'],
                });
            });

            it('object', () => {
                expect(AirportsInfoComparator.isCarNumberValid({ car_number: 'x309ox799' })).toEqual({
                    isValid: false,
                    errors: ['Номер не является строкой'],
                });
            });

            it('boolean', () => {
                expect(AirportsInfoComparator.isCarNumberValid(true)).toEqual({
                    isValid: false,
                    errors: ['Номер не является строкой'],
                });
            });
        });

        describe('Validate string car number', () => {
            it('valid string', () => {
                expect(AirportsInfoComparator.isCarNumberValid('x309ox799')).toEqual({
                    isValid: true,
                });
            });

            it('wrong string with small length', () => {
                expect(AirportsInfoComparator.isCarNumberValid('x309o')).toEqual({
                    isValid: false,
                    errors: ['Неверное число символов'],
                });
            });

            it('wrong string with big length', () => {
                expect(AirportsInfoComparator.isCarNumberValid('x309ox7999')).toEqual({
                    isValid: false,
                    errors: ['Неверное число символов'],
                });
            });

            it('wrong string without region', () => {
                expect(AirportsInfoComparator.isCarNumberValid('x309ox')).toEqual({
                    isValid: false,
                    errors: ['Неверное число символов', 'Отсутствует регион'],
                });
            });

            it('wrong string with wrong order (letters first)', () => {
                expect(AirportsInfoComparator.isCarNumberValid('xox309')).toEqual({
                    isValid: false,
                    errors: ['Неверное число символов', 'Неверный порядок символов'],
                });
            });

            it('wrong string with wrong order (numbers first)', () => {
                expect(AirportsInfoComparator.isCarNumberValid('309xox')).toEqual({
                    isValid: false,
                    errors: ['Неверное число символов', 'Неверный порядок символов'],
                });
            });
        });

        // it('Validate not string number', () => {
        //     expect(wrapper.instance().compareTables(CAR_NUMBERS_1, CAR_NUMBERS_2)).toEqual(EXPECTED_RESULT);
        // });

    });
});
