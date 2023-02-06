import {makeSuite, makeCase} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

export const checkBlockSuite = makeSuite('Блок в Профиле', {
    feature: 'Помощь рядом',
    story: {
        'Отображение блока': makeCase({
            id: 'marketfront-4584',
            issue: 'MARKETFRONT-36815',
            async test() {
                const {helpTitle, helpDescrption, checkIsBalanceVisible = false} = this.params;

                await this.yandexHelpMenuItem.isVisible()
                    .should.eventually.be.equal(true, 'Пункт меню "Помощь рядом" должен отображаться');

                await this.yandexHelpMenuItem.getTitle()
                    .should.eventually.be.equal(helpTitle, `Отображается блок с заголовком "${helpTitle}"`);

                await this.yandexHelpMenuItem.getDescription()
                    .should.eventually.be.equal(
                        helpDescrption,
                        `Отображается текст "${helpDescrption}"`
                    );

                await this.yandexHelpMenuItem.isIconVisible()
                    .should.eventually.be.equal(true, 'Иконка должна отображаться');

                if (checkIsBalanceVisible) {
                    await this.yandexHelpMenuItem.isBalanceVisible()
                        .should.eventually.be.equal(
                            true,
                            'Отображается баланс отчислений в фонд Помощи'
                        );
                }

                const link = await this.browser.yaBuildURL(PAGE_IDS_COMMON.YANDEX_HELP);
                const expectedFullLink = `http:${link}`;

                await this.yandexHelpMenuItem.getLinkUrl()
                    .should.eventually.to.be.link({
                        host: expectedFullLink,
                    }, {
                        mode: 'match',
                    });
            },
        }),
    },
});
