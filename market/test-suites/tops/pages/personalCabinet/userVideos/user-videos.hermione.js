import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';

// suites
import RemovableSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/UserVideos/removableSnippet';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import UserVideos from '@self/platform/widgets/content/UserVideos/__pageObject';
import UserVideo from '@self/platform/components/UserVideo/__pageObject';

const USER_PROFILE_CONFIG = profiles.ugctest3;
const DEFAULT_USER = createUser({
    id: USER_PROFILE_CONFIG.uid,
    uid: {
        value: USER_PROFILE_CONFIG.uid,
    },
    login: USER_PROFILE_CONFIG.login,
    display_name: {
        name: 'Willy Wonka',
        public_name: 'Willy W.',
        avatar: {
            default: '61207/462703116-1544492602',
            empty: false,
        },
    },
    dbfields: {
        'userinfo.firstname.uid': 'Willy',
        'userinfo.lastname.uid': 'Wonka',
    },
});

const DEFAULT_VOTES_COUNT = 10;
const DEFAULT_USER_VOTE = 0;

const DEFAULT_VIDEO_ID = 222;

const DEFAULT_PRODUCT = {
    id: 14206682, // id продукта совпадает с id productWithPicture,
    slug: 'smartfon-apple-iphone-7-128gb', // slug продукта совпадает с slug productWithPicture
    entity: 'product',
};

const createVideo = (params = {}) => {
    const {id, productId, moderationState} = params;

    return ({
        id,
        entity: 'ugcvideo',
        videoId: `video-${id}`,
        product: productId ? {id: productId, entity: 'product'} : null,
        autor: {
            id: DEFAULT_USER.id,
            entity: 'user',
        },
        title: `видео к товару ${productId}`,
        meta_info: {
            duration_ms: 141,
            height: 798,
            player_url: 'https://frontend.vh.yandex.ru/player/2788537994053022062',
            stream_url: '',
            thumbnail: '//avatars.mds.yandex.net/get-vh/175796/2788537994053022062-GW4yz9nZ3U8lMoIEUlrOHA/orig',
            width: 1920,
        },
        mod_state: moderationState,
        created: Date.now(),
    });
};

const prepareUserVideosTabPage = async (ctx, params = {}) => {
    const {
        videosCount = 1,
        productId = DEFAULT_PRODUCT.id,
        moderationState = 'APPROVED',
    } = params;

    let videos = [];
    let videoVotes = [];
    if (videosCount > 0) {
        videos = new Array(videosCount).fill(null).map((_, index) => createVideo({
            id: DEFAULT_VIDEO_ID + index,
            productId,
            moderationState,
        }));
        videoVotes = videos.map(video => ({
            id: video.id,
            votes: {
                dislikeCount: DEFAULT_VOTES_COUNT,
                likeCount: DEFAULT_VOTES_COUNT,
                userVote: DEFAULT_USER_VOTE,
            },
        }));
    }

    await ctx.browser.setState('report', productWithPicture);
    await ctx.browser.setState('schema', {
        users: [DEFAULT_USER],
        ugcvideo: videos,
    });
    await ctx.browser.setState('storage', {videoVotes});

    await ctx.browser.yaProfile('ugctest3', 'market:my-videos');
    return ctx.browser.yaClosePopup(ctx.createPageObject(RegionPopup));
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница личного кабинета. Вкладка с видео пользователя.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-9168',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    userVideos: () => this.createPageObject(UserVideos),
                    userVideo: () => this.createPageObject(UserVideo),
                });
            },
            afterEach() {
                return this.browser.yaLogout();
            },
        },
        {
            'Опубликованное видео.': mergeSuites(
                {
                    async beforeEach() {
                        await this.setPageObjects({
                            userVideos: () => this.createPageObject(UserVideos),
                            userVideo: () => this.createPageObject(UserVideo),
                        });

                        await prepareUserVideosTabPage(this);
                    },
                },
                prepareSuite(RemovableSnippetSuite)
            ),
        },
        {
            'Видео, не прошедшее модерацию': mergeSuites(
                {
                    async beforeEach() {
                        await prepareUserVideosTabPage(this, {
                            moderationState: 'REJECTED',
                        });
                        this.setPageObjects({
                            userVideos: () => this.createPageObject(UserVideos),
                            userVideo: () => this.createPageObject(UserVideo),
                        });
                    },
                },
                prepareSuite(RemovableSnippetSuite)
            ),
            'Видео, ожидающее модерацию': mergeSuites(
                {
                    async beforeEach() {
                        await prepareUserVideosTabPage(this, {
                            moderationState: 'NEW',
                        });
                        this.setPageObjects({
                            userVideos: () => this.createPageObject(UserVideos),
                            userVideo: () => this.createPageObject(UserVideo),
                        });
                    },
                },
                prepareSuite(RemovableSnippetSuite)
            ),
        }
    ),
});
