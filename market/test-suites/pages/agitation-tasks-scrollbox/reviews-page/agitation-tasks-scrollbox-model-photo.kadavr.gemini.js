import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import utils from '@yandex-market/gemini-extended-actions';

import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import {
    MODEL_GRADE_PHOTO,
} from '@self/root/src/entities/agitation/constants';

import AgitationScrollBox from '@self/platform/spec/gemini/test-suites/blocks/AgitationScrollBox';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

import {preparePageState, getPath} from './helpers';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const entityId = '12345678';
const slug = 'any-product';
/** Тест на страницы отзывов c 4 отзывами
 * Агитация на оставление фотографии
 */
export default {
    suiteName: 'AgitationTasksScrollBoxReviewPageModelPhoto[KADAVR]',
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
            entityName: 'Самый красивый товар',
            agitationType: MODEL_GRADE_PHOTO,
            slug,
            uid: profiles.ugctest3.uid,
        }]);
        const schema = {
            users: [reviewAuthor, ...agitationState.user],
            gradesOpinions: review,
            modelOpinions: review,
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
