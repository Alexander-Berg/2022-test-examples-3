import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

const PRODUCT_ID = 123;

const productOptions = {
    type: 'model',
    id: PRODUCT_ID,
    slug: 'smartfon-apple-iphone-7-128gb',
    categories: [
        {
            entity: 'category',
            id: 91491,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            slug: 'mobilnye-telefony',
            type: 'guru',
            isLeaf: true,
        },
    ],
    video: ['http://test/url/1'],
    titles: {
        raw: 'Смартфон Apple iPhone 7 128GB',
        highlighted: [{
            value: 'Смартфон Apple iPhone 7 128GB',
        }],
    },
};

const mockedProduct = createProduct(productOptions, PRODUCT_ID);
const picture = createEntityPicture(
    {
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
        thumbnails: [{
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/2hq',
        }],
    },
    'product', PRODUCT_ID,
    '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig'
);

const product = mergeReportState([
    mockedProduct,
    picture,
]);

const tarantinoState = {
    videoUrls: [
        {
            created: '20121016T194555',
            duration: 542,
            hosting: 'youtube.com',
            is_manual: false,
            relevance: 0.4661544441,
            thumbnail_link: 'http://avatars.mds.yandex.net/get-vthumb/213495/c1221d24e5ae384419b690b9c80b06ed',
            title: 'Samsung GT-N7100 Galaxy Note II',
            url: 'http://www.youtube.com/watch?v=X2mSeIQmBso',
            views: 46724,
        },
    ],
};

const vhFrontend = {
    reqid: '1',
    stream_info: [{
        uuid: '1',
        output_stream_id: 'a1',
        streams: [{
            stream: 'https://strm.yandex.ru/kal/demo_channel/demo_channel0.m3u8',
            stream_type: '',
        }],
        playlist_generation: '',
    }],
};

const s3Mds = {
    id: `${PRODUCT_ID}`,
    videoUrl: 'test/url',
    metaUrl: 'test/metaUrl',
    videoId: '1',
};

export {
    product,
    productOptions,
    tarantinoState,
    vhFrontend,
    s3Mds,
};
