import {isEmpty} from 'ambar';
import {screen, within} from '@testing-library/dom';

import {makeMirror} from '@self/platform/helpers/testament';

import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createProductQuestion} from '@yandex-market/kadavr/dist/helpers/entities/question/productQuestion';
import {createQuestionAnswer} from '@yandex-market/kadavr/dist/helpers/entities/questionAnswer';
import {createShopInfo} from '@yandex-market/kadavr/dist/helpers/entities/shopInfo';
import {createUser} from '@yandex-market/kadavr/dist/helpers/entities/user';
import {createVendor} from '@yandex-market/kadavr/dist/helpers/entities/vendor';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {PUBLIC_USER_COLLECTION_KEY} from '@self/root/src/entities/publicUser';
import {
    PRODUCT_ID, PRODUCT,
    knownVendorParams, unknownVendorParams,
    knownShopParams, unknownShopParams,
    unknownSimpleUserParams, knownSimpleUserParams,
    publicUser,
} from './__mock__';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

const WIDGET_PATH = require.resolve('@self/platform/widgets/parts/QuestionsAnswers/QuestionList');
const WIDGET_OPTIONS = {entityId: PRODUCT_ID, entity: 'product'};

async function makeContext(user) {
    return mandrelLayer.initContext({
        request: {params: {page: 1}},
        page: {pageId: 'touch:product-questions'},
        user,
    });
}

