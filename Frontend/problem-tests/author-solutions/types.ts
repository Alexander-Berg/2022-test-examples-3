import { ICompiler } from 'common/types/compiler';
import { IMaybeError } from 'common/types/error';
import { IAuthorSolution, IProblem } from 'common/types/problem';

import { IProblemSettingsSubmitRequest } from 'client/store/problem-settings/types';
import {
    IClearSubmissionStatus,
    ISubmitSolutionRequest,
} from 'client/store/problem-solutions/types';

export interface OwnProps {
    className?: string;
    solutions: IAuthorSolution[];
    problemId: IProblem['id'];
    readonly?: boolean;
}

export interface StateProps {
    fetchCompilersError: IMaybeError;
    fetchCompilersStarted: boolean;
    compilers: ICompiler[] | null;
    submitSolutionStarted: boolean;
    settingsFetchStarted: boolean;
    settingsSubmitStarted: boolean;
}

export interface DispatchProps {
    fetchCompilers: () => void;
    submitSolution: (_request: ISubmitSolutionRequest) => void;
    submitSettings: (_request: IProblemSettingsSubmitRequest) => void;
    clearSubmissionStatus: (_params: IClearSubmissionStatus) => void;
}

export type Props = OwnProps & StateProps & DispatchProps;
