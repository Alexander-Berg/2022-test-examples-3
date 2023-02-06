import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {DEFAULT_VIDEO_ID, createUserProductVideos} from '@self/platform/spec/hermione/fixtures/ugcvideo';
// suites
import BreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/has-all-breadcrumbs';
import ContentUserInfoSuite from '@self/platform/spec/hermione/test-suites/blocks/components/ContentUserInfo';
import ProductHorizontalSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductHorizontal';
import CommentFormSuite from '@self/platform/spec/hermione/test-suites/blocks/CommentForm';
import CommentListSuite from '@self/platform/spec/hermione/test-suites/blocks/CommentList';
import UgcVideoSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcVideo';
// page-objects
import Breadcrumbs from '@self/platform/components/Breadcrumbs/__pageObject';
import ProductHorizontal from '@self/platform/spec/page-objects/components/ProductHorizontal';
import BigForm from '@self/platform/spec/page-objects/components/Comment/BigForm';
import CommentList from '@self/platform/spec/page-objects/components/Comment/List';
import UgcVideoPlayer from '@self/platform/components/UgcVideoPlayer/__pageObject';
import UgcVideoPlayerFooter from '@self/platform/widgets/content/UgcVideo/components/UgcVideoFooter/__pageObject';

const productId = 14236972;
const productSlug = 'random-fake-slug';
const product = createProduct({
    type: 'model',
    titles: {
        raw: 'Chateau d\'Armailhac',
    },
    categories: [{
        entity: 'category',
        fullName: 'Вино',
        isLeaf: true,
        kinds: [
            'alco',
        ],
        name: 'Вино',
        nid: 82914,
        type: 'gurulight',
    }],
    slug: productSlug,
    deletedId: null,
}, productId);

function newComment(props = {}) {
    return {
        id: 1234,
        state: 'NEW',
        entity: 'ugcvideo',
        entityId: DEFAULT_VIDEO_ID,
        votes: {
            dislikeCount: 0,
            likeCount: 0,
            userVote: 0,
        },
        ...props,
    };
}

const comments = [{id: 1234}, {id: 1235}, {id: 1236}, {id: 1237}].map(newComment);

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница комментирования видео о товаре.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-21620',
    story: mergeSuites(
        {
            async beforeEach() {
                const testUser = profiles['pan-topinambur'];

                const currentUser = createUser({
                    id: testUser.uid,
                    uid: {
                        value: testUser.uid,
                    },
                    login: testUser.login,
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
                    public_id: testUser.publicId,
                });

                const {videos, videoVotes} = createUserProductVideos({
                    userId: testUser.uid,
                    productId,
                });

                await this.browser
                    .setState('report', product)
                    .setState('schema', {
                        users: [currentUser],
                        ugcvideo: videos,
                        commentary: comments,
                    });
                await this.browser.setState('storage', {videoVotes});

                await this.browser.yaLogin(
                    testUser.login,
                    testUser.password
                );

                await this.browser.yaOpenPage('market:product-video', {
                    productId,
                    slug: productSlug,
                    videoId: DEFAULT_VIDEO_ID,
                });
            },
        },
        prepareSuite(BreadcrumbsSuite, {
            pageObjects: {
                breadcrumbs() {
                    return this.createPageObject(Breadcrumbs);
                },
            },
            params: {
                breadcrumbsText: [
                    'Вино',
                    'Chateau d\'Armailhac',
                    'Отзывы',
                ],
            },
        }),
        prepareSuite(ContentUserInfoSuite, {
            params: {
                userName: 'Willy W.',
            },
        }),
        prepareSuite(ProductHorizontalSuite, {
            pageObjects: {
                productHorizontal() {
                    return this.createPageObject(ProductHorizontal);
                },
            },
            params: {
                productId,
                slug: productSlug,
            },
        }),
        prepareSuite(CommentFormSuite, {
            hooks: {
                async beforeEach() {
                    return this.browser.yaSlowlyScroll(BigForm.root);
                },
            },
            pageObjects: {
                commentForm() {
                    return this.createPageObject(BigForm);
                },
            },
        }),
        prepareSuite(CommentListSuite, {
            hooks: {
                async beforeEach() {
                    return this.browser.yaSlowlyScroll(CommentList.root);
                },
            },
            pageObjects: {
                commentList() {
                    return this.createPageObject(CommentList);
                },
            },
        }),
        prepareSuite(UgcVideoSuite, {
            hooks: {
                async beforeEach() {
                    return this.browser.yaSlowlyScroll(UgcVideoPlayer.root);
                },
            },
            pageObjects: {
                ugcVideoPlayer() {
                    return this.createPageObject(UgcVideoPlayer);
                },
                ugcVideoPlayerFooter() {
                    return this.createPageObject(UgcVideoPlayerFooter);
                },
            },
        })
    ),
});
