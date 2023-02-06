import { IProblem, ISubmission, SubmissionStatus } from 'common/types/problem';

import {
    IClearSubmissionStatus,
    IRejudgeSubmissionRequest,
} from 'client/store/problem-solutions/types';

export interface OwnProps {
    className?: string;
    problemId: IProblem['id'];
    submissions?: ISubmission[];
    readonly?: boolean;
}

export interface StateProps {
    rejudgeSubmissionStarted?: boolean;
    getSubmissionStatusStarted: boolean;
    lastSubmissionStatus: SubmissionStatus | null;
}

export interface DispatchProps {
    rejudgeSubmission: (_req: IRejudgeSubmissionRequest) => void;
    clearSubmissionStatus: (_params: IClearSubmissionStatus) => void;
}

export type Props = OwnProps & StateProps & DispatchProps;
