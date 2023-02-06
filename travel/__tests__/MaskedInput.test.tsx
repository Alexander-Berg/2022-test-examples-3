import {render, fireEvent, cleanup} from '@testing-library/react';

import {MaskedInput} from '../MaskedInput';

describe('<MaskedInput />', () => {
    afterEach(cleanup);

    it('format inputted text with mask', () => {
        const handleChange = jest.fn();
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <MaskedInput
                    mask={['.', /\d/, '.', /\d/]}
                    id="hello"
                    name="greeting"
                    value=""
                    onChange={handleChange}
                />
            </>,
        );
        const input = getByLabelText('Hello label');

        fireEvent.change(input, {target: {value: '99'}});
        expect(handleChange).toBeCalled();
        expect(handleChange.mock.calls[0][1]).toBe('.9.9');
    });

    it('has mask char "_" by default', () => {
        const handleChange = jest.fn();
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <MaskedInput
                    mask={['.', /\d/, '.', /\d/]}
                    id="hello"
                    name="greeting"
                    value=""
                    onChange={handleChange}
                />
            </>,
        );
        const input = getByLabelText('Hello label');

        fireEvent.change(input, {target: {value: '9'}});
        expect(handleChange).toBeCalled();
        expect(handleChange.mock.calls[0][1]).toBe('.9._');
    });

    it('supports custom mask char', () => {
        const handleChange = jest.fn();
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <MaskedInput
                    mask={['.', /\d/, '.', /\d/]}
                    placeholderChar="$"
                    id="hello"
                    name="greeting"
                    value=""
                    onChange={handleChange}
                />
            </>,
        );
        const input = getByLabelText('Hello label');

        fireEvent.change(input, {target: {value: '9'}});
        expect(handleChange).toBeCalled();
        expect(handleChange.mock.calls[0][1]).toBe('.9.$');
    });

    it('removes mask guide if mask char is empty', () => {
        const handleChange = jest.fn();
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <MaskedInput
                    mask={['.', /\d/, '.', /\d/, '-']}
                    placeholderChar=""
                    id="hello"
                    name="greeting"
                    value=""
                    onChange={handleChange}
                />
            </>,
        );
        const input = getByLabelText('Hello label');

        fireEvent.change(input, {target: {value: '9'}});
        expect(handleChange).toBeCalled();
        expect(handleChange.mock.calls[0][1]).toBe('.9.');
    });

    it('does not show mask on empty input', () => {
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <MaskedInput
                    mask={['.', /\d/, '.', /\d/, '-']}
                    id="hello"
                    name="greeting"
                    value=""
                />
            </>,
        );
        const input = getByLabelText('Hello label') as HTMLInputElement;

        expect(input.value).toBe('');

        input.focus();
        expect(input.value).toBe('');
    });

    it('shows mask on first char entered', () => {
        const {getByLabelText} = render(
            <>
                <label htmlFor="hello">Hello label</label>
                <MaskedInput
                    mask={[/\d/, /\d/, '.', /\d/, /\d/]}
                    id="hello"
                    name="greeting"
                    value=""
                />
            </>,
        );
        const input = getByLabelText('Hello label') as HTMLInputElement;

        fireEvent.change(input, {target: {value: '1'}});
        expect(input.value).toBe('1_.__');
    });
});
