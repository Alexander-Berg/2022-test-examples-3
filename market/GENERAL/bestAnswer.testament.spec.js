import {isEmpty} from 'ambar';

import {makeMirror} from '@self/platform/helpers/testament';

import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createProductQuestion} from '@yandex-market/kadavr/dist/helpers/entities/question/productQuestion';
import {createQuestionAnswer} from '@yandex-market/kadavr/dist/helpers/entities/questionAnswer';
import {createShopInfo} from '@yandex-market/kadavr/dist/helpers/entities/shopInfo';
import {createUser} from '@yandex-market/kadavr/dist/helpers/entities/user';
import {createVendor} from '@yandex-market/kadavr/dist/helpers/entities/vendor';

import BestAnswerPO from '@self/platform/components/Question/BestAnswer/__pageObject__';

import {PUBLIC_USER_COLLECTION_KEY} from '@self/root/src/entities/publicUser';
import {
    PRODUCT_ID, PRODUCT,
    knownVendorParams, unknownVendorParams,
    knownShopParams, unknownSimpleUserParams, unknownShopParams,
    knownSimpleUserParams, publicUser,
} from './__mocks__';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

const WIDGET_PATH = require.resolve('@self/platform/widgets/parts/ProductQuestionsLayout/QuestionList');
const WIDGET_OPTIONS = {productId: PRODUCT_ID};

async function makeContext(user = {}) {
    return mandrelLayer.initContext({
        request: {params: {page: 1}},
        user,
    });
}

beforeAll(async () => {
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await Promise.all([
        jestLayer.backend.doMock(require.resolve('@self/root/src/resolvers/expertise'),
            () => ({
                __esModule: true,
                resolveExpertiseDictionary: () => Promise.resolve({result: [], collections: {espertise: {}}}),
            })
        ),
        jestLayer.backend.doMock(require.resolve('@self/root/src/resolvers/experimentFlags'),
            () => ({
                __esModule: true,
                resolveExperimentFlagsSync: () => ({result: [], collections: {experimentFlag: {}}}),
            })
        ),
        jestLayer.backend.doMock(require.resolve('@self/project/src/resolvers/offer'),
            () => ({
                __esModule: true,
                resolveOfferByProductIdAndShopId: () => Promise.resolve({result: [], collections: {offer: {}}}),
            })
        ),
        jestLayer.backend.doMock(require.resolve('@self/platform/resolvers/user'),
            () => ({__esModule: true, getAuthUrlSync: () => ''})
        ),
        jestLayer.doMock(
            require.resolve('@self/project/src/utils/router'),
            () => ({buildUrl: () => '', buildURL: () => ''})
        )]);

    const {collections} = await createProduct(PRODUCT, PRODUCT_ID);
    const product = collections.product[PRODUCT_ID];
    await jestLayer.backend.runCode(mock => {
        jest.spyOn(require('@self/platform/resolvers/products'), 'getProductById').mockResolvedValue(mock);
    }, [product]);
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: QuestionList. Лучшие ответы', () => {
    const prepareResolverMocks = async params => {
        const {authorId, authorSlug, authorEntity, authorName, answerText} = params;

        let vendor = {};
        let shop = {};
        let publicUserCollection = {};
        let user = await createUser();
        if (authorId) {
            if (authorEntity === 'vendor') {
                vendor = await createVendor({name: authorName, id: Number(authorId)});
            }

            if (authorEntity === 'shop') {
                shop = await createShopInfo({id: Number(authorId), slug: authorSlug, shopName: authorName});
            }

            if (authorEntity === 'user') {
                publicUserCollection = {[authorId]: publicUser};
            }
        }
        const question = await createProductQuestion({votes: {}}, {user, product: {id: PRODUCT_ID}});
        const answer = await createQuestionAnswer({
            text: answerText,
            author: {
                id: authorId,
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
                user,
                pager: {count: 1},
            },
        };
    };

    describe.each([
        ['авторизован', {isAuth: true}],
        ['не авторизован', {}],
    ])('Пользователь %s.', (_, ctxParam) => {
        beforeEach(async () => {
            await makeContext(ctxParam);
        });
        describe.each([
            ['Вендор', knownVendorParams, unknownVendorParams, 'отображается у верифицированного автора', 'toBeTruthy'],
            ['Магазин', knownShopParams, unknownShopParams, 'отображается у верифицированного автора', 'toBeTruthy'],
            ['Простой пользователь', knownSimpleUserParams, unknownSimpleUserParams, 'не отображается у простого автора', 'toBeNull'],
        ])('Автор %s.', (testName, knownAuthor, unknownAuthor, badgeExpectedResult, badgeChecker) => {
            // eslint-disable-next-line jest/valid-describe
            describe(`${testName} известен.`, () => {
                beforeEach(async () => {
                    const {resolveAuthorAndReplyToMock, getQuestionsByProductIdMock} = await prepareResolverMocks(knownAuthor);

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
                test('Сниппет лучшего ответа отображается под вопросом', async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.root)).toBeTruthy();
                });
                test('Сниппет лучшего ответа не имеет тулбара', async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.toolbar)).toBeNull();
                });
                test('Сниппет лучшего ответа содержит дату ответа', async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.date).textContent).toBe('Только что');
                });
                test('Сниппет лучшего ответа содержит текст ответа', async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.text).textContent).toBe(knownAuthor.answerText);
                });
                test(`Бейдж верифицированного автора ${badgeExpectedResult}.`, async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.tooltip))[badgeChecker]();
                });
                test('Блок автора в сниппете лучшего ответа содержит аватар автора ответа.', async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.avatar)).toBeTruthy();
                });
                test('Блок автора в сниппете лучшего ответа содержит имя автора ответа.', async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.userName).textContent)
                        .toBe(knownAuthor.expectedAuthorName);
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
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.tooltip))[badgeChecker]();
                });
                test('Блок автора в сниппете лучшего ответа содержит аватар автора ответа.', async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.avatar)).toBeTruthy();
                });
                test('Блок автора в сниппете лучшего ответа содержит имя автора ответа.', async () => {
                    const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);
                    expect(container.querySelector(BestAnswerPO.userName).textContent)
                        .toBe(unknownAuthor.expectedAuthorName);
                });
            });
        });
    });
});
