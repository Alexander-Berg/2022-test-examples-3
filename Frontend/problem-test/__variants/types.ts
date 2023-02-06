import { Props as ProblemTestProps } from 'client/components/problem-test/types';

export interface Props {
    answer: ProblemTestProps['answer'];
    variants: ProblemTestProps['variants'];
    disabled?: boolean;
    onChange: (_idx: number, _value: boolean) => void;
    onRemove: (_idx: number) => void;
}
