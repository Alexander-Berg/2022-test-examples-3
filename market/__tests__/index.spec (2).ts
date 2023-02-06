import {validFqns, invalidFqns, vaildGids, invalidGids, possibleEntityValues} from './fixtures';
import {isFqn, getFqnType, getFqnId, isGid, isEqualEntityValues} from '..';

declare const test: jest.It;

describe('jmfUtils', () => {
    describe('isFqn', () => {
        test.each(validFqns)('"%s" is a fqn', string => {
            expect(isFqn(string)).toBeTruthy();
        });

        test.each(invalidFqns)(`"%s" isn't a fqn`, string => {
            expect(isFqn(string)).toBeFalsy();
        });

        test.each(vaildGids)(`"%s" isn't a fqn`, string => {
            expect(isFqn(string)).toBeFalsy();
        });
    });

    describe('isGid', () => {
        test.each(vaildGids)(`"%s" is a gid`, string => {
            expect(isGid(string)).toBeTruthy();
        });

        test.each(invalidGids)(`"%s" is not a gid`, string => {
            expect(isGid(string)).toBeFalsy();
        });

        test.each(validFqns)(`"%s" is not a gid`, string => {
            expect(isGid(string)).toBeFalsy();
        });
    });

    describe('getFqnId and getFqnType', () => {
        test.each`
            fqn                                    | fqnId                      | fqnType
            ${'systemEntity'}                      | ${'systemEntity'}          | ${null}
            ${'needsHelpAlert'}                    | ${'needsHelpAlert'}        | ${null}
            ${'service$inboundTelephony'}          | ${'service'}               | ${'inboundTelephony'}
            ${'serviceTime'}                       | ${'serviceTime'}           | ${null}
            ${'attachment'}                        | ${'attachment'}            | ${null}
            ${'mailMessage$in'}                    | ${'mailMessage'}           | ${'in'}
            ${'ticket'}                            | ${'ticket'}                | ${null}
            ${'ticket$fmcgTelephony'}              | ${'ticket'}                | ${'fmcgTelephony'}
            ${'import$orderTicketsManualCreation'} | ${'import'}                | ${'orderTicketsManualCreation'}
            ${'checkouterClientRole'}              | ${'checkouterClientRole'}  | ${null}
            ${'entity_version'}                    | ${'entity_version'}        | ${null}
            ${'comment$user'}                      | ${'comment'}               | ${'user'}
            ${'service$outboundTelephony'}         | ${'service'}               | ${'outboundTelephony'}
            ${'orderHistoryEventType'}             | ${'orderHistoryEventType'} | ${null}
            ${'attachment$direct'}                 | ${'attachment'}            | ${'direct'}
            ${'logisticSupportRules$edit'}         | ${'logisticSupportRules'}  | ${'edit'}
        `(
            `
        fqn: $fqn
        fqnId: $fqnId
        fqnType: $fqnType
    `,
            ({fqn, fqnId, fqnType}) => {
                expect(getFqnId(fqn)).toBe(fqnId);
                expect(getFqnType(fqn)).toBe(fqnType);
            }
        );
    });

    describe('isEqualEntityValues', () => {
        test.each(possibleEntityValues)(
            'Сравнение null и %s',
            // @ts-ignore
            (name, value) => {
                expect(isEqualEntityValues(null, value)).toEqual(name === 'null');
            }
        );

        test.each(possibleEntityValues)(
            'Сравнение undefined и %s',
            // @ts-ignore
            (name, value) => {
                expect(isEqualEntityValues(undefined, value)).toEqual(name === 'undefined');
            }
        );

        test.each(possibleEntityValues)(
            'Сравнение числа "123" и %s',
            // @ts-ignore
            (name, value) => {
                expect(isEqualEntityValues(123, value)).toEqual(name === 'number');
            }
        );

        test.each(possibleEntityValues)(
            'Сравнение строки "test" и %s',
            // @ts-ignore
            (name, value) => {
                expect(isEqualEntityValues('test', value)).toEqual(name === 'string');
            }
        );

        test.each(possibleEntityValues)(
            'Сравнение сущности и %s',
            // @ts-ignore
            (name, value) => {
                expect(isEqualEntityValues({gid: 'test@1', metaclass: 'test'}, value)).toEqual(name === 'entity');
            }
        );

        test.each(possibleEntityValues)(
            'Сравнение массива сущностей сущности и %s',
            // @ts-ignore
            (name, value) => {
                expect(
                    isEqualEntityValues(
                        [
                            {gid: 'test@1', metaclass: 'test'},
                            {gid: 'test@2', metaclass: 'test'},
                        ],
                        value
                    )
                ).toEqual(name === 'entity array');
            }
        );

        test.each(possibleEntityValues)(
            'Сравнение массива строк сущности и %s',
            // @ts-ignore
            (name, value) => {
                expect(isEqualEntityValues(['test1', 'test2', 'test3'], value)).toEqual(name === 'string array');
            }
        );

        test.each(possibleEntityValues)(
            'Сравнение массива чисел сущности и %s',
            // @ts-ignore
            (name, value) => {
                expect(isEqualEntityValues([1, 2, 3], value)).toEqual(name === 'number array');
            }
        );
    });
});
