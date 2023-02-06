import FooterMarket from '@self/platform/spec/page-objects/footer-market';
import url from 'url';

const widgetPath = '../';
export const BASE_URL = 'm.market.yandex.ru';

export const EXPECTED_QUERY_PARAMS = [
    {
        paramName: 'pda-redir',
        paramValue: '1',
    },
    {
        paramName: 'track',
        paramValue: 'ftr_desktop_to_touch',
    },
];

async function makeContext(params, kadavrLayer, mandrelLayer) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            cookie,
            params,
        },
    });
}

export const checkMobileLinkInFooter = async (
    contextParams,
    apiaryLayer,
    kadavrLayer,
    mandrelLayer,
    neededMobileLink
) => {
    await makeContext(contextParams, kadavrLayer, mandrelLayer);
    await kadavrLayer.setState('Cataloger.stat', {
        baseAssemblyEnd: '2022-04-08 11:20:35',
        baseAssemblyStart: '2022-04-08 10:15:43.591472',
        baseId: '20220408_1015',
        baseReleaseComplete: '2022-04-08 11:26',
        offersCount: '1115506',
        shopCount: '237',
    });

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    const compareMobileLink = container.querySelector(`${FooterMarket.mobileLink}`);
    const mobileLinkHref = compareMobileLink.getAttribute('href');
    const {pathname: mobileLinkPathname, query: mobileLinkParams} = url.parse(
        mobileLinkHref,
        true
    );

    expect(compareMobileLink).toBeTruthy();
    expect(mobileLinkPathname).toContain(neededMobileLink);
    // // INFO: проверяем наличие query-параметров в ссылке и их соответствие ожидаемым
    EXPECTED_QUERY_PARAMS.forEach(item => {
        const {paramName, paramValue} = item;
        expect(mobileLinkParams)
            .toHaveProperty(paramName, String(paramValue));
    });
};
