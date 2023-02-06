import {screen} from '@testing-library/dom';

import {makeMirror} from '@self/platform/helpers/testament';
import {
    productMock,
    publicDisplayName,
    productGradesSchema,
    productCfg as reviewsRequestParams,
} from '@self/root/src/spec/testament/review/mocks';

// путь к виджету который тестируем
const WIDGET_PATH = '@self/platform/widgets/parts/ProductGrades';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

async function makeContext({pageId, requestParams}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            cookie,
            params: requestParams,
        },
        page: {
            pageId,
        },
    });
}

beforeAll(async () => {
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

afterAll(() => {
    mirror.destroy();
});

const expectedUserName = publicDisplayName;

describe('Widget: ProductGrades', () => {
    describe('Оценки - отзывы без текста. Имя пользователя.', () => {
        beforeEach(async () => {
            await kadavrLayer.setState('report', productMock);
            await makeContext({pageId: 'touch:product-reviews', requestParams: reviewsRequestParams});
        });
        describe('Отображаемое имя задано в паспорте.', () => {
            beforeEach(async () => {
                await kadavrLayer.setState('schema', productGradesSchema);
                await apiaryLayer.mountWidget(WIDGET_PATH);
            });
            describe('Имя пользователя в заголовке оценки.', () => {
                it('По умолчанию должно быть корректным.', async () => {
                    const userName = screen.getByRole('button', {name: expectedUserName});
                    expect(userName.textContent).toEqual(expectedUserName);
                });
            });
        });
    });
});
