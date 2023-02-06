import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';

// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';

// suites
import DefaultBehaviourSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShortenedReviewUploading/defaultBehaviour';
import SubmitShortenedReview from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShortenedReviewUploading/submitShortenedReview';
import PhotoAttachedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/PhotoList';

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

const DEFAULT_PRODUCT = {
    id: 14206682, // id продукта совпадает с id productWithPicture,
    slug: 'smartfon-apple-iphone-7-128gb', // slug продукта совпадает с slug productWithPicture,
    entity: 'product',
};

const DEFAULT_PICTURE = {
    namespace: 'market-ugc',
    groupId: 3723,
    imageName: '2a000001654282aec0648192ce44a1708325',
};

const prepareProductPhotoAddPage = async ctx => {
    await ctx.browser.setState('report', productWithPicture);
    await ctx.browser.setState('schema', {
        users: [DEFAULT_USER],
        mdsPictures: [DEFAULT_PICTURE],
    });

    await ctx.browser.yaProfile('ugctest3', 'market:product-photo-add', {
        productId: DEFAULT_PRODUCT.id,
        slug: DEFAULT_PRODUCT.slug,
    });
    return ctx.browser.yaClosePopup(ctx.createPageObject(RegionPopup));
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница оставления новых фотографий на товар.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-11329',
    story: {
        async beforeEach() {
            this.params = {
                text: 'Текст отзыва бла бла бла',
                authPluginSkip: true,
            };

            await prepareProductPhotoAddPage(this);
        },

        'Авторизованный пользователь.': mergeSuites(
            prepareSuite(DefaultBehaviourSuite),
            prepareSuite(SubmitShortenedReview),
            prepareSuite(PhotoAttachedSuite)
        ),
    },
});
