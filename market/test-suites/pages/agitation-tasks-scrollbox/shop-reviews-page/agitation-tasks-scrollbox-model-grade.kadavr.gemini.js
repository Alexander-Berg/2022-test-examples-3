import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {createShopInfo as createReportShopInfo, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
// $FlowIgnore
import {createShopInfo} from '@yandex-market/kadavr/mocks/ShopInfo/helpers';
import AgitationScrollBox from '@self/platform/spec/gemini/test-suites/blocks/AgitationScrollBox';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import utils from '@yandex-market/gemini-extended-actions';
import {
    MODEL_GRADE,
} from '@self/root/src/entities/agitation/constants';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const entityId = '12345678';
const slug = 'any-shop';
const shopName = 'Лучший магазин';
const getPath = () => `/shop--${slug}/${entityId}/reviews`;

/** Тест на страницe отзывов на магазин
 * Агитация на оставление оценки на товар
 */
export default {
    suiteName: 'AgitationTasksScrollBoxShopReviewsPageModelGrade[KADAVR]',
    url: {
        pathname: getPath(),
    },
    before(actions) {
        commonBeforeActions(actions, getPath());
        const shop = createReportShopInfo({
            slug,
            shopName,
            name: shopName,
            datasourceId: entityId,
        }, entityId);
        const shopInfo = createShopInfo({
            slug,
            shopName,
            name: shopName,
            datasourceId: entityId,
        }, entityId);

        const agitationState = prepareAgitationState([{
            entityId,
            entityName: 'Самый лучший товар',
            agitationType: MODEL_GRADE,
            slug,
            uid: profiles.ugctest3.uid,
        }]);
        const schema = {
            users: [agitationState.user],
            agitation: agitationState.agitation,
            gradesOpinions: [],
            shopOpinions: [],
        };
        setState.call(actions, 'ShopInfo.collections', {shopInfo, shopNames: {entityId: {id: entityId, name: shopName, slug}}});
        setState.call(actions, 'Cataloger.shopTree', {
            navnodes: [{
                category:
                    {
                        entity: 'category',
                        id: 90401,
                        isLeaf: false,
                        modelsCount: 3771185,
                        name: 'Все товары',
                        nid: 54415,
                        offersCount: 2512601,
                    },
                childrenType: 'mixed',
                entity: 'navnode',
                fullName: 'Все товары',
                hasPromo: false,
                id: 54415,
                isLeaf: false,
                link: {
                    params: {
                        hid: [90401],
                        nid: [54415],
                    },
                    target: 'catalog',
                },
                name: 'Все товары',
                slug: 'vse-tovary-v-novosibirske',
                type: 'category',
            }]});
        setState.call(actions, 'schema', schema);
        setState.call(actions, 'report', mergeState([...agitationState.product, shop]));

        commonAfterActions(actions, schema);
    },
    after(actions) {
        utils.logout.call(actions);
        deleteSession.call(actions);
    },
    childSuites: [
        AgitationScrollBox,
    ],
};
