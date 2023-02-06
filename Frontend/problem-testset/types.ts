import { IMaybeError } from 'common/types/error';
import { IProblem } from 'common/types/problem';
import { IProblemTestset } from 'common/types/problem-test';

export interface State {
    inputPattern: string;
    outputPattern: string;
}

export interface OwnProps {
    problemId: IProblem['id'];
    testsetId: IProblemTestset['id'];
    readonly?: boolean;
    updateTests: () => void;
    onRemove: () => void;
    onChange: (_fieldName: string, _value?: string) => void;
}

export interface IStateProps {
    data?: IProblemTestset;
    fetchTestsStarted?: boolean;
    fetchTestsError?: IMaybeError;
    updateTestsStarted?: boolean;
}

export type Props = OwnProps & IStateProps;
