import {isProduction, getTestingShop, getProductionShop, makeShotCase, makeShotSuite} from 'spec/utils';
import type {TestingShopName, ProductionShopName} from 'spec/utils';
import {PLATFORM_TYPE} from '@yandex-market/b2b-core/shared/constants';
import ShopInfo from './pageObjects/ShopInfo';

const getShop = isProduction ? getProductionShop : getTestingShop;

const suites = [
    {
        id: 'marketmbi-1673',
        rgb: 'white',
        status: 'old',
        page: 'market-partner:html:shops-dashboard:get',
        shop: isProduction ? 'test.yandex.ru' : 'auction-test-01.yandex.ru',
        env: 'all',
    },
    {
        id: 'marketmbi-1673',
        rgb: 'white',
        status: 'new',
        page: 'market-partner:html:order-wizard:get',
        shop: isProduction ? 'autotests-market-partner-web1.yandex.ru' : 'autotest-newby-invoice.ru',
        env: 'all',
    },
    {
        id: 'marketmbi-1588',
        rgb: 'white',
        status: 'failed',
        page: 'market-partner:html:shops-dashboard:get',
        shop: isProduction ? null : 'autotest-clone-1.yandex.ru',
        env: 'testing',
    },
] as const;

const names = {
    white: 'White',
    old: 'Old shop',
    new: 'New shop',
    failed: 'Shop that failed the check',
};

export default makeShotSuite({
    suiteName: 'Lights',
    feature: 'Подключение к Маркету',
    childSuites: suites.reduce((acc, suite) => {
        if (!suite.shop) return acc;
        const {campaignId, contacts} = getShop(suite.shop as TestingShopName & ProductionShopName);
        const user = contacts.owner;
        const commonProps = {
            issue: 'MARKETPARTNER-7691',
            id: suite.id,
            environment: suite.env || 'all',
        } as const;

        acc.push(
            // @ts-expect-error(TS2345) найдено в рамках MARKETPARTNER-16237
            makeShotSuite({
                suiteName: `${names[suite.rgb]} - ${names[suite.status]}`,
                page: {
                    route: suite.page,
                    params: {campaignId, platformType: PLATFORM_TYPE.SHOP},
                },
                user,
                childSuites: [
                    makeShotCase({
                        ...commonProps,
                        suiteName: 'Program',
                        selector: ShopInfo.root,
                    }),
                    makeShotCase({
                        ...commonProps,
                        suiteName: 'Program popup',
                        selector: ShopInfo.popupContent,
                        capture(actions, find) {
                            const campaignProgram = find(ShopInfo.trigger);
                            actions.click(campaignProgram);
                            return actions.wait(300);
                        },
                    }),
                ],
            }),
        );

        return acc;
    }, []),
});
