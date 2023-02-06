import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import AgitationScrollBox from '@self/platform/spec/gemini/test-suites/blocks/AgitationScrollBox';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import utils from '@yandex-market/gemini-extended-actions';
import {
    MODEL_GRADE,
} from '@self/root/src/entities/agitation/constants';
import {preparePageState, getPath} from './helpers';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const entityId = '12345678';
const slug = 'any-product';
/** Тест на страницы отзывов c 4 отзывами
 * Агитация на оставление оценки с кешбеком
 */
export default {
    suiteName: 'AgitationTasksScrollBoxReviewPageModelGradeCashback[KADAVR]',
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
            agitationType: MODEL_GRADE,
            slug,
            uid: profiles.ugctest3.uid,
            sku: {
                id: '123455678899',
                name: 'test',
            },
            persPayAvailable: true,
        }]);
        const schema = {
            users: [reviewAuthor, ...agitationState.user],
            gradesOpinions: review,
            modelOpinions: review,
            skus: agitationState.skus,
            agitation: agitationState.agitation,
            paymentOffer: agitationState.paymentOffer,
        };
        setState.call(actions, 'schema', schema);
        setState.call(actions, 'report', mergeState([...agitationState.product, reportProduct]));
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
