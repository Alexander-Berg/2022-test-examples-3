import { IMaybeError } from 'common/types/error';
import { IProblemTestsetBriefInfo } from 'common/types/problem-test';

import {
    IAddTestsetRequest,
    IFetchRequest,
    IFetchTestsetRequest,
    IRemoveRequest,
    IUpdateRequest,
} from 'client/store/problem-testsets/types';

export interface OwnProps {
    className?: string;
    problemId: string;
    readonly?: boolean;
}

export interface IStateProps {
    addSampleStarted: boolean;
    addSampleError: IMaybeError;
    testsets: IProblemTestsetBriefInfo[];
    fetchStarted?: boolean;
    addTestsetStarted?: boolean;
}

export interface IDispatchProps {
    fetchTestsets: (_params: IFetchRequest) => void;
    fetchTestset: (_params: IFetchTestsetRequest) => void;
    removeTestset: (_params: IRemoveRequest) => void;
    updateTestset: (_params: IUpdateRequest) => void;
    addTestset: (_params: IAddTestsetRequest) => void;
    addSample: (_params: IAddTestsetRequest) => void;
}

export type Props = OwnProps & IStateProps & IDispatchProps;
