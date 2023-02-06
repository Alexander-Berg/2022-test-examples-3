import { ReactNode } from 'react';

import { IMaybeError } from 'common/types/error';
import { IProblemTest } from 'common/types/problem';

import { Props as WithIdProps } from 'client/decorators/with-id/types';
import { Props as WithPopupProps } from 'client/decorators/with-popup/types';
import { IProblemCreateTestRequest } from 'client/store/problems/types';

export interface OwnProps {
    problemId: string;
    children: (_openModal: () => void) => ReactNode;
}

export interface IStateProps {
    createTestStarted: boolean;
    createTestError: IMaybeError;
}

export interface IDispatchProps {
    createTest: (_params: IProblemCreateTestRequest) => void;
}

export interface State {
    data: IProblemTest;
}

export type Props = OwnProps & IStateProps & IDispatchProps & WithIdProps & WithPopupProps;
