
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {waitFor} from '@testing-library/dom';

// page-objects
import CartJurInformation from '@self/root/src/widgets/content/cart/CartJurInformation/__pageObject';

const {mockScrollBy} = require('@self/root/src/helpers/testament/mock');

const WIDGET_PATH = '@self/root/src/widgets/content/cart/CartJurInformation';
const WIDGET_PARAMS = {isEda: true};
const JUR_TEXT = 'Оформление заказа партнером — Яндекс.Еда';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;


const makeContext = async () => {
    await mandrelLayer.initContext();
};

beforeAll(async () => {
    // $FlowFixMe<type of jest?>
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    jestLayer = mirror.getLayer('jest');

    mockScrollBy();

    await jestLayer.doMock(
        require.resolve('@self/root/src/utils/router'),
        () => ({buildUrl: () => 'test-link'})
    );
});

afterAll(() => {
    mirror.destroy();
});

describe('Юридическая информация', () => {
    describe('Авторизованный пользователь', () => {
        beforeEach(async () => {
            await makeContext({isAuth: true});
        });

        it('Для обычной корзины информация скрыта', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
            expect(container.querySelector(CartJurInformation.root)).toBeNull();
        });

        it('Для Ритейла юридическая информация корректна', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_PARAMS);
            expect(container.querySelector(CartJurInformation.root).textContent).toBe(JUR_TEXT);
        });

        it('Для Ритейла показывается иконка-вопросик, попап скрыт', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_PARAMS);

            expect(container.querySelector(CartJurInformation.icon)).toBeTruthy();
            expect(container.querySelector(CartJurInformation.jurPopup)).toBeNull();
        });

        it('Попап открываеся при нажатии на иконку-вопросик и закрывается по клику на крестик', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_PARAMS);
            expect(container.querySelector(CartJurInformation.root)).toBeTruthy();

            await step('Кликаем на иконку-вопросик', async () => {
                container.querySelector(CartJurInformation.icon).click();
            });

            await step('Попап появляется при нажатии', async () => {
                await waitFor(async () => {
                    expect(container.querySelector(CartJurInformation.jurPopup)).toBeVisible();
                    expect(container.querySelector(CartJurInformation.jurContent)).toBeVisible();
                }, {timeout: 1000});
            });

            await step('Кликаем на иконку закрытия попапа', async () => {
                const closeIcon = container.querySelector(CartJurInformation.closeIcon);
                expect(closeIcon).toBeTruthy();
                closeIcon.click();
            });

            await step('Попапа скрылся', async () => {
                await waitFor(() => {
                    expect(container.querySelector(CartJurInformation.jurContent)).toBeNull();
                });
            });
        });

        it('Попап содержит корректную ссылку на подробную информацию', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_PARAMS);
            expect(container.querySelector(CartJurInformation.root)).toBeTruthy();

            await step('Кликаем на иконку-вопросик', async () => {
                container.querySelector(CartJurInformation.icon).click();
            });

            await step('Попап появляется при нажатии', async () => {
                await waitFor(async () => {
                    expect(container.querySelector(CartJurInformation.jurPopup)).toBeVisible();
                    expect(container.querySelector(CartJurInformation.jurContent)).toBeVisible();
                }, {timeout: 1000});
            });

            await step('Проверяем ссылку', async () => {
                const link = container.querySelector(CartJurInformation.link);

                expect(link.textContent).toBe('Сборщики и доставщики');
                expect(link.getAttribute('href')).toBe('test-link');
            });
        });
    });
});
