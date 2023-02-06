import {
    waitFor,
    fireEvent,
    queryByRole,
    getByRole,
} from '@testing-library/dom';

import {makeMirrorTouch as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {buildUrl} from '@self/root/src/utils/router';
import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds/pageIds';
import * as ugcVideoAction from '@self/project/src/actions/ugcVideo';

import WidgetPageObject from '@self/platform/widgets/content/UserVideos/__pageObject';
import UserVideoPageObject from '@self/platform/components/UserVideo/__pageObject';
import FooterPageObject from '@self/platform/components/UserVideo/Footer/__pageObject';
import VotesPageObject from '@self/platform/components/Votes/__pageObject';
import StatusHeadlinePO from '@self/platform/components/UserVideo/StatusHeadline/__pageObject';
import InfoFooterPO from '@self/platform/components/UserVideo/InfoFooter/__pageObject';

import {productMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

import {
    UID,
    mockResolveVideosByUid,
    mockResolveAddVideoVote,
    mockResolveRemoveVideoVote,
    mockCreateCurrentUserCollection,
    mockPublicUserInfo,
    TEN_LESS_VIDEOS_COUNT,
    TEN_MORE_VIDEOS_COUNT,
    EXPECTED_TEXT,
    PRODUCT_ID,
    SLUG,
    VIDEO_ID,
    VOTE_COUNT_INITIAL,
    VIEWS_COUNT,
} from './__mocks__';
import {prepareUserVideosTabPage} from './helpers';

const WIDGET_OPTIONS = {
    userUid: UID,
};

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

async function makeContext() {
    return mandrelLayer.initContext({});
}
describe('Widget: UserVideos', () => {
    beforeAll(async () => {
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        jestLayer = mirror.getLayer('jest');
        apiaryLayer = mirror.getLayer('apiary');
    });

    beforeEach(async () => {
        await makeContext();
    });

    afterAll(() => {
        mirror.destroy();
    });

    const setState = async (params, pagerParams = {}) => {
        const {
            product,
            videos,
        } = prepareUserVideosTabPage(params);
        const mockVideos = {
            data: videos,
            pager: {
                pageNum: 1,
                pageSize: 10,
                count: 1,
                total: 1,
                totalPageCount: 1,
                ...pagerParams,
            },
        };

        const mockProduct = {
            search: {
                results: [product],
            },
        };

        await jestLayer.backend.runCode((mockPublicUserInfo, mockVideos, mockProduct) => {
            const {cleanUpMocks, saveToCleanUp} = require('@self/root/src/helpers/testament/mockCleanUping');
            const {mockResource} = require('@self/root/src/helpers/testament/mock');
            cleanUpMocks('mockVideos');
            saveToCleanUp(
                'mockVideos',
                mockResource('@self/root/src/resources/persAuthor', 'fetchVideosByUid', () => mockVideos)
            );

            jest.spyOn(require('@self/root/src/resources/report'), 'fetchProductsByIds')
                .mockReturnValue(Promise.resolve(mockProduct));
            jest.spyOn(require('@self/root/src/resolvers/publicUser'), 'resolvePublicUsersInfo')
                .mockReturnValue(Promise.resolve(mockPublicUserInfo));
        }, [mockPublicUserInfo, mockVideos, mockProduct]);
    };

    describe('Zero стейт страницы.', () => {
        beforeAll(async () => {
            await setState({videosCount: 0});
        });
        test('Если сниппеты видео отсутствуют, по умолчанию отображается', async () => {
            const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
            const zeroState = container.querySelector(WidgetPageObject.zeroState);

            await step('Zero стейт страницы отображается', async () => {
                expect(zeroState).toBeVisible();
            });
        });
    });

    describe('Если видео меньше 10.', () => {
        beforeAll(async () => {
            await setState({videosCount: TEN_LESS_VIDEOS_COUNT}, {totalPageCount: 0});
        });
        describe('По умолчанию', () => {
            test('кнопка "Показать еще" не отображается', async () => {
                const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                const loadMoreButton = queryByRole(container, 'button', {
                    name: /показать ещё/i,
                });

                expect(loadMoreButton).toBeNull();
            });

            test('отображается верное количество видео', async () => {
                const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                const rootElement = container.querySelector(WidgetPageObject.root);
                const userVideos = rootElement.querySelectorAll(UserVideoPageObject.root);

                expect(userVideos.length).toBe(TEN_LESS_VIDEOS_COUNT);
            });
        });
    });

    describe('Если видео больше 10.', () => {
        beforeAll(async () => {
            await setState({videosCount: TEN_MORE_VIDEOS_COUNT}, {totalPageCount: 2});
        });
        describe('По умолчанию', () => {
            test('кнопка "Показать еще" отображается', async () => {
                const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                const loadMoreButton = getByRole(container, 'button', {
                    name: /показать ещё/i,
                });

                expect(loadMoreButton).toBeVisible();
            });

            test('отображается верное количество видео', async () => {
                const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                const rootElement = container.querySelector(WidgetPageObject.root);
                const userVideos = rootElement.querySelectorAll(UserVideoPageObject.root);

                expect(userVideos.length).toBe(TEN_MORE_VIDEOS_COUNT);
            });
        });
    });

    describe.each([
        ['Видео, не прошедшее модерацию.', {moderationState: 'REJECTED'}, WIDGET_OPTIONS],
        ['Видео, ожидающее модерацию.', {moderationState: 'NEW'}, WIDGET_OPTIONS],
    ])('%s', (_, stateParams) => {
        beforeAll(async () => {
            await setState(stateParams);
        });
        describe('Контент сниппета видео.', () => {
            describe('По умолчанию', () => {
                test('содержит верную ссылку на товар', async () => {
                    const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                    const productLink = container.querySelector(UserVideoPageObject.productLink);

                    expect(productLink.getAttribute('href')).toBe(
                        buildUrl(PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT, {
                            slug: productMock.slug,
                            productId: productMock.id,
                        })
                    );
                });
            });
        });
    });

    describe.each([
        ['Видео, не прошедшее модерацию.', {moderationState: 'REJECTED'}, EXPECTED_TEXT.REJECTED],
        ['Видео, ожидающее модерацию.', {moderationState: 'NEW'}, EXPECTED_TEXT.NEW],
        ['Неопубликованное видео на удаленный товар.', {moderationState: 'NEW', productId: null}, EXPECTED_TEXT.NEW_DELETED],
    ])('%s', (_, stateParams, expectedText) => {
        beforeAll(async () => {
            await setState(stateParams);
        });
        describe('Плашка статуса.', () => {
            describe('По умолчанию', () => {
                test('отображается', async () => {
                    const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                    const statusHeadline = container.querySelector(StatusHeadlinePO.root);

                    expect(statusHeadline).toBeVisible();
                });

                test('содержит верный текст', async () => {
                    const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                    const statusText = container.querySelector(StatusHeadlinePO.statusText);

                    expect(statusText.textContent).toBe(expectedText.status);
                });
            });
        });
    });

    describe.each([
        ['Видео, не прошедшее модерацию.', {moderationState: 'REJECTED'}, EXPECTED_TEXT.REJECTED],
        ['Неопубликованное видео на удаленный товар.', {moderationState: 'NEW', productId: null}, EXPECTED_TEXT.NEW_DELETED],
    ])('%s', (_, stateParams, expectedText) => {
        beforeAll(async () => {
            await setState(stateParams);
        });
        describe('Информационный футер видео.', () => {
            describe('По умолчанию', () => {
                test('отображается', async () => {
                    const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                    const infoFooter = container.querySelector(InfoFooterPO.root);

                    expect(infoFooter).toBeVisible();
                });

                test('содержит верный текст', async () => {
                    const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                    const footerText = container.querySelector(InfoFooterPO.footerText);

                    expect(footerText.textContent).toBe(expectedText.footer);
                });
            });
        });
    });
    // SKIPPED MARKETFRONT-96354
    // весь пак тестов падает в 4.8%
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Опубликованное видео', () => {
        const WIDGET_OPTIONS = {
            userUid: UID,
            isPublic: true,
        };
        const VOTE_BUTTON_COUNT = 2;

        beforeAll(async () => {
            await jestLayer.backend.runCode(mockResolveVideosByUid => {
                jest.spyOn(require('@self/root/src/resolvers/ugcVideo/resolveVideosByUid'), 'resolveVideosByUid')
                    .mockReturnValue(Promise.resolve(mockResolveVideosByUid));
            }, [mockResolveVideosByUid]);

            await jestLayer.backend.runCode(mockCreateCurrentUserCollection => {
                jest.spyOn(require('@self/root/src/entities/currentUser/helpers'), 'createCurrentUserCollection')
                    .mockReturnValue(Promise.resolve(mockCreateCurrentUserCollection));
            }, [mockCreateCurrentUserCollection]);

            await jestLayer.backend.runCode(mockResolveAddVideoVote => {
                jest.spyOn(require('@self/root/src/resolvers/ugcVideo/addVideoVote'), 'addVideoVote')
                    .mockReturnValue(Promise.resolve(mockResolveAddVideoVote));
            }, [mockResolveAddVideoVote]);

            await jestLayer.backend.runCode(mockResolveRemoveVideoVote => {
                jest.spyOn(require('@self/root/src/resolvers/ugcVideo/removeVideoVote'), 'removeVideoVote')
                    .mockReturnValue(Promise.resolve(mockResolveRemoveVideoVote));
            }, [mockResolveRemoveVideoVote]);

            await jestLayer.backend.runCode(
                (
                    mockResolveVideosByUid,
                    mockCreateCurrentUserCollection,
                    mockResolveAddVideoVote,
                    mockResolveRemoveVideoVote
                ) => {
                    jest.spyOn(require('@self/root/src/resolvers/ugcVideo/resolveVideosByUid'), 'resolveVideosByUid')
                        .mockReturnValue(Promise.resolve(mockResolveVideosByUid));

                    jest.spyOn(require('@self/root/src/entities/currentUser/helpers'), 'createCurrentUserCollection')
                        .mockReturnValue(Promise.resolve(mockCreateCurrentUserCollection));

                    jest.spyOn(require('@self/root/src/resolvers/ugcVideo/addVideoVote'), 'addVideoVote')
                        .mockReturnValue(Promise.resolve(mockResolveAddVideoVote));

                    jest.spyOn(require('@self/root/src/resolvers/ugcVideo/removeVideoVote'), 'removeVideoVote')
                        .mockReturnValue(Promise.resolve(mockResolveRemoveVideoVote));
                }, [
                    mockResolveVideosByUid,
                    mockCreateCurrentUserCollection,
                    mockResolveAddVideoVote,
                    mockResolveRemoveVideoVote,
                ]);
        });

        test('Футер отображается', async () => {
            const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
            const rootElement = container.querySelector(WidgetPageObject.root);
            const footerElement = rootElement.querySelector(UserVideoPageObject.footer);

            return expect(footerElement).toBeVisible();
        });

        test('Лайки и дизлайки отображаются', async () => {
            const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
            const rootElement = container.querySelector(WidgetPageObject.root);
            const voteButtonElements = rootElement.querySelectorAll(VotesPageObject.button);

            expect(voteButtonElements.length).toEqual(VOTE_BUTTON_COUNT);
            voteButtonElements.forEach(voteButtonElement =>
                expect(voteButtonElement).toBeVisible()
            );
        });

        test('При двойном клике на кнопку лайка количество лайков сначала увеличивается, а потом возвращается', async () => {
            const ugcVideoVoteUpSuccessSpy = jest.spyOn(ugcVideoAction, 'ugcVideoVoteUpSuccess');
            const removeUgcVideoVoteSuccessSpy = jest.spyOn(ugcVideoAction, 'removeUgcVideoVoteSuccess');

            const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
            const rootElement = container.querySelector(WidgetPageObject.root);
            const voteButtonElements = rootElement.querySelectorAll(VotesPageObject.button);
            expect(voteButtonElements.length).toEqual(VOTE_BUTTON_COUNT);

            const likeButtonElement = voteButtonElements[0];
            const likeCountBeforeClick = Number(likeButtonElement.textContent);
            expect(likeCountBeforeClick).toEqual(VOTE_COUNT_INITIAL);

            fireEvent.click(likeButtonElement);

            await waitFor(() => {
                expect(ugcVideoVoteUpSuccessSpy).toHaveBeenCalled();
            });

            const likeCountAfterFirstClick = Number(likeButtonElement.textContent);

            expect(likeCountAfterFirstClick).toEqual(VOTE_COUNT_INITIAL + 1);

            fireEvent.click(likeButtonElement);

            await waitFor(() => {
                expect(removeUgcVideoVoteSuccessSpy).toHaveBeenCalled();
            });

            const likeCountAfterSecondClick = Number(likeButtonElement.textContent);

            expect(likeCountAfterSecondClick).toEqual(VOTE_COUNT_INITIAL);

            ugcVideoVoteUpSuccessSpy.mockRestore();
            removeUgcVideoVoteSuccessSpy.mockRestore();
        });

        test('При двойном клике на кнопку дизлайка количество дизлайков сначала увеличивается, а потом возвращается', async () => {
            const ugcVideoVoteDownSuccessSpy = jest.spyOn(ugcVideoAction, 'ugcVideoVoteDownSuccess');
            const removeUgcVideoVoteSuccessSpy = jest.spyOn(ugcVideoAction, 'removeUgcVideoVoteSuccess');

            const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
            const rootElement = container.querySelector(WidgetPageObject.root);
            const voteButtonElements = rootElement.querySelectorAll(VotesPageObject.button);
            expect(voteButtonElements.length).toEqual(VOTE_BUTTON_COUNT);

            const dislikeButtonElement = voteButtonElements[1];
            const dislikeCountBeforeClick = Number(dislikeButtonElement.textContent);
            expect(dislikeCountBeforeClick).toEqual(VOTE_COUNT_INITIAL);

            fireEvent.click(dislikeButtonElement);

            await waitFor(() => {
                expect(ugcVideoVoteDownSuccessSpy).toHaveBeenCalled();
            });

            const dislikeCountAfterFirstClick = Number(dislikeButtonElement.textContent);

            expect(dislikeCountAfterFirstClick).toEqual(VOTE_COUNT_INITIAL + 1);

            fireEvent.click(dislikeButtonElement);

            await waitFor(() => {
                expect(removeUgcVideoVoteSuccessSpy).toHaveBeenCalled();
            });

            const dislikeCountAfterSecondClick = Number(dislikeButtonElement.textContent);

            expect(dislikeCountAfterSecondClick).toEqual(VOTE_COUNT_INITIAL);

            ugcVideoVoteDownSuccessSpy.mockRestore();
            removeUgcVideoVoteSuccessSpy.mockRestore();
        });

        test('Количество просмотров отображается корректно', async () => {
            const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
            const rootElement = container.querySelector(WidgetPageObject.root);
            const viewsCountElement = rootElement.querySelector(`${UserVideoPageObject.footer} ${FooterPageObject.viewsCount}`);

            const viewsCount = Number(viewsCountElement.textContent);

            expect(viewsCount).toEqual(VIEWS_COUNT);
        });

        test('Кнопка "Комментировать" отображается', async () => {
            const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
            const rootElement = container.querySelector(WidgetPageObject.root);
            const commentsLinkElement = rootElement.querySelector(`${UserVideoPageObject.footer} ${FooterPageObject.commentsLink}`);
            expect(commentsLinkElement).toBeVisible();
        });

        test('Кнопка "Комментировать" ведет на страницу видео', async () => {
            const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
            const rootElement = container.querySelector(WidgetPageObject.root);
            const commentsLinkElement = rootElement.querySelector(`${UserVideoPageObject.footer} a`);

            expect(commentsLinkElement.getAttribute('href')).toBe(
                buildUrl(PAGE_IDS_TOUCH.PRODUCT_VIDEO, {
                    productId: PRODUCT_ID,
                    slug: SLUG,
                    videoId: VIDEO_ID,
                })
            );
        });

        describe('Контент сниппета видео.', () => {
            describe('По умолчанию', () => {
                test('содержит верную ссылку на товар', async () => {
                    const {container} = await apiaryLayer.mountWidget('..', WIDGET_OPTIONS);
                    const productLink = container.querySelector(UserVideoPageObject.productLink);

                    expect(productLink.getAttribute('href')).toBe(
                        buildUrl(PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT, {
                            slug: SLUG,
                            productId: PRODUCT_ID,
                        })
                    );
                });
            });
        });
    });
});
