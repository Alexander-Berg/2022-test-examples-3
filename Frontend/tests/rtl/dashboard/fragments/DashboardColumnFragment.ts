import { DashboardBoardCard } from './DashboardBoardCard';
import { PageFragment } from '../../utils/fragments/PageFragment';

export class DashboardColumnFragment extends PageFragment {
    get title(): string {
        return this.textFromSelector('.BoardColumn-Header');
    }

    get cards(): DashboardBoardCard[] {
        return this.getAll(DashboardBoardCard, '.BoardCard');
    }
}
