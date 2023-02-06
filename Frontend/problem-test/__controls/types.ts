export interface Props {
    disabled?: boolean;
    onAdd: (_variantText: string) => void;
}

export interface State {
    variantText: string;
}
