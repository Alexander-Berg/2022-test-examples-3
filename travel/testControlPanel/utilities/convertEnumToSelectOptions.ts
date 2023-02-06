import {ISelectOption} from 'components/Select/Select';

export default function convertEnumToSelectOptions(
    someEnum: object,
): ISelectOption<string, string>[] {
    return Object.values(someEnum).map(value => ({
        value,
        data: value,
    }));
}
