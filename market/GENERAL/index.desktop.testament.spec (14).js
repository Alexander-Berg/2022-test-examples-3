import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {findByText, queryByText, screen} from '@testing-library/dom';
import {getRelativeDate} from '@self/root/src/helpers/datetime';
import {
    currentUser,
    productMock,
    schemaWithComments,
    schemaWithoutComments,
    userExpertise,
    reviewsOpinionsMock,
    schemaWithoutComments2,
} from './__mock__';

const widgetPath = '../';
const DATE_REGION = 'date_region';
const USER_NAME = 'user_name';
const PRODUCT_REVIEW_AVATAR = 'product_review_avatar';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

const testCommonButtonsPresence = () => {
    describe('По умолчанию', () => {
        test('должен содержать ссылку "Удалить"', async () => {
            await apiaryLayer.mountWidget(widgetPath, {
                listState: {
                    productId: 1,
                },
            });
            const reviewFooter = screen.getByTestId('product-review-footer');
            return expect(queryByText(reviewFooter, 'Удалить')).toBeTruthy();
        });
        test('должен содержать ссылку "Ответить"', async () => {
            await apiaryLayer.mountWidget(widgetPath, {
                listState: {
                    productId: 1,
                },
            });
            const reviewFooter = screen.getByTestId('product-review-footer');
            return expect(queryByText(reviewFooter, 'Комментировать')).toBeTruthy();
        });
    });
};

async function makeContext(user) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user,
        request: {
            cookie,
            params: {
                // Параметр который ждет контроллер
                productId: 1,
            },
        },
    });
}

beforeAll(async () => {
    mockIntersectionObserver();
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    window.scrollTo = () => {};

    await jestLayer.doMock(
        require.resolve('@self/project/src/utils/router'),
        () => ({
            buildUrl: () => '',
            buildURL: () => '',
        })
    );
});

afterAll(() => {
    mirror.destroy();
});
describe('Список отзывов', () => {
    // SKIPPED MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Авторизованный пользователь', () => {
        beforeAll(async () => {
            await jestLayer.doMock(
                require.resolve('@self/root/src/resources/persAuthor/expertise/fetchExpertiseDictionary'),
                () => ({fetchExpertiseDictionary: () => Promise.resolve({result: [],
                    collections: {expertise: {
                        9: {
                            entity: 'expertise',
                            // Айдишник из pers-author
                            id: 9,
                            // Название экспертизы
                            name: 'expertise name',
                            // Описание, например, департамент
                            description: 'expertise description',
                            // Массив уровней экспертизы
                            levels: [
                                {
                                // Номер уровня внутри одной экспертизы
                                    level: 2,
                                    // Звание, которым величают юзера, взявшего этот уровень
                                    label: 'level 2 label',
                                    // Величина, начиная с которой достигается уровень
                                    startValue: 0,
                                    // Величина, начиная с которой начинается следующий уровень
                                    endValue: Number.MAX_SAFE_INTEGER,
                                },
                            ],
                            // Набор иллюстраций
                            images: {},
                        },
                    },
                    }})}));
        });
        beforeEach(async () => {
            await makeContext({
                UID: currentUser.uid,
                yandexuid: currentUser.uid,
                publicId: currentUser.publicId,
            });
        });
        describe('Блок с отзывом от текущего пользователя', () => {
            describe('без комментариев.', () => {
                beforeEach(async () => {
                    await kadavrLayer.setState('schema', schemaWithoutComments);
                });
                describe('по умолчанию', () => {
                    test('должен содержать ссылку "Изменить"', async () => {
                        await apiaryLayer.mountWidget(widgetPath, {
                            listState: {
                                productId: 1,
                            },
                        });
                        const reviewFooter = screen.getByTestId('product-review-footer');
                        return expect(queryByText(reviewFooter, 'Изменить')).toBeTruthy();
                    });
                    testCommonButtonsPresence();
                });
            });
            describe('с комментарием.', () => {
                beforeEach(async () => {
                    await kadavrLayer.setState('schema', schemaWithComments);
                });
                describe('по умолчанию', () => {
                    test('не должен содержать ссылку "Изменить"', async () => {
                        await apiaryLayer.mountWidget(widgetPath, {
                            listState: {
                                productId: 1,
                            },
                        });
                        const reviewFooter = screen.getByTestId('product-review-footer');
                        return expect(queryByText(reviewFooter, 'Изменить')).toBeFalsy();
                    });
                    testCommonButtonsPresence();
                });
            });
        });
    });
    describe('Неавторизованный пользователь', () => {
        beforeEach(async () => {
            await makeContext({
                isAuth: false,
            });
            await kadavrLayer.setState('schema', schemaWithComments);
            await kadavrLayer.setState('report', productMock);
            await kadavrLayer.setState('storage', {userExpertise: [userExpertise]});
        });
        describe('Контейнер, в котором лежит аватарка и имя пользователя, который оставил комментарий', () => {
            test('По умолчанию отображается верное имя пользователя', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    listState: {
                        productId: 1,
                    },
                });
                const popupSnippet = screen.findAllByTestId('user-profile-popup-snippet')[0];
                return expect(findByText(popupSnippet, 'review-creator')).toBeTruthy();
            });
        });
    });
    describe('Отзывы без текста. Блок отзыва без текста.', () => {
        beforeAll(async () => {
            await kadavrLayer.setState('schema', schemaWithoutComments2);
            await kadavrLayer.setState('storage.modelOpinions', reviewsOpinionsMock);
        });
        beforeEach(async () => {
            await makeContext({isAuth: false});
        });
        it('должен содержать дату создания', async () => {
            await apiaryLayer.mountWidget(widgetPath, {
                listState: {
                    productId: 1,
                },
            });
            expect(screen.getByTestId(DATE_REGION)).toBeInTheDocument();
            // eslint-disable-next-line no-irregular-whitespace
            expect(screen.getByTestId(DATE_REGION).textContent).toEqual(`${getRelativeDate(1440871093000, new Date())}, Тествиль`);
        });
        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        it.skip('должен содержать профиль пользователя', async () => {
            await apiaryLayer.mountWidget(widgetPath, {
                listState: {
                    productId: 1,
                },
            });
            const userName = screen.findAllByTestId(USER_NAME)[0];
            expect(findByText(userName, 'review-creator')).toBeTruthy();
            expect(screen.getByTestId(PRODUCT_REVIEW_AVATAR)).toBeInTheDocument();
        });
    });
});
