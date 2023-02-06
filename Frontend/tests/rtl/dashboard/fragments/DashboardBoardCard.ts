import { PageFragment } from '../../utils/fragments/PageFragment';

export class DashboardBoardCard extends PageFragment {
    get name(): string {
        return this.textFromSelector('.CandidateName');
    }
}
