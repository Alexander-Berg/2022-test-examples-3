import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import YandexHelpMenuItem from '@self/root/src/widgets/content/YandexHelpMenuItem/components/View/__pageObject';

import {checkBlockSuite} from './yandexHelpMenuItemModule';

module.exports = makeSuite('Помощь рядом. Блок в Профиле', {
    feature: 'Помощь рядом',
    params: {
        pageId: 'id страниицы',
    },
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    yandexHelpMenuItem: () => this.createPageObject(YandexHelpMenuItem),
                });
            },
        },
        prepareSuite(checkBlockSuite, {
            suiteName: 'Пользователь без подписки.',
            params: {
                isSubscribedToYandexHelp: false,
                helpTitle: 'Помощь рядом',
                helpDescrption: 'Социальный проект Яндекса',
            },
        }),
        prepareSuite(checkBlockSuite, {
            suiteName: 'Пользователь с подпиской.',
            params: {
                isSubscribedToYandexHelp: true,
                helpTitle: 'Помощь рядом',
                helpDescrption: 'Спасибо, что помогаете!',
                checkIsBalanceVisible: true,
            },
        })
    ),
});
