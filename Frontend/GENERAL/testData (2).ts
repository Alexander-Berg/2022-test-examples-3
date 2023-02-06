import {
    IDutyPoolListItem,
    StoreForDutyPool,
} from '~/src/features/Duty2/redux/DutyPool.types';

export const pool5: IDutyPoolListItem = {
    id: 5,
    serviceId: 23,
    name: 'test5',
    slug: 'slug5',
    fullService: true,
    participants: [],
    autoupdate: true,
};

export const pool10: IDutyPoolListItem = {
    id: 10,
    serviceId: 213,
    name: 'test10',
    slug: 'slug10',
    fullService: true,
    participants: [],
    autoupdate: true,
};

export const dutyPoolList = {
    [pool5.id]: pool5,
    [pool10.id]: pool10,
};

export const dutyPoolIds = Object.keys(dutyPoolList).map(Number);
export const next5 = 'next5';
export const prev5 = 'prev5';

export const state: StoreForDutyPool = {
    dutyPool: {
        dutyPoolList: dutyPoolList,
        dutyPoolIds: dutyPoolIds,
        prev: prev5,
        next: next5,
    },
};
