export interface ITestUpdate {
    variants: Props['variants'];
    answer: Props['answer'];
}

export interface Props {
    multi?: boolean;
    variants: string[];
    answer: number[];
    disabled?: boolean;
    onUpdate: (_update: ITestUpdate) => void;
}
