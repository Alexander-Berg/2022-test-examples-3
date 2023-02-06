import { IProblem } from 'common/types/problem';

export interface OwnProps {
    className?: string;
    problemId: IProblem['id'];
    readonly?: boolean;
}

export interface StateProps {
    settings: IProblem;
}

export type Props = OwnProps & StateProps;
