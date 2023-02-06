import {getEntityUrl, NO_GID_IN_ENTITY} from '../jmfApiRoutes';

describe('getEntityUrl', () => {
    it('just should work', () => {
        expect(getEntityUrl({gid: 'xxx@yyy'})).toBe('/entity/xxx@yyy');
    });

    it('should trow exception', () => {
        expect(() => {
            getEntityUrl({});
        }).toThrow(new Error(NO_GID_IN_ENTITY));
    });
});
