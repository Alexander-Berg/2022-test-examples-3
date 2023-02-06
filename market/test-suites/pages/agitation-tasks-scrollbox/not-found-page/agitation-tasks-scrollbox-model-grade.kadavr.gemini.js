import {setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import AgitationScrollBox from '@self/platform/spec/gemini/test-suites/blocks/AgitationScrollBox';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {prepareAgitationState} from '@self/project/src/spec/helpers/prepareAgitationState';
import utils from '@yandex-market/gemini-extended-actions';
import {
    MODEL_GRADE,
} from '@self/root/src/entities/agitation/constants';
import {commonAfterActions, commonBeforeActions} from '../helpers';

const entityId = '12345678';
const slug = 'any-product';
const getPath = () => '/not-found';

/** Тест на страницe магазина
 * Агитация на оставление оценки на товар
 */
export default {
    suiteName: 'AgitationTasksScrollBoxNotExistsPageModelGrade[KADAVR]',
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
        const schema = {
            users: [agitationState.user],
            agitation: agitationState.agitation,
        };
        setState.call(actions, 'schema', schema);
        setState.call(actions, 'report', mergeState([...agitationState.product]));

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
