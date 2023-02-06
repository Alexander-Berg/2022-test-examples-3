import React, {useCallback} from 'react';

import cx from './DatePicker.scss';

interface IDatePickerProps {
    name?: string;
    value: string;
    onChange(value: string): void;
}

const DatePicker: React.FC<IDatePickerProps> = props => {
    const {name, value, onChange} = props;

    const handleChange: React.ChangeEventHandler<HTMLInputElement> =
        useCallback(
            e => {
                onChange(e.target.value);
            },
            [onChange],
        );

    return (
        <input
            className={cx('root')}
            type="date"
            name={name}
            value={value}
            onChange={handleChange}
        />
    );
};

export default DatePicker;
