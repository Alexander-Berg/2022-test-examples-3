import { assert } from 'chai';

import type { ISerpDocument, IPrivExternals } from '../../../../typings';
import type { ISnippetContext } from '../../../../lib/Context/SnippetContext';
import type { IAfishaCinemaScheduleMovieEvent } from '../../Companies.typings/IOneOrgPrivState';
import { AdapterCompaniesAfishaCinemaSchedule as Base } from './AfishaCinemaSchedule@desktop.server';
import type { ICompaniesAfishaCinemaScheduleSnippet } from '.';

// Нужен для тестирования protected-методов
class AdapterCompaniesAfishaCinemaSchedule extends Base {
    __getItemMark(ratings: IAfishaCinemaScheduleMovieEvent['ratings']) {
        return super.getItemMark(ratings);
    }

    __getItemMarkBgColor(rating: number) {
        return super.getItemMarkBgColor(rating);
    }
}

describe('AdapterCompaniesAfishaCinemaSchedule', () => {
    let adapter: AdapterCompaniesAfishaCinemaSchedule;

    beforeEach(() => {
        adapter = new AdapterCompaniesAfishaCinemaSchedule({
            context: {} as ISnippetContext,
            snippet: {
                state: {},
                params: {},
            } as ICompaniesAfishaCinemaScheduleSnippet,
            document: {} as ISerpDocument,
            privExternals: {} as IPrivExternals,
        });
    });

    describe('getItemMark', () => {
        const RATING_GOOD_VALUES = [.3, 1, 5.123, 10];
        const RATING_BAD_VALUES = [0, NaN, 11, -1, undefined];
        let ratings: IAfishaCinemaScheduleMovieEvent['ratings'];

        RATING_GOOD_VALUES.forEach(rating => {
            it('should return mark data if rating between 0 and 10', () => {
                ratings = { kinopoisk: rating };

                const result = adapter.__getItemMark(ratings);

                assert.isString(result?.text);
            });
        });

        RATING_BAD_VALUES.forEach(rating => {
            it('should return nothing if rating has not expected value', () => {
                ratings = { kinopoisk: rating };

                const result = adapter.__getItemMark(ratings);

                assert.isUndefined(result);
            });
        });

        it('should format rating value', function() {
            ratings = { kinopoisk: 5.783 };

            const result = adapter.__getItemMark(ratings);

            assert.equal(result?.text, '5,8');
        });

        it('should use IMDb rating if KP rating does not exist', function() {
            ratings = { imdb: 4 };

            const result = adapter.__getItemMark(ratings);

            assert.equal(result?.text, '4,0');
        });
    });

    describe('getItemMarkBgColor', () => {
        [8, 10, 100].forEach(rating => {
            it('should return correct value for rating values from 8', () => {
                const result = adapter.__getItemMarkBgColor(rating);

                assert.strictEqual(result, '#32ba43');
            });
        });

        [7, 8 - 0.0001].forEach(rating => {
            it('should return correct value for rating values between 7 and 8', () => {
                const result = adapter.__getItemMarkBgColor(rating);

                assert.strictEqual(result, '#89c939');
            });
        });

        [5, 7 - 0.0001].forEach(rating => {
            it('should return correct value for rating values between 5 and 7', () => {
                const result = adapter.__getItemMarkBgColor(rating);

                assert.strictEqual(result, '#91a449');
            });
        });

        [3, 5 - 0.0001].forEach(rating => {
            it('should return correct value for rating values between 3 and 5', () => {
                const result = adapter.__getItemMarkBgColor(rating);

                assert.strictEqual(result, '#85855d');
            });
        });

        [0, -1, 3 - 0.0001].forEach(rating => {
            it('should return correct value for trash rating values', () => {
                const result = adapter.__getItemMarkBgColor(rating);

                assert.strictEqual(result, '#727272');
            });
        });
    });
});
