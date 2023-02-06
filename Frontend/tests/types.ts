import { ISubmissionTest, ISubmissionReport } from 'common/types/submission';

export enum TestsTab {
    FAILED = 'FAILED',
    ALL = 'ALL',
}

export interface Props {
    submissionId: ISubmissionReport['id'];
    tests: ISubmissionTest[];
    isLoading: boolean;
    onShowTestDetail: () => void;
}
