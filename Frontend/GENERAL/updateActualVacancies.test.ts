import { updateActualVacancies, IActualVacanciesUpdate, mapPageToUrlPath } from './updateActualVacancies';
import { getShowAllUrl } from './getActualVacanciesHelpers';

jest.mock('./getActualVacanciesHelpers');

describe('updateActualVacancies', () => {
    let params: IActualVacanciesUpdate;

    beforeEach(() => {
        params = {
            id: 123,
            page: 'unknown page',
            publications: [],
        };
    });

    it('should use getShowAllUrl in updates', () => {
        updateActualVacancies(params);

        expect(getShowAllUrl).toBeCalled();
    });

    it('should pass correct page to getShowAllUrl (should use page map)', () => {
        updateActualVacancies({ ...params, page: Object.keys(mapPageToUrlPath)[0] });

        expect(getShowAllUrl).toHaveBeenLastCalledWith(Object.values(mapPageToUrlPath)[0], String(params.id));
    });

    it('should update vacancies to null if publications is not passed', () => {
        const { vacancies } = updateActualVacancies({ ...params, publications: null }).updates;

        expect(vacancies).toBe(null);
    });
});
