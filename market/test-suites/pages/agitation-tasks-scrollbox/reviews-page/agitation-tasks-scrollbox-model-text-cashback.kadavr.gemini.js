import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import utils from '@yandex-market/gemini-extended-actions';

import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import {
    MODEL_GRADE_TEXT,
} from '@self/root/src/entities/agitation/constants';

import AgitationScrollBox from '@self/platform/spec/gemini/test-suites/blocks/AgitationScrollBox';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

import {preparePageState, getPath} from './helpers';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const entityId = '14236972';
const slug = 'any';
/** Тест на страницы отзывов c 4 отзывами
 * Агитация на оставление текста к оценке с кешбеком
 */
export default {
    suiteName: 'AgitationTasksScrollBoxReviewPageModelTextCashback[KADAVR]',
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
            agitationType: MODEL_GRADE_TEXT,
            slug,
            uid: profiles.ugctest3.uid,
            persPayAvailable: true,
        }]);
        const schema = {
            users: [reviewAuthor, ...agitationState.user],
            gradesOpinions: review,
            modelOpinions: review,
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
