'use strict';

import {keys} from 'lodash';

import {scrollToElementBySelector, makeElementInvisibleBySelector} from 'spec/gemini/helpers';

import {subscriberProducts} from 'app/constants/notifications';
import Notifications from 'spec/page-objects/Notifications';
import LayoutBase from 'spec/page-objects/LayoutBase';
import Hint from 'spec/page-objects/Hint';
import DropdownB2bNext from 'spec/page-objects/DropdownB2bNext';
import {Case} from 'spec/gemini/lib/types';

const activeDropdownSelector = `${DropdownB2bNext.root}${DropdownB2bNext.active}`;

export default {
    suiteName: 'Hints',
    childSuites: keys(subscriberProducts)
        .filter(productKey => !subscriberProducts[productKey as keyof typeof subscriberProducts].hidden)
        .map(
            (suiteName, index) =>
                ({
                    suiteName,
                    selector: activeDropdownSelector,
                    before(actions, find) {
                        const hintTogglerSelector = `${Notifications.getSubscribersGroupByIndex(index)} ${Hint.root}`;

                        actions.waitForElementToShow(hintTogglerSelector, 10000);
                        scrollToElementBySelector.call(actions, hintTogglerSelector);
                        actions
                            .click(find(hintTogglerSelector))
                            .wait(500)
                            .waitForElementToShow(activeDropdownSelector, 10000);
                        scrollToElementBySelector.call(actions, activeDropdownSelector);
                        makeElementInvisibleBySelector.call(actions, LayoutBase.root);
                    },
                    // eslint-disable-next-line @typescript-eslint/no-empty-function
                    capture() {},
                } as Case),
        ),
};
