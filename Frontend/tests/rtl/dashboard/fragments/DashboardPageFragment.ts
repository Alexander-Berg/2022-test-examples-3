import { DashboardTabsFragment } from './DashboardTabsFragment';
import { DashboardColumnFragment } from './DashboardColumnFragment';

import { PageFragment } from '../../utils/fragments/PageFragment';
import { waitFor } from '@testing-library/react';

type ColumnName = 'drafts' | 'news' | 'interestings' | 'ourSkypes' | 'ourOnsites' | 'ourFinals' | 'ourOffers'

export class DashboardPageFragment extends PageFragment {
    get columns() {
        return this.getAll(DashboardColumnFragment, '.BoardColumn');
    }

    get tabs() {
        return this.get(DashboardTabsFragment, '.ApplicationsDashboardView-Tabs');
    }

    column(columnName: ColumnName): DashboardColumnFragment {
        return this.get(DashboardColumnFragment, `.BoardColumn_name_${columnName}`);
    }

    async waitTableColumnLoaded(columnName: ColumnName) {
        await waitFor(() => {
            const columnElement = this.selectElement(`.BoardColumn_name_${columnName}`)
            expect(columnElement.querySelector('.List-EmptyMessage')).toBeNull();
        });
    }
}
