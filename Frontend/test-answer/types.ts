import { AnswerType, ITestProblem } from 'common/types/problem';

import { OnChangeFunction } from 'client/decorators/with-form/types';
import { Props as WithIdProps } from 'client/decorators/with-id/types';

export interface OwnProps {
    answerType: AnswerType;
    options: string[];
    answers: number[];
    isDisabled: boolean;
    onChange: OnChangeFunction<ITestProblem['details']>;
}

export type Props = OwnProps & WithIdProps;
