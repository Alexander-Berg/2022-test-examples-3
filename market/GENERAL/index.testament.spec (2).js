import {screen} from '@testing-library/dom';
import {mockIntersectionObserver, mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {
    createSnippetWithShopState,
    SHOP_ID,
    SHOP_NAME,
    SHOP_LOGO,
    SHOP_SLUG,
    OFFER_ENCRYPTED_URL,
} from './__mocks__/snippetWithShop';
import {
    createSnippet,
    USER_PUBLIC_NAME,
} from './__mocks__/snippet';

const widgetPath = '../';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

async function makeContext(isAuth = false) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId(true)};

    return mandrelLayer.initContext({
        user: {
            isAuth,
        },
        request: {
            cookie,
        },
    });
}

beforeAll(async () => {
    mockIntersectionObserver();
    mockLocation();
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

afterAll(() => {
    mirror.destroy();
});

describe('Сниппет ответа магазина с CPC предложением.', () => {
    beforeEach(async () => {
        await makeContext();
        const {reportState, schemaState, data} = await createSnippetWithShopState(true);
        await kadavrLayer.setState('report', reportState);
        await kadavrLayer.setState('schema', schemaState);
        await kadavrLayer.setState('ShopInfo.collections', {shopNames: []});
        await apiaryLayer.mountWidget(widgetPath, {
            questionId: data.question.id,
            productId: data.product.id,
        });
    });
    test('По умолчанию оффер отображается', async () => {
        expect(screen.queryByTestId('offer-snippet')).toBeVisible();
    });
    test('По умолчанию ссылка в магазин корректная', async () => {
        const link = screen.getByRole('link', {name: /в магазин/i});
        expect(link).toHaveAttribute('href', expect.stringMatching(`external:clickdaemon_{'tld':'ru','url':'${OFFER_ENCRYPTED_URL}'`));
    });
});
describe('Сниппет ответа магазина', () => {
    beforeEach(async () => {
        await makeContext();
        const {reportState, schemaState, data} = await createSnippetWithShopState();
        await kadavrLayer.setState('report', reportState);
        await kadavrLayer.setState('schema', schemaState);
        await kadavrLayer.setState('ShopInfo.collections', {shopNames: []});
        await apiaryLayer.mountWidget(widgetPath, {
            questionId: data.question.id,
            productId: data.product.id,
        });
    });
    test('По умолчанию отображается галочка', async () => {
        const shopName = screen.getByTestId('shop-name');
        const icon = shopName.querySelector('svg');
        expect(icon).toBeVisible();
    });
    test('По умолчанию отображается дата публикации', async () => {
        const date = screen.getByTestId('publication-date');
        expect(date.textContent).toEqual('Год назад');
    });
    test('По умолчанию отображается корректное название магазина', async () => {
        const shopName = screen.getByTestId('shop-name').querySelector('a').textContent;
        expect(shopName).toEqual(SHOP_NAME);
    });
    test('По умолчанию отображается логотип', async () => {
        const avatar = screen.getByTestId('avatar').querySelector('img');
        expect(avatar.getAttribute('src')).toEqual(SHOP_LOGO);
    });
    test('По умолчанию ссылки в имени и логотипе ведут на страницу магазина', async () => {
        await step('Ссылка в имени ведёт на страницу магазина', async () => {
            const avatarLink = screen.getByTestId('avatar').querySelector('a');
            expect(avatarLink).toHaveAttribute('href', expect.stringMatching(`market:shop_{'shopId':${SHOP_ID},'slug':'${SHOP_SLUG}'}`));
        });
        await step('Ссылка в логотипе ведёт на страницу магазина', async () => {
            const shopNameLink = screen.getByTestId('shop-name').querySelector('a');
            expect(shopNameLink).toHaveAttribute('href', expect.stringMatching(`market:shop_{'shopId':${SHOP_ID},'slug':'${SHOP_SLUG}'}`));
        });
    });
    test('При отсутствии предложения оффер не отображается', async () => {
        expect(screen.queryByTestId('offer-snippet')).toBeNull();
    });
});
describe('Сниппет ответа', () => {
    beforeEach(async () => {
        await makeContext();
        const {reportState, schemaState, data} = await createSnippet();
        await kadavrLayer.setState('report', reportState);
        await kadavrLayer.setState('schema', schemaState);
        await kadavrLayer.setState('ShopInfo.collections', {shopNames: []});
        await apiaryLayer.mountWidget(widgetPath, {
            questionId: data.question.id,
            productId: data.product.id,
        });
    });
    describe('По умолчанию', () => {
        test('аватарка в сниппете отображается', async () => {
            const avatar = screen.getByTestId('avatar').querySelector('img');
            expect(avatar).toBeVisible();
        });
        test('дата публикации отображается', async () => {
            const date = screen.getByTestId('publication-date');
            expect(date.textContent).toEqual('Год назад');
        });
        test('имя пользователя отображается', async () => {
            const name = screen.getByTestId('user_name').textContent;
            expect(name).toContain(USER_PUBLIC_NAME);
        });
        test('сниппет отображается', async () => {
            const snippet = screen.getByTestId('answer');
            expect(snippet).toBeVisible();
        });
    });
});
