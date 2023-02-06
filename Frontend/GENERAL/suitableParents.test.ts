import { filterParentServiceItems } from './suitableParents';
import { suitableParentsResponse } from '../testData/suitableParentsResponse';

describe('utils', () => {
    describe('filterParentServiceItems', () => {
        it('Should find item if it is in array', () => {
            filterParentServiceItems(['169'], suitableParentsResponse);
        });

        it('Should not find item if it is not in array', () => {
            filterParentServiceItems(['56'], suitableParentsResponse);
        });
    });
});
