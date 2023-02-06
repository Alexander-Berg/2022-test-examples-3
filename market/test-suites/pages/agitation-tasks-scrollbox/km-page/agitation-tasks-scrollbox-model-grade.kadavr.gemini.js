import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createReportProductStateWithPicture} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import AgitationTasksScrollUp from '@self/platform/spec/gemini/test-suites/blocks/AgitationTasksScrollUp';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import utils from '@yandex-market/gemini-extended-actions';
import {
    MODEL_GRADE,
} from '@self/root/src/entities/agitation/constants';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const entityId = '12345678';
const slug = 'any-product';
/** Тест на страницы отзывов c 4 отзывами
 * Агитация на оставление оценки
 */
export const getPath = () => `/product--${slug}/${entityId}`;

export default {
    suiteName: 'AgitationTasksScrollBoxProductPageModelGrade[KADAVR]',
    url: {
        pathname: getPath(),
    },
    before(actions) {
        const reportProduct = createReportProductStateWithPicture({
            slug,
            id: entityId,
            description: 'Ящик пандоры',
            titles: {
                raw: 'Ящик пандоры',
            },
        });
        commonBeforeActions(actions, getPath());
        const agitationState = prepareAgitationState([{
            entityId,
            entityName: 'Самый лучший товар',
            agitationType: MODEL_GRADE,
            slug,
            uid: profiles.ugctest3.uid,
        }]);
        const schema = {
            users: agitationState.user,
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
        AgitationTasksScrollUp,
    ],
};
