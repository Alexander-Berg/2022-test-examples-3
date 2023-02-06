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
/** Тест на страницы оценок c 4 оценками
 * Агитация на оставление оценки на товар
 */
export default {
    suiteName: 'AgitationTasksScrollBoxQuestionPageModelGrade[KADAVR]',
    url: {
        pathname: getPath(),
    },
    before(actions) {
        const {
            reportProduct,
            question,
            questionAuthor,
        } = preparePageState(actions);
        commonBeforeActions(actions, getPath());
        const agitationState = prepareAgitationState([{
            entityId,
            entityName: 'Самый опасный товар',
            agitationType: MODEL_GRADE,
            slug,
            uid: profiles.ugctest3.uid,
        }]);
        const schema = {
            users: [questionAuthor, ...agitationState.user],
            modelQuestions: question,
            agitation: agitationState.agitation,
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
