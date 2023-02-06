import events from '@testing-library/user-event';

import { PageFragment } from '../../utils/fragments/PageFragment';

export class DashboardTabsFragment extends PageFragment {
    get selectedTab(): string {
        return this.textFromSelector('.TabsMenu-Tab_active')
    }

    selectTab(tabName: string) {
        const element = this.getElementByTitle(tabName, { selector: '.TabsMenu-Tab'});

        events.click(element);
    }
}
