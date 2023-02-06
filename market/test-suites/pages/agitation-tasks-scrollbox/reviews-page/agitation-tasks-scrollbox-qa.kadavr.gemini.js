import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import utils from '@yandex-market/gemini-extended-actions';

import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import {
    MODEL_QUESTION_ANSWER,
} from '@self/root/src/entities/agitation/constants';

import AgitationScrollBox from '@self/platform/spec/gemini/test-suites/blocks/AgitationScrollBox';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

import {preparePageState, getPath} from './helpers';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const slug = 'any';
const entityId = '1234567890';
/** Тест на страницы отзывов c 4 отзывами
 * Агитация на оставление ответа на вопрос
 */
export default {
    suiteName: 'AgitationTasksScrollBoxReviewPageQA[KADAVR]',
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
            agitationType: MODEL_QUESTION_ANSWER,
            slug,
            uid: profiles.ugctest3.uid,
            extraEntity: {
                authorUid: '12345667',
                authorPublicName: 'Asker',
                slug: 'question-slug',
            },
        }]);
        const schema = {
            users: [reviewAuthor, ...agitationState.user],
            gradesOpinions: review,
            modelOpinions: review,
            modelQuestions: agitationState.question,
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
