import { Path } from 'shared/consts/Path';
import { generateRouterPath } from 'shared/helpers/generateRouterPath/generateRouterPath';

describe('generateRouterPath', () => {
    it('work with simple', () => {
        expect(generateRouterPath(Path.HOME)).toEqual('/');
    });

    it('work with params', () => {
        expect(generateRouterPath(Path.CAR_OVERVIEW, { id: 'kek' })).toEqual('/cars/kek/overview');
    });
});