beforeAll(async () => {
    mockIntersectionObserver();
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {skipLayer: true},
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    const normalizedProduct = await createProduct(PRODUCT, PRODUCT_ID);
    const product = normalizedProduct.collections.product[PRODUCT_ID];

    await jestLayer.backend.runCode((
        userResolvers,
        contextResolvers,
        expertiseResolvers,
        experimentFlagsResolver,
        offerResolvers,
        requestResolvers,
        productResolvers,
        normalizedProductMock,
        rawProductResolvers,
        productMock,
        widgetMock
    ) => {
        jest.doMock(userResolvers, () => ({
            __esModule: true,
            resolveAuthUrlSync: () => '',
            resolveCurrentUserNormalizedSync: () => ({collections: {currentUser: {}}}),
            resolveCurrentUserDenormalizedSync: () => ({}),
            resolveSessionSync: () => ({isAuth: false}),
        }));
        jest.doMock(contextResolvers, () => ({
            __esModule: true,
            createResolverContext: () => ({user: {isAuth: () => false}}),
        }));
        jest.doMock(expertiseResolvers, () => ({
            __esModule: true,
            resolveExpertiseDictionary: () => Promise.resolve({result: [], collections: {espertise: {}}}),
        }));
        jest.doMock(experimentFlagsResolver, () => ({
            resolveExperimentFlagsSync: () => ({result: [], collections: {experimentFlag: {}}}),
        }));
        jest.doMock(offerResolvers, () => ({
            __esModule: true,
            resolveOfferByProductIdAndShopId: () => Promise.resolve({result: [], collections: {offer: {}}}),
        }));
        jest.doMock(requestResolvers, () => ({
            __esModule: true,
            resolveUserTimezoneOffset: () => Promise.resolve(0),
        }));
        jest.doMock(productResolvers, () => ({
            resolveProductsByIds: () => Promise.resolve(normalizedProductMock),
        }));
        jest.doMock(rawProductResolvers, () => ({
            __esModule: true,
            resolveRawProductsByIdOnly: () => Promise.resolve([productMock]),
        }));
        jest.doMock(widgetMock, () => ({
            create: () => Promise.resolve(null),
        }));
    }, [
        require.resolve('@self/root/src/resolvers/user'),
        require.resolve('@self/platform/app/node_modules/entities/resolver-context'),
        require.resolve('@self/root/src/resolvers/expertise'),
        require.resolve('@self/root/src/resolvers/experimentFlags'),
        require.resolve('@self/project/src/resolvers/offer'),
        require.resolve('@self/project/src/resolvers/request'),
        require.resolve('@self/project/src/resolvers/product/resolveProductsByIds'),
        normalizedProduct,
        require.resolve('@self/project/src/resolvers/product/resolveRawProductsById'),
        product,
        require.resolve('@self/platform/widgets/content/HighratedSimilarProducts'),
    ]);
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: QuestionList. Лучшие ответы', () => {
    const prepareResolverMocks = async params => {
        const {authorId, authorSlug, authorEntity, authorName, answerText, authorAvatar} = params;

        let vendor = {};
        let shop = {};
        let user = {};
        let publicUserCollection = {};
        if (authorId) {
            if (authorEntity === 'vendor') {
                vendor = await createVendor({name: authorName, id: Number(authorId), logo: authorAvatar});
            }

            if (authorEntity === 'shop') {
                shop = await createShopInfo({id: Number(authorId), slug: authorSlug, shopName: authorName, logo: {url: authorAvatar}});
            }

            if (authorEntity === 'user') {
                user = await createUser({id: authorId, display_name: {name: authorName}, uid: authorId}); // tyt
                publicUserCollection = {[authorId]: publicUser};
            }
        }

        const questionAuthor = await createUser();
        const question = await createProductQuestion({votes: {}}, {user: questionAuthor, product: {id: PRODUCT_ID}});
        const answer = await createQuestionAnswer({
            text: answerText,
            author: {
                id: Number(authorId),
                entity: authorEntity,
                slug: authorSlug,
            },
        }, {question});

        question.answers = [answer];
        question.answersCount = 1;

        return {
            resolveAuthorAndReplyToMock: {
                result: {},
                collections: {
                    vendor: !isEmpty(vendor) ? {[vendor.id]: vendor} : {},
                    shop: !isEmpty(shop) ? {[shop.id]: shop} : {},
                    user: !isEmpty(user) ? {[user.id]: user} : {},
                    [PUBLIC_USER_COLLECTION_KEY]: publicUserCollection,
                },
            },
            getQuestionsByProductIdMock: {
                data: [question],
                answer,
                answerIds: [answer.id],
                shop,
                vendor,
                user: questionAuthor,
                pager: {count: 1},
            },
        };
    };

    describe.each([
        ['авторизован', {isAuth: true}],
        ['не авторизован'],
    ])('Пользователь %s.', (_, ctxParam) => {
        beforeEach(async () => {
            await makeContext(ctxParam);
        });
        describe.each([
            ['Вендор', knownVendorParams, unknownVendorParams, 'отображается у верифицированного автора', 'verified-icon', true],
            ['Магазин', knownShopParams, unknownShopParams, 'отображается у верифицированного автора', 'verified-icon', true],
            ['Простой пользователь', knownSimpleUserParams, unknownSimpleUserParams, 'не отображается у простого автора', 'user-info-icon', false],
        ])('Автор %s.', (testName, knownAuthor, unknownAuthor, badgeExpectedResult, iconLabel, isBadgeInDocument) => {
            // eslint-disable-next-line jest/valid-describe
            describe(`${testName} известен.`, () => {
                beforeEach(async () => {
                    const {resolveAuthorAndReplyToMock, getQuestionsByProductIdMock} = await prepareResolverMocks(knownAuthor);

                    await jestLayer.backend.runCode(mock => {
                        jest.spyOn(require('@self/root/src/resolvers/author'), 'resolveAuthorAndReplyTo').mockResolvedValue(mock);
                    }, [resolveAuthorAndReplyToMock]);

                    await jestLayer.backend.runCode(resourceMock => {
                        // flowlint-next-line untyped-import: off
                        require('@self/project/src/spec/unit/mocks/yandex-market/mandrel/resolver');
                        const {unsafeResource} = require('@yandex-market/mandrel/resolver');
                        const {createUnsafeResourceMockImplementation} = require('@self/root/src/spec/unit/mocks/resolvers/helpers');
                        unsafeResource.mockImplementation(
                            createUnsafeResourceMockImplementation({
                                'questionsAnswers.getQuestionsByProductId': () => Promise.resolve(resourceMock),
                            })
                        );
                    }, [getQuestionsByProductIdMock]);
                });
                test('Сниппет лучшего ответа отображается под вопросом', async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(screen.getByTestId('best-answer')).toBeInTheDocument();
                });
                test('Сниппет лучшего ответа содержит текст ответа', async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(screen.getAllByRole('button', {name: knownAuthor.answerText})).not.toBeNull();
                });
                test('Футер сниппета ответа не отображается', async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(screen.queryByRole(/Комментировать/i)).not.toBeInTheDocument();
                    expect(screen.queryByLabelText(/Нравится/i)).not.toBeInTheDocument();
                    expect(screen.queryByLabelText(/Не нравится/i)).not.toBeInTheDocument();
                });
                test('Сниппет лучшего ответа содержит дату ответа', async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(screen.getByText(/Сегодня/i)).toBeInTheDocument();
                });
                test('Блок автора в сниппете лучшего ответа содержит аватар автора ответа.', async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    const alt = new RegExp(knownAuthor.expectedAuthorName, 'i');
                    expect(screen.getByRole('img', {alt})).toBeInTheDocument();
                });
                test('Блок автора в сниппете лучшего ответа содержит имя автора ответа.', async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(screen.getByText(knownAuthor.expectedAuthorName)).toBeInTheDocument();
                });
                test(`Бейдж верифицированного автора ${badgeExpectedResult}.`, async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    const badge = await screen.queryByTestId(iconLabel);
                    if (isBadgeInDocument) {
                        expect(badge).toBeInTheDocument();
                    } else {
                        expect(badge).not.toBeInTheDocument();
                    }
                });
            });

            // eslint-disable-next-line jest/valid-describe
            describe(`${testName} неизвестен.`, () => {
                beforeEach(async () => {
                    const {resolveAuthorAndReplyToMock, getQuestionsByProductIdMock} = await prepareResolverMocks(unknownAuthor);

                    await jestLayer.backend.runCode(mock => {
                        jest.spyOn(require('@self/root/src/resolvers/author'), 'resolveAuthorAndReplyTo').mockResolvedValue(mock);
                    }, [resolveAuthorAndReplyToMock]);

                    await jestLayer.backend.runCode(mock => {
                        // flowlint-next-line untyped-import: off
                        require('@self/project/src/spec/unit/mocks/yandex-market/mandrel/resolver');
                        const {unsafeResource} = require('@yandex-market/mandrel/resolver');
                        const {createUnsafeResourceMockImplementation} = require('@self/root/src/spec/unit/mocks/resolvers/helpers');
                        unsafeResource.mockImplementation(
                            createUnsafeResourceMockImplementation({
                                'questionsAnswers.getQuestionsByProductId': () => Promise.resolve(mock),
                            })
                        );
                    }, [getQuestionsByProductIdMock]);
                });
                test(`Бейдж верифицированного автора ${badgeExpectedResult}.`, async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    const badge = await screen.queryByTestId(iconLabel);
                    if (isBadgeInDocument) {
                        expect(badge).toBeInTheDocument();
                    } else {
                        expect(badge).not.toBeInTheDocument();
                    }
                });
                test('Блок автора в сниппете лучшего ответа содержит аватар автора ответа.', async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(screen.getByTestId('empty-avatar')).toBeInTheDocument();
                });
                test('Блок автора в сниппете лучшего ответа содержит имя автора ответа.', async () => {
                    await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    const bestAnswer = screen.getByTestId('best-answer');
                    const authorName = within(bestAnswer).getByText(unknownAuthor.expectedAuthorName);
                    expect(authorName).toBeInTheDocument();
                });
            });
        });
    });
});
