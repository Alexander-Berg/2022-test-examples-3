const gapMock = {
    type: 'type',
    work_in_absence: false,
    date_from: 'date',
    date_to: 'date',
};

const itemMock = {
    title: 'Title',
    url: 'https://staff.yandex-team.ru/id',
    id: 'id',
    layer: 'people',
    click_urls: ['click_url'],
    staff_id: 1234,
    uid: 'uid',
    login: 'login',
    name: {
        first: 'Екатерина',
        last: 'Чичварина (Гобарева)',
        middle: '',
    },
    phone: 'phone',
    department_name: 'department name',
    position: 'position',
    is_dismissed: false,
    is_memorial: false,
    is_robot: false,
    bicycles: [],
    cars: [],
    gaps: [],
};

const servicesItemMock = {
    title: 'Буткемпы',
    url: 'https://abc.yandex-team.ru/services/bootcamps',
    id: 7576,
    layer: 'services',
    click_urls: ['click_url'],
    slug: '@hlword@bootcam@/hlword@ps',
    status: 'develop',
    owner: {
        login: 'elena-bo',
        first_name: 'Елена',
        last_name: 'Богданович',
    },
};

const describeCases = {
    gaps: [
        {
            name: 'absence',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'absence' }] },
        },
        {
            name: 'conference trip',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'conference-trip' }] },
        },
        {
            name: 'conference',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'conference' }] },
        },
        {
            name: 'trip',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'trip' }] },
        },
        {
            name: 'maternity',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'maternity' }] },
        },
        {
            name: 'learning',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'learning' }] },
        },
        {
            name: 'vacation',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'vacation' }] },
        },
        {
            name: 'paid day off',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'paid-day-off' }] },
        },
        {
            name: 'illness',
            item: { ...itemMock, gaps: [{ ...gapMock, type: 'illness' }] },
        },
    ],
    highlights: [
        {
            name: 'title',
            item: {
                ...itemMock,
                title: '@hlword@Екатерина@/hlword@ Чичварина (Гобарева)',
            },
        },
        {
            name: 'login',
            item: {
                ...itemMock,
                login: '@hlword@log@/hlword@in',
            },
        },
        {
            name: 'phone',
            item: {
                ...itemMock,
                phone: '@hlword@12@/hlword@456',
            },
        },
        {
            name: 'department_name',
            item: {
                ...itemMock,
                department_name: '@hlword@deparmt@/hlword@ment',
            },
        },
    ],
    people: [
        {
            name: 'people_without_department',
            item: {
                ...itemMock,
                department_name: null,
            },
        },
    ],
    services: [
        {
            name: 'service_with_owner',
            item: {
                ...servicesItemMock,
            },
        },
        {
            name: 'service_without_owner',
            item: {
                ...servicesItemMock,
                owner: null,
            },
        },
    ],
};

module.exports = {
    describeCases,
};
