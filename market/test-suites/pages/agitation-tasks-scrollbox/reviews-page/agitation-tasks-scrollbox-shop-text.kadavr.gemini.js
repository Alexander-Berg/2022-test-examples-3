import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import utils from '@yandex-market/gemini-extended-actions';

import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import {
    SHOP_GRADE_TEXT,
} from '@self/root/src/entities/agitation/constants';

import AgitationScrollBox from '@self/platform/spec/gemini/test-suites/blocks/AgitationScrollBox';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

import {preparePageState, getPath} from './helpers';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const entityId = '12345';
const slug = 'any';
/** Тест на страницы отзывов c 4 отзывами
 * Агитация на оставление текста к оценке на магазин
 */
export default {
    suiteName: 'AgitationTasksScrollBoxReviewPageShopText[KADAVR]',
    url: {
        pathname: getPath(),
    },
    before(actions) {
        commonBeforeActions(actions, getPath());
        const {
            reportProduct,
            review,
            reviewAuthor,
        } = preparePageState(actions);
        const agitationState = prepareAgitationState([{
            entityId,
            agitationType: SHOP_GRADE_TEXT,
            slug,
            uid: profiles.ugctest3.uid,
            entityName: 'Самый лучший магазин',
        }]);
        const schema = {
            users: [reviewAuthor, ...agitationState.user],
            gradesOpinions: review,
            modelOpinions: review,
            agitation: agitationState.agitation,
        };
        setState.call(actions, 'ShopInfo.collections', {shopInfo: mergeState(agitationState.shopInfo)});
        setState.call(actions, 'schema', schema);
        setState.call(actions, 'report', mergeState([
            ...agitationState.product,
            reportProduct,
            ...agitationState.shop,
        ]));

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
