import { ApproveRequest } from '../../../redux/types/requests';

export const getUser = (id: number) => ({
    affiliation: '',
    department: 1,
    // eslint-disable-next-line camelcase
    isRobot: false,
    uid: '2',
    // eslint-disable-next-line camelcase
    isDismissed: false,
    // eslint-disable-next-line camelcase
    firstName: { ru: `Имя${id}`, en: `firstName${id}` },
    lastName: { ru: `Фамилия${id}`, en: `lastName${id}` },

    id: id,
    login: `testUser${id}`,
    name: { ru: `Имя${id} Фамилия${id}`, en: `firstName${id} lastName${id}` },
});

export const getService = (id: number) => ({
    id,
    name: { ru: `Сервис${id}`, en: `Service${id}` },
    slug: `service${id}`,
});

type RequestGetter = (number: number) => ApproveRequest

export const getRequest: RequestGetter = number => ({
    actions: [],
    // eslint-disable-next-line camelcase
    createdAt: '',
    id: number,
    // eslint-disable-next-line camelcase
    moveTo: { id: number, name: { ru: `Родитель${number}`, en: `Parent${number}` }, slug: `parent${number}` },
    requester: getUser(number),
    approverIncoming: getUser(100 + number),
    service: { id: number + 100, name: { ru: `Ребенок${number + 100}`, en: `Child${number + 100}` }, slug: `child${number + 100}` },
    state: 'requested',
    // eslint-disable-next-line camelcase
    stateDisplay: { en: 'Requested', ru: 'Запрошен' },
});

export const requests = [getRequest(1), getRequest(2), getRequest(3)];
