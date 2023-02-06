import {render, fireEvent, cleanup} from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import Input from '../Input';

describe('<Input />', () => {
    afterEach(cleanup);

    it('renders without errors', () => {
        expect(() => render(<Input />)).not.toThrow();
    });

    it('sets value in input', () => {
        const {getByDisplayValue} = render(<Input value="hello" />);

        expect(getByDisplayValue('hello')).toBeTruthy();
    });

    it('sets placeholder in input', () => {
        const {getByPlaceholderText} = render(<Input placeholder="hello" />);

        expect(getByPlaceholderText('hello')).toBeTruthy();
    });

    it('sets id in input', () => {
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input id="hello" />
            </>,
        );

        expect(getByLabelText('Hello label')).toBeTruthy();
    });

    it('sets name in input', () => {
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input id="hello" name="world" />
            </>,
        );

        expect((getByLabelText('Hello label') as HTMLInputElement).name).toBe(
            'world',
        );
    });

    it('handles change in input', () => {
        const handleChange = jest.fn();
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input
                    id="hello"
                    name="greeting"
                    value=""
                    onChange={handleChange}
                />
            </>,
        );
        const input = getByLabelText('Hello label');

        fireEvent.change(input, {target: {value: 'world'}});
        expect(handleChange).toBeCalled();
        expect(handleChange.mock.calls[0][0].target.id).toBe('hello');
        expect(handleChange.mock.calls[0][0].target.name).toBe('greeting');
        expect(handleChange.mock.calls[0][1]).toBe('world');
    });

    it('handles focus', () => {
        const handleFocus = jest.fn();
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input id="hello" onFocus={handleFocus} />
            </>,
        );
        const input = getByLabelText('Hello label');

        input.focus();
        expect(handleFocus).toBeCalledTimes(1);
    });

    it('handles blur', () => {
        const handleBlur = jest.fn();
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input id="hello" onBlur={handleBlur} />
            </>,
        );
        const input = getByLabelText('Hello label');

        input.focus();
        input.blur();
        expect(handleBlur).toBeCalledTimes(1);
    });

    it('disables input', () => {
        const handleChange = jest.fn();
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input
                    disabled
                    id="hello"
                    name="greeting"
                    value=""
                    onChange={handleChange}
                />
            </>,
        );
        const input = getByLabelText('Hello label');

        // Не триггерим напрямую событие изменения, т.к. это некорректно
        // https://testing-library.com/docs/guide-events/
        userEvents.click(input);
        userEvents.type(input, 'world');

        expect(handleChange).not.toBeCalled();
    });

    it('autofocuses input', () => {
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input autoFocus id="hello" />
            </>,
        );

        expect(getByLabelText('Hello label')).toBe(document.activeElement);
    });

    it('provides ref to input', () => {
        let inputRef;
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input
                    id="hello"
                    inputRef={input => {
                        inputRef = input;
                    }}
                />
            </>,
        );

        expect(getByLabelText('Hello label')).toBe(inputRef);
    });

    it('handles keydown', () => {
        const keyDowns: string[] = [];
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input
                    id="hello"
                    name="greeting"
                    value=""
                    onKeyDown={e => keyDowns.push(e.key)}
                />
            </>,
        );
        const input = getByLabelText('Hello label');

        fireEvent.keyDown(input, {key: 'Escape', code: 27});
        expect(keyDowns).toHaveLength(1);
        expect(keyDowns[0]).toBe('Escape');
    });

    it('supports type password', () => {
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <Input id="hello" name="greeting" value="" type="password" />
            </>,
        );
        const input = getByLabelText('Hello label') as HTMLInputElement;

        expect(input.type).toBe('password');
    });
});
