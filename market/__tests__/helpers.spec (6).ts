import {prepareCategoryOptions, prepareImportOptions} from '../helpers';

const CATEGORY_NAMES = ['order', 'team', 'ticket'];
const METACLASSES = {
    order: {title: 'Заказ'},
    team: {title: 'Линия'},
    ticket: {title: 'Обращение'},
};
const CATEGORY_OPTIONS = [
    {
        label: METACLASSES[CATEGORY_NAMES[0]].title,
        value: CATEGORY_NAMES[0],
    },
    {
        label: METACLASSES[CATEGORY_NAMES[1]].title,
        value: CATEGORY_NAMES[1],
    },
    {
        label: METACLASSES[CATEGORY_NAMES[2]].title,
        value: CATEGORY_NAMES[2],
    },
];

describe('prepareCategoryOptions', () => {
    it('выдача соответствует шаблону', () => {
        const prepared = prepareCategoryOptions(CATEGORY_NAMES, METACLASSES);

        expect(prepared).toEqual(CATEGORY_OPTIONS);
    });
});

// =================================================================================================

const IMPORTS = [
    {
        code: 'beruOutgoingCallTicketByOrderId',
        title: 'Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию',
    },
    {
        code: 'beruOutgoingTicketByEmail',
        title: 'Создание исходящих обращений по e-mail и внешнему комментарию',
    },
    {
        code: 'beruOutgoingTicketByOrderId',
        title: 'Создание исходящих обращений по номеру заказа и внешнему комментарию',
    },
    {
        code: 'ticketChangeStatusAddComment',
        title: 'Смена статуса с добавлением комментариев',
    },
    {
        code: 'beruOutgoingCallTicketByClientPhone',
        title: 'Создание исходящих обращений телефонии по номеру телефона и внутреннему комментарию',
    },
];

const IMPORTS_OPTIONS = [
    {value: IMPORTS[0].code, label: IMPORTS[0].title},
    {value: IMPORTS[1].code, label: IMPORTS[1].title},
    {value: IMPORTS[2].code, label: IMPORTS[2].title},
    {value: IMPORTS[3].code, label: IMPORTS[3].title},
    {value: IMPORTS[4].code, label: IMPORTS[4].title},
];

describe('prepareImportOptions', () => {
    it('выдача соответствует шаблону', () => {
        const prepared = prepareImportOptions(IMPORTS);

        expect(prepared).toEqual(IMPORTS_OPTIONS);
    });
});
