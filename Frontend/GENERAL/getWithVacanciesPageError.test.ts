import { getWithVacanciesPageError } from './getWithVacanciesPageError';
import { getWithVacanciesSection } from '../stubs/withVacanciesSection';
import { getWithVacanciesErrorSection } from '../stubs/withVacanciesSectionAfterError';

describe('getWithVacanciesPageError', () => {
    it('should return error only', () => {
        expect(getWithVacanciesPageError(getWithVacanciesSection())).toEqual(getWithVacanciesErrorSection());
    });
});
