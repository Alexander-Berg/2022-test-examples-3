'use strict';

import {scrollToElementBySelector, makeElementInvisibleBySelector} from 'spec/gemini/helpers';
import Tabs from 'spec/page-objects/Tabs';
import SettingsUsersGroup from 'spec/page-objects/SettingsUsersGroup';
import Heading from 'spec/page-objects/HeadingB2b';
import LayoutBase from 'spec/page-objects/LayoutBase';
import Hint from 'spec/page-objects/Hint';
import DropdownB2bNext from 'spec/page-objects/DropdownB2bNext';
import {MakeKadavrSuiteProps} from 'spec/gemini/lib/types';

export default <Product extends {name: string; paid?: boolean} & Record<string, unknown>>(products: Product[]) => {
    const childSuites: MakeKadavrSuiteProps['childSuites'] = [];
    const activeDropdownSelector = `${DropdownB2bNext.root}${DropdownB2bNext.active}`;

    products.forEach(({name, paid}, index) => {
        if (paid) {
            // 2 хинта
            childSuites.push(
                {
                    suiteName: `${name} management`,
                    selector: activeDropdownSelector,
                    before(actions) {
                        const hintSelector = `${Tabs.activePane} ${SettingsUsersGroup.root}:nth-of-type(${index + 1}) ${
                            Heading.root
                        }:nth-of-type(1) ${Hint.root}`;

                        actions.waitForElementToShow(hintSelector, 10000);
                        scrollToElementBySelector.call(actions, hintSelector);
                        actions.click(hintSelector).wait(500).waitForElementToShow(activeDropdownSelector, 10000);
                        scrollToElementBySelector.call(actions, activeDropdownSelector);
                        makeElementInvisibleBySelector.call(actions, LayoutBase.root);
                    },
                    // eslint-disable-next-line @typescript-eslint/no-empty-function
                    capture() {},
                },
                {
                    suiteName: `${name} balance`,
                    selector: activeDropdownSelector,
                    before(actions) {
                        const hintSelector = `${Tabs.activePane} ${SettingsUsersGroup.root}:nth-of-type(${index + 1}) ${
                            Heading.root
                        }:nth-of-type(2) ${Hint.root}`;

                        actions.waitForElementToShow(hintSelector, 10000);
                        scrollToElementBySelector.call(actions, hintSelector);
                        actions.click(hintSelector).wait(500).waitForElementToShow(activeDropdownSelector, 10000);
                        scrollToElementBySelector.call(actions, activeDropdownSelector);
                        makeElementInvisibleBySelector.call(actions, LayoutBase.root);
                    },
                    // eslint-disable-next-line @typescript-eslint/no-empty-function
                    capture() {},
                },
            );
        } else {
            childSuites.push({
                suiteName: name,
                selector: activeDropdownSelector,
                before(actions) {
                    const hintSelector = `${Tabs.activePane} ${SettingsUsersGroup.root}:nth-child(${index + 1}) ${
                        Hint.root
                    }`;

                    actions.waitForElementToShow(hintSelector, 10000);
                    scrollToElementBySelector.call(actions, hintSelector);
                    actions.click(hintSelector).wait(500).waitForElementToShow(activeDropdownSelector, 10000);
                    scrollToElementBySelector.call(actions, activeDropdownSelector);
                    makeElementInvisibleBySelector.call(actions, LayoutBase.root);
                },
                // eslint-disable-next-line @typescript-eslint/no-empty-function
                capture() {},
            });
        }
    });

    return {
        childSuites,
        suiteName: 'Hints',
    };
};
