const contactType = {
    id: 100,
    code: 'communication_contact',
    name: { ru: 'Контакт для связи', en: 'Communication contact' },
};
export const getSampleContacts = () => ([
    {
        title: { ru: 'Сайт', en: 'Сайт' },
        content: 'https://abc.yandex-team.ru/',
        comment: '',
        url: 'https://abc.yandex-team.ru/',
        type: contactType,
    }, {
        id: 2,
        title: { ru: 'Внешний сайт', en: 'Внешний сайт' },
        content: 'https://yandex.ru/',
        comment: '',
        url: 'http://h.yandex.net?https%3A//yandex.ru/',
        type: contactType,
    }, {
        id: 3,
        title: { ru: 'Неанонимная форма', en: 'Неанонимная форма' },
        content: 'https://forms.yandex-team.ru/surveys/62156/',
        comment: '',
        url: 'https://forms.yandex-team.ru/surveys/62156/',
        type: contactType,
    }, {
        id: 4,
        title: { ru: 'Анонимная форма', en: 'Анонимная форма' },
        content: 'https://forms.yandex.net/surveys/62156/',
        comment: '',
        url: 'http://h.yandex.net?https%3A//forms.yandex.net/surveys/62156/',
        type: contactType,
    },
]);
