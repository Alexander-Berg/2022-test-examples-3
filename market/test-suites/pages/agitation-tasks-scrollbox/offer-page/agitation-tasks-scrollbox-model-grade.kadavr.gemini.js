import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import AgitationTasksScrollUp from '@self/platform/spec/gemini/test-suites/blocks/AgitationTasksScrollUp';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import utils from '@yandex-market/gemini-extended-actions';
import {
    MODEL_GRADE,
} from '@self/root/src/entities/agitation/constants';
import {offerMock} from '@self/platform/spec/gemini/test-suites/pages/KO/mocks/offer.mock';
import {makeCatalogerTree} from '@self/project/src/spec/hermione/helpers/metakadavr';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const catalogerMock = makeCatalogerTree('Название категории в Хлебных Крошках', 138608, 54726, {categoryType: 'guru'});

const entityId = '12345678';
const slug = 'any-product';
const getPath = () => `/offer/${offerMock.wareId}`;

// Кадавр вернёт все доступные КМ - т.е. carouselProducts + mainProduct
const carouselProducts = [
    createProduct({
        titles: {raw: 'Товар без офферов'},
        pictures: offerMock.pictures,
    }),
    createProduct({
        titles: {raw: 'No offers'},
        pictures: offerMock.pictures,
    }),
];
const mainProduct = {
    type: 'model',
    titles: {
        raw: 'Товар с промокодом',
    },
    offers: {
        items: [offerMock.wareId],
    },
    pictures: offerMock.pictures,
    isNew: true,
};
/** Тест на страницe магазина
 * Агитация на оставление оценки на товар
 */
export default {
    suiteName: 'AgitationTasksScrollBoxOfferPageModelGrade[KADAVR]',
    url: {
        pathname: getPath(),
    },
    before(actions) {
        commonBeforeActions(actions, getPath());
        const agitationState = prepareAgitationState([{
            entityId,
            entityName: 'Самый лучший товар',
            agitationType: MODEL_GRADE,
            slug,
            uid: profiles.ugctest3.uid,
        }]);
        const customOfferMock = {
            ...offerMock,
            titles: {
                raw: 'Оффер без привязки к КМ M357/OF2176/QQ1337/ASD1234/DF8103/MEM',
            },
            description: 'Замоканый оффер.\nКартинка может протухнуть.\nОффер без привязки к КМ',
            // Нужно чтобы кадавр не пытался замокать эти значения рандомно
            productId: null,
            model: {
                id: null,
            },
        };

        setState.call(actions, 'Cataloger.tree', catalogerMock);
        setState.call(actions, 'report', mergeState([
            createProduct(mainProduct, 123),
            ...carouselProducts,
            createOffer(customOfferMock, customOfferMock.wareId),
            ...agitationState.product,
        ]));


        const schema = {
            users: [agitationState.user],
            agitation: agitationState.agitation,
        };
        setState.call(actions, 'schema', schema);
        commonAfterActions(actions, schema);
    },
    after(actions) {
        utils.logout.call(actions);
        deleteSession.call(actions);
    },
    childSuites: [
        AgitationTasksScrollUp,
    ],
};
