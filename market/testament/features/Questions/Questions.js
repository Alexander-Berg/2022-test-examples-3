import {createUser} from '@yandex-market/kadavr/dist/helpers/entities/user';
import {createShopInfo} from '@yandex-market/kadavr/dist/helpers/entities/shopInfo';
import {createQuestionAnswer} from '@yandex-market/kadavr/dist/helpers/entities/questionAnswer';
import {createShop} from '@yandex-market/kadavr/dist/helpers/entities/shop';
import {createOffer} from '@yandex-market/kadavr/dist/helpers/entities/offer';

export class QuestionsState {
    constructor() {
        this.users = {};
        this.shops = {};
        this.shopInfo = {};
        this.vendors = {};
        this.offers = {};
        this.questions = {};
        this.answers = {};
        this.comments = {};
        this.answerInc = 0;
        this.commentInc = 0;
    }
    async createUser(rawUser = {}) {
        const user = await createUser(rawUser);
        if (!user.id) {
            throw new Error('set user.id');
        }
        this.users[user.id] = user;
        return user;
    }
    async createShop(rawShop = {}) {
        const shop = await createShop(rawShop);
        if (!shop.id) {
            throw new Error('set shop.id');
        }
        this.shops[shop.id] = shop;
        return shop;
    }
    async createShopInfo(rawShopInfo = {}) {
        const shopInfo = await createShopInfo(rawShopInfo);
        if (!shopInfo.id) {
            throw new Error('set shopInfo.id');
        }
        this.shopInfo[shopInfo.id] = shopInfo;
        return shopInfo;
    }
    async createOffer(
        rawOffer = {},
        options = {}
    ) {
        const offer = await createOffer(rawOffer, options);
        if (!offer.wareId) {
            throw new Error('set offer.wareId');
        }
        this.offers[offer.wareId] = offer;
        return offer;
    }

    async createQuestionAnswer(
        rawAnswer = {},
        options = {}
    ) {
        if (!rawAnswer.id) {
            throw new Error('set answer.id');
        }
        const answer = await createQuestionAnswer(rawAnswer, options);
        if (!answer.id) {
            throw new Error('set answer.id');
        }
        this.answers[answer.id] = answer;
        const questionId =
            answer.question && answer.question.id ? answer.question.id : '';
        const question = this.questions[questionId];
        if (!question.answersCount) {
            question.answersCount = 0;
        }
        question.answersCount++;
        return answer;
    }
}
