const mainServices = [
    {
        application_id: 'wiki',
        name: 'Вики',
        title: 'Вики',
        url: 'https://wiki.yandex-team.ru/',
        title_en: 'Wiki',
        url_en: '',
        name_en: 'Wiki',
    },
    {
        application_id: 'startrek',
        name: 'Трекер',
        title: 'Трекер',
        url: 'https://st.yandex-team.ru/',
        title_en: 'Tracker',
        url_en: '',
        name_en: 'Tracker',
    },
    {
        application_id: 'mail',
        name: 'Почта',
        title: 'Почта',
        url: 'https://mail.yandex-team.ru/',
        title_en: 'Mail',
        url_en: '',
        name_en: 'Mail',
    },
    {
        application_id: 'staff',
        name: 'Стафф',
        title: 'Стафф',
        url: 'https://staff.yandex-team.ru/',
        title_en: 'Staff',
        url_en: '',
        name_en: 'Staff',
    },
    {
        application_id: 'at',
        name: 'Этушка',
        title: 'Этушка',
        url: 'https://my.at.yandex-team.ru/',
        title_en: 'Atushka',
        url_en: '',
        name_en: 'Atushka',
    },
    {
        application_id: 'cal',
        name: 'Календарь',
        title: 'Календарь',
        url: 'https://calendar.yandex-team.ru/',
        title_en: 'Calendar',
        url_en: 'https://calendar.yandex-team.ru/',
        name_en: 'Calendar',
    },
    {
        application_id: 'vconf',
        name: 'VConf',
        title: 'VConf',
        url: 'https://vconf.yandex-team.ru/',
        title_en: 'VConf',
        url_en: 'https://vconf.yandex-team.ru/',
        name_en: 'VConf',
    },
];

const secondaryServices = [
    {
        application_id: 'planner',
        name: 'ABC',
        title: 'ABC',
        url: 'https://abc.yandex-team.ru/',
        title_en: 'ABC',
        url_en: '',
        name_en: 'ABC',
    },
    {
        application_id: 'idm',
        name: 'IDM',
        title: 'IDM',
        url: 'https://idm.yandex-team.ru/',
        title_en: 'IDM',
        url_en: '',
        name_en: 'IDM',
    },
    {
        application_id: 'femida',
        name: 'Фемида',
        title: 'Фемида',
        url: 'https://femida.yandex-team.ru/',
        title_en: 'Femida',
        url_en: 'https://femida.yandex-team.ru/',
        name_en: 'Femida',
    },
    {
        application_id: 'invite',
        name: 'Переговорки',
        title: 'Переговорки',
        url: 'https://calendar.yandex-team.ru/invite/',
        title_en: 'Invite',
        url_en: '',
        name_en: 'Invite',
    },
    {
        application_id: 'libra',
        name: 'Библиотека',
        title: 'Библиотека',
        url: 'https://lib.yandex-team.ru/',
        title_en: 'Library',
        url_en: '',
        name_en: 'Library',
    },
    {
        application_id: 'doc',
        name: 'Документация',
        title: 'Документация',
        url: 'https://doc.yandex-team.ru/',
        title_en: 'Documentation',
        url_en: '',
        name_en: 'Documentation',
    },
    {
        application_id: 'lego',
        name: 'Лего',
        title: 'Лего',
        url: 'https://lego.yandex-team.ru/',
        title_en: 'Lego',
        url_en: '',
        name_en: 'Lego',
    },
    {
        application_id: 'mag',
        name: 'Журнал',
        title: 'Журнал',
        url: 'https://clubs.at.yandex-team.ru/mag/?from=header',
        title_en: 'Magazine',
        url_en: '',
        name_en: 'Magazine',
    },
    {
        application_id: 'design',
        name: 'Дизайн',
        title: 'Дизайн',
        url: 'https://wiki.yandex-team.ru/design/Design-code/',
        title_en: 'Design',
        url_en: '',
        name_en: 'Design',
    },
    {
        application_id: 'ml',
        name: 'Рассылки',
        title: 'Рассылки',
        url: 'https://ml.yandex-team.ru/',
        title_en: 'Maillists',
        url_en: '',
        name_en: 'Maillists',
    },
    {
        application_id: 'stat',
        name: 'Статистика',
        title: 'Статистика',
        url: 'https://stat.yandex-team.ru/',
        title_en: 'Statistics',
        url_en: '',
        name_en: 'Statistics',
    },
    {
        application_id: 'moebius',
        name: 'Мёбиус',
        title: 'Мёбиус',
        url: 'https://moe.yandex-team.ru/moebius/courses/',
        title_en: 'Möbius',
        url_en: 'https://moe.yandex-team.ru/moebius/courses/',
        name_en: 'Möbius',
    },
    {
        application_id: 'learning_offline',
        name: 'Очные курсы',
        title: 'Очные курсы',
        url: 'https://db.hranalytics.yandex-team.ru/education/catalog/enlist/',
        title_en: 'Studying program',
        url_en: 'https://db.hranalytics.yandex-team.ru/education/catalog/enlist/',
        name_en: 'Studying program',
    },
    {
        application_id: 'mentor',
        name: 'Ментор',
        title: 'Ментор',
        url: 'https://mentor.yandex-team.ru/',
        title_en: 'Mentor',
        url_en: 'https://mentor.yandex-team.ru/',
        name_en: 'Mentor',
    },
    {
        application_id: 'recommend',
        name: 'Порекомендовать',
        title: 'Порекомендовать',
        url: 'https://femida.yandex-team.ru/vacancies/publications/recommend/',
        title_en: 'Recommend',
        url_en: 'https://femida.yandex-team.ru/vacancies/publications/recommend/',
        name_en: 'Recommend',
    },
];

const menuHTML = require('../../../../dist/base/tools-menu.touch-phone').getContent({
    ctx: {
        items: [
            {
                text: 'Главная',
                url: '/',
                active: true,
            },
            {
                text: 'Не главная',
                url: 'http://yandex.ru',
            },
            {
                text: 'В новой вкладке',
                url: 'http://yandex.ru',
                target: '_blank',
            },
            {
                text: 'Кнопка',
                eventType: 'button-test',
                children: [],
            },
        ],
    },
});

module.exports = [
    {
        env: 'yateam'
    },
    {
        services: {
            main: mainServices,
        },
    },
    {
        services: {
            secondary: secondaryServices,
        },
    },
    {
        menuHTML,
        crossNavPopupHTML: '<h2>Test</h2>',
        services: {
            main: mainServices,
            secondary: secondaryServices,
        },
    },
];
