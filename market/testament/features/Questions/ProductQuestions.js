import {createProduct} from '@yandex-market/kadavr/dist/helpers/entities/product';
import {createProductQuestion} from '@yandex-market/kadavr/dist/helpers/entities/question/productQuestion';
import reportHelpers from '@yandex-market/kadavr/mocks/Report/helpers';
import {QuestionsState} from './Questions';

const objectToArray = obj => Object.values(obj).map(value => value);

const {
    createProduct: createReportProduct,
    mergeState: mergeReportState,
    createEntityPicture: createReportEntityPicture,
    createShopInfo: createReportShopInfo,
    createOffer: createReportOffer,
} = reportHelpers;

const createProductPicture = productId =>
    createReportEntityPicture(
        {
            original: {
                url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
            },
        },
        'product',
        productId,
        '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/1hq'
    );

export class ProductQuestionsState extends QuestionsState {
    constructor() {
        super();
        this.products = {};
    }
    async createProduct(rawProduct = {}) {
        const product = await createProduct(rawProduct);
        if (!product.id) {
            throw new Error('set product.id');
        }
        this.products[product.id] = product;
        return product;
    }
    async createProductQuestion(
        rawQuestion = {},
        options = {}
    ) {
        const question = await createProductQuestion(rawQuestion, options);
        if (!question.id) {
            throw new Error('set question.id');
        }
        this.questions[question.id] = question;
        return question;
    }

    async getState() {
        const productReportStates = objectToArray(this.products).map(
            product => createReportProduct(product, product.id)
        );

        // TODO picture to createProductWithPicture
        const productPictureStates = objectToArray(this.products).map(
            product => {
                if (!product.id) {
                    throw new Error(
                        `no product.id for ${JSON.stringify(product)}`
                    );
                }
                return createProductPicture(product.id);
            }
        );

        const shopInfoStates = objectToArray(this.shopInfo).map(shop => {
            if (!shop.id) {
                throw new Error(`no shop.id for ${JSON.stringify(shop)}`);
            }
            return createReportShopInfo(shop, shop.id);
        });

        const offerStates = objectToArray(this.offers).map(offer =>
            createReportOffer(offer, offer.wareId)
        );

        const reportState = mergeReportState([
            ...productReportStates,
            ...productPictureStates,
            ...shopInfoStates,
            ...offerStates,
        ]);

        const schemaState = {
            users: objectToArray(this.users),
            modelQuestions: objectToArray(this.questions),
            modelAnswers: objectToArray(this.answers),
            comments: objectToArray(this.comments),
        };

        return {
            reportState,
            schemaState,
        };
    }
}
