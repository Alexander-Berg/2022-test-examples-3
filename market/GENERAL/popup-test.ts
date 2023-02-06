import {describe, simpleAssertLink} from 'spec/utils';
import RowContainer from '../page-objects/rowContainer';
import type {Ctx} from '..';

type RowNames = 'externalStat' | 'passMarketParams' | 'shopName' | 'showRating' | 'metrika';

const testPopupText = (ctx: Ctx) => async (componentName: RowNames) => {
    const instance = (ctx.commonInfo[componentName] as unknown) as typeof RowContainer;
    await instance
        .isTooltopExist()
        .should.eventually.equal(true, 'Проверяем доступность тултипа с подсказкой на странице');
    await instance.clickOnTooltip();
    await instance.popup.waitDialogExist();
    await ctx.browser.allure.runStep('В попапе должен быть текст', () => instance.popup.content.getText());
};

const testPopupLink = (ctx: Ctx) => async ({
    componentName,
    href,
    expectedHref = href,
    withoutClickOnLink = false,
}: {
    componentName: RowNames;
    href: string;
    expectedHref?: string;
    withoutClickOnLink?: boolean;
}) => {
    const instance = (ctx.commonInfo[componentName] as unknown) as typeof RowContainer;
    if (withoutClickOnLink) {
        await simpleAssertLink(ctx, {
            link: instance.popup.elem(`a[href*="${href}"]`),
            expectedUrl: href,
            sameWindow: false,
        });

        return;
    }

    await ctx.browser.allure.runStep(`Находим ссылку, заканчивающуюся на ${href} и совершаем клик по ней`, () =>
        instance.popup.elem(`a[href*="${href}"]`).click(),
    );
    await ctx.browser.yaSwitchWindow();
    await ctx.browser
        .getUrl()
        .should.eventually.match(
            new RegExp(`^https:${expectedHref}`),
            `URL открывшейся страницы должен быть https:${href}`,
        );
};

export default describe<Ctx>(
    {
        title: 'Общие настройки. Попапы',
        environment: 'all',
        feature: 'Подключение к Маркету',
    },
    it => {
        it(
            {
                title: 'Общие настройки. Текст подсказки у поля "Название магазина для покупателей"',
                id: 'marketmbi-1558',
                issue: 'MARKETPARTNER-7640',
            },
            async ctx => {
                const componentName = 'shopName';

                await testPopupText(ctx)(componentName);
                await testPopupLink(ctx)({
                    componentName,
                    href: '//yandex.ru/support/partnermarket/registration/authorization.html',
                    withoutClickOnLink: true,
                });
            },
        );

        it(
            {
                title: 'Общие настройки. Подсказка у галочки про Внешнюю интернет-статистику',
                id: 'marketmbi-1563',
                issue: 'MARKETPARTNER-7645',
            },
            async ctx => {
                await testPopupText(ctx)('externalStat');
            },
        );

        it(
            {
                title: 'Общие настройки. Подсказка у галочки про параметры перехода с Маркета',
                id: 'marketmbi-1564',
                issue: 'MARKETPARTNER-7646',
            },
            async ctx => {
                await testPopupText(ctx)('passMarketParams');
            },
        );

        it(
            {
                title: 'Общие настройки. Подсказка у галочки про рейтинг в местах размещения',
                id: 'marketmbi-1562',
                issue: 'MARKETPARTNER-7644',
            },
            async ctx => {
                const componentName = 'showRating';

                await testPopupText(ctx)(componentName);
                await testPopupLink(ctx)({
                    componentName,
                    href: '//yandex.ru/support/partnermarket/rating.html',
                    withoutClickOnLink: true,
                });
            },
        );

        it(
            {
                title: 'Общие настройки. Подсказка у галочки про Интеграцию с Метрикой. Ссылка на отправку данных',
                id: 'marketmbi-1566',
                issue: 'MARKETPARTNER-7647',
            },
            async ctx => {
                const componentName = 'metrika';

                await testPopupText(ctx)(componentName);
                await testPopupLink(ctx)({
                    componentName,
                    href: '//yandex.ru/support/metrica/data/e-commerce.html#e-commerce__ecommerce-enable',
                    withoutClickOnLink: true,
                });
            },
        );

        it(
            {
                title: 'Общие настройки. Подсказка у галочки про Интеграцию с Метрикой. Ссылка на цели',
                id: 'marketmbi-1567',
                issue: 'MARKETPARTNER-7647',
            },
            async ctx => {
                const componentName = 'metrika';

                await testPopupText(ctx)(componentName);
                await testPopupLink(ctx)({
                    componentName,
                    href: '//yandex.ru/support/metrica/general/goals.html',
                    withoutClickOnLink: true,
                });
            },
        );

        it(
            {
                title:
                    'Общие настройки. Подсказка у галочки про Интеграцию с Метрикой. Ссылка на сертифицированные агентства',
                id: 'marketmbi-1568',
                issue: 'MARKETPARTNER-7647',
            },
            async ctx => {
                const componentName = 'metrika';

                await testPopupText(ctx)(componentName);
                await testPopupLink(ctx)({
                    componentName,
                    href: '//yandex.ru/adv/contact/agency',
                    expectedHref: '//yandex.ru/adv/contact/agencies',
                });
            },
        );
    },
);
