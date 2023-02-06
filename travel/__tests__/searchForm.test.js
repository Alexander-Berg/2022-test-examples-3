const parseDate = jest.fn();

jest.setMock('../../lib/date/parse', parseDate);

const getParseParams = jest.fn();

jest.setMock('../../lib/date/utils', {getParseParams});

const {mockStore} = require.requireActual('./mockStore');
const {findAction} = require.requireActual('../../lib/testUtils/store');

const {setFormDataFromRequestWithSlugs, SET_FORM_DATA, SET_USER_INPUT} =
    require.requireActual('../searchForm');

const {TRAIN_TYPE, PLANE_TYPE, SUBURBAN_TYPE, BUS_TYPE, ALL_TYPE} =
    require.requireActual('../../lib/transportType');

const getActions = actions => ({
    setFormData: findAction(actions, SET_FORM_DATA),
    setUserInput: findAction(actions, SET_USER_INPUT),
});

const initialState = {
    searchForm: {
        from: {title: '', key: ''},
        originalFrom: {title: '', key: ''},
        to: {title: '', key: ''},
        originalTo: {title: '', key: ''},
        when: {text: ''},
        userInput: {
            from: {title: '', key: ''},
            to: {title: '', key: ''},
        },
    },
    language: 'ru',
};

describe('setFormDataFromRequestWithSlugs', () => {
    const from = {slug: 'moscow'};
    const to = {slug: 'yekaterinburg'};

    it('Поиск любым транспортом Москва - Екатеринбург 09.05.2018', () => {
        const parsedDate = {
            text: '9 мая',
            date: '2018-05-09',
        };

        parseDate.mockReturnValue(parsedDate);

        const store = mockStore(initialState);

        store.dispatch(
            setFormDataFromRequestWithSlugs(
                {},
                {
                    fromSlug: 'moscow',
                    toSlug: 'yekaterinburg',
                    period: '2018-05-09',
                },
                false,
            ),
        );

        const actions = getActions(store.getActions());

        expect(actions.setFormData.payload).toEqual({
            transportType: ALL_TYPE,
            from: {},
            to: {},
            originalFrom: from,
            originalTo: to,
            when: parsedDate,
            plan: null,
        });
    });

    it('Поиск поезда Москва - Екатеринбург 09.05.2018', () => {
        const parsedDate = {
            text: '9 мая',
            date: '2018-05-09',
        };

        parseDate.mockReturnValue(parsedDate);

        const store = mockStore(initialState);

        store.dispatch(
            setFormDataFromRequestWithSlugs(
                {},
                {
                    transportType: 'train',
                    fromSlug: 'moscow',
                    toSlug: 'yekaterinburg',
                    period: '2018-05-09',
                },
                false,
            ),
        );

        const actions = getActions(store.getActions());

        expect(actions.setFormData.payload).toEqual({
            transportType: TRAIN_TYPE,
            from: {},
            to: {},
            originalFrom: from,
            originalTo: to,
            when: parsedDate,
            plan: null,
        });
    });

    it('Поиск самолета Москва - Екатеринбург на все дни', () => {
        const parsedDate = {
            text: 'на все дни',
            special: 'all-days',
        };

        parseDate.mockReturnValue(parsedDate);

        const store = mockStore(initialState);

        store.dispatch(
            setFormDataFromRequestWithSlugs(
                {plan: 'plane'},
                {
                    transportType: 'plane',
                    fromSlug: 'moscow',
                    toSlug: 'yekaterinburg',
                    period: 'на все дни',
                },
                false,
            ),
        );

        const actions = getActions(store.getActions());

        expect(actions.setFormData.payload).toEqual({
            transportType: PLANE_TYPE,
            from: {},
            to: {},
            originalFrom: from,
            originalTo: to,
            when: parsedDate,
            plan: null,
        });
    });

    it('Поиск ближайщего автобуса Москва - Екатеринбург', () => {
        const parsedDate = {
            text: '3 апреля',
            special: 'today',
        };

        parseDate.mockReturnValue(parsedDate);

        const store = mockStore(initialState);

        store.dispatch(
            setFormDataFromRequestWithSlugs(
                {},
                {
                    transportType: 'bus',
                    fromSlug: 'moscow',
                    toSlug: 'yekaterinburg',
                    period: 'next',
                },
            ),
        );

        const actions = getActions(store.getActions());

        expect(actions.setFormData.payload).toEqual({
            transportType: BUS_TYPE,
            from: {},
            to: {},
            originalFrom: from,
            originalTo: to,
            when: parsedDate,
            plan: null,
        });
    });

    it('Поиск электричек Москва - Екатеринбург на все дни', () => {
        const parsedDate = {
            text: 'на все дни',
            special: 'all-days',
        };

        parseDate.mockReturnValue(parsedDate);

        const store = mockStore(initialState);

        store.dispatch(
            setFormDataFromRequestWithSlugs(
                {plan: 'plan'},
                {
                    transportType: 'suburban',
                    fromSlug: 'moscow',
                    toSlug: 'yekaterinburg',
                    period: 'на все дни',
                },
                false,
            ),
        );

        const actions = getActions(store.getActions());

        expect(actions.setFormData.payload).toEqual({
            transportType: SUBURBAN_TYPE,
            from: {},
            to: {},
            originalFrom: from,
            originalTo: to,
            when: parsedDate,
            plan: 'plan',
        });
    });
});
