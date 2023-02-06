import {makeSuite, mergeSuites, makeCase} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import TooltipContent
    from '@self/root/src/widgets/content/YandexHelpOnboardingTooltip/components/TooltipContent/__pageObject';

const TITLE_TEXT = 'Помощь рядом';
const MAIN_TEXT = 'Простой способ помогать, покупая на Маркете. Подробнее';
const LINK_TEXT = 'Подробнее';

module.exports = makeSuite('Помощь рядом. Попап на главной.', {
    environment: 'kadavr',
    defaultParams: {
        pageId: PAGE_IDS_COMMON.INDEX,
    },
    feature: 'Помощь рядом',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    tooltipContent: () => this.createPageObject(TooltipContent),
                });
            },
        },
        {
            'Отображение попапа Помощи рядом': makeCase({
                id: 'marketfront-4586',
                issue: 'MARKETFRONT-36815',
                async test() {
                    await this.tooltipContent.waitForAppearance()
                        .should.eventually.to.be.equal(true, 'Отображается тултип Помощи');

                    await this.tooltipContent.isIconVisible()
                        .should.eventually.to.be.equal(true, 'Иконка должна отображаться');

                    await this.tooltipContent.getTitle()
                        .should.eventually.to.be.equal(TITLE_TEXT, `Заголовок блока "${TITLE_TEXT}"`);

                    await this.tooltipContent.getMainText()
                        .should.eventually.to.be.equal(MAIN_TEXT, `Основной текст блока "${MAIN_TEXT}"`);

                    await this.tooltipContent.getLinkText()
                        .should.eventually.to.be.equal(LINK_TEXT, `Текст ссылки "${LINK_TEXT}"`);

                    const link = await this.browser.yaBuildURL(PAGE_IDS_COMMON.YANDEX_HELP);
                    const expectedFullLink = `https:${link}`;

                    await this.tooltipContent.getLinkUrl()
                        .should.eventually.to.be.link(expectedFullLink, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHash: true,
                        });
                },
            }),
        }
    ),
});
