'use strict';

import getRouteData from 'spec/lib/helpers/getRouteData';
import questionsMock from 'spec/lib/page-mocks/questions.json';
import modelOpinionsMock from 'spec/lib/page-mocks/modelOpinions.json';
import {makeKadavrSuite} from 'spec/gemini/lib/kadavr-gemini';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import {isAllowed} from 'shared/permissions';
import Menu from 'spec/page-objects/Menu';
import {User} from 'spec/lib/constants/users/users';

const COUNTERS_WAIT_TIMEOUT = 10000;

const waitForCountersShow = (count: number) =>
    // eslint-disable-next-line no-new-func
    new Function(`return window.document.querySelectorAll('${Menu.item}[data-counter]').length === ${count}`);

const opinionsPermissions = getRouteData(ROUTE_NAMES.MODELS_OPINIONS).permissionsOnly;
const questionsPermissions = getRouteData(ROUTE_NAMES.QUESTIONS).permissionsOnly;

type Options = {user: User; permissionsByVendor: string[]; vendor: number; url: string};

export default ({user, permissionsByVendor, vendor, url}: Options) =>
    makeKadavrSuite({
        url,
        user,
        suiteName: 'Menu',
        selector: Menu.root,
        state: {
            vendorsModelQuestions: questionsMock,
            vendorsModelOpinions: modelOpinionsMock,
        },
        before(actions) {
            // без вендора не будет пунктов со счетчиками
            if (!vendor) {
                return;
            }

            let count = 0;

            // если есть пермишены на отзывы
            if (isAllowed(permissionsByVendor, opinionsPermissions)) {
                count++;
            }

            // если есть пермишены на вопросы
            if (isAllowed(permissionsByVendor, questionsPermissions)) {
                count++;
            }

            if (count > 0) {
                actions.waitForJSCondition(waitForCountersShow(count), COUNTERS_WAIT_TIMEOUT);
            }
        },
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        capture() {},
    });
