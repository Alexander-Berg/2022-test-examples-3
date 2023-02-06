import {
    selectPool,
    selectPoolList,
    selectPoolIds,
    selectPoolListPrev,
    selectPoolListNext,
} from '~/src/features/Duty2/redux/DutyPool.selectors';
import {
    state,
    pool5,
    dutyPoolList,
    dutyPoolIds,
    prev5, next5,
} from './testData';

describe('DutyPool.selectors', () => {
    it('selectPool', () => {
        const pool = selectPool(state, 5);

        expect(pool).toEqual(pool5);
    });

    it('selectPoolList ', () => {
        const poolList = selectPoolList(state);

        expect(poolList).toEqual(dutyPoolList);
    });

    it('selectPoolIds ', () => {
        const poolIds = selectPoolIds(state);

        expect(poolIds).toEqual(dutyPoolIds);
    });

    it('selectPoolListPrev', () => {
        const prev = selectPoolListPrev(state);

        expect(prev).toEqual(prev5);
    });

    it('selectPoolListNext', () => {
        const next = selectPoolListNext(state);

        expect(next).toEqual(next5);
    });
});
