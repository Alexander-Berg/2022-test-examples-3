import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import ShareButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/SocialShareButtons/shareButton';

/**
 * Тесты на разворачивалку в блоке соц кнопок
 * @property {PageObject.SocialShareButtons} socialShareButtons
 */
export default makeSuite('Кнопки шаринга.', {
    params: {
        shareInfo: 'Информация для шаринга',
    },
    story: mergeSuites(
        prepareSuite(ShareButtonSuite, {
            suiteName: 'Кнопка шаринга Вконтакте.',
            params: {
                index: 0,
                service: 'vk',
            },
        }),
        prepareSuite(ShareButtonSuite, {
            suiteName: 'Кнопка шаринга Одноклассники.',
            params: {
                index: 1,
                service: 'ok',
            },
        }),
        // MARKETFRONT-79166: Кнопку удаляем индексы сдвинулись
        // prepareSuite(ShareButtonSuite, {
        //     suiteName: 'Кнопка шаринга Facebook.',
        //     params: {
        //         index: 2,
        //         service: 'fb',
        //     },
        // }),
        prepareSuite(ShareButtonSuite, {
            suiteName: 'Кнопка шаринга Whatsapp.',
            params: {
                index: 2,
                service: 'whatsapp',
            },
        }),
        prepareSuite(ShareButtonSuite, {
            suiteName: 'Кнопка шаринга Telegram.',
            params: {
                index: 3,
                service: 'telegram',
            },
        }),
        prepareSuite(ShareButtonSuite, {
            suiteName: 'Кнопка шаринга Мой Мир.',
            params: {
                index: 4,
                service: 'moimir',
            },
        }),
        prepareSuite(ShareButtonSuite, {
            suiteName: 'Кнопка шаринга Twitter.',
            params: {
                index: 5,
                service: 'twitter',
            },
        })
    ),
});
