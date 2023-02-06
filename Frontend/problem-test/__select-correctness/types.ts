export interface Props {
    onChange: (_isCorrect: boolean) => void;
    value: boolean;
    disabled?: boolean;
}
