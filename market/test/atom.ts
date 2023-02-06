import {combine} from '@reatom/core';

import {userStateAtom} from '../../entities/user';

export const PageAtom = combine([
    userStateAtom,
]);
