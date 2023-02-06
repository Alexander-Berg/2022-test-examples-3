import {StaticRouter} from 'react-router-dom';

import {render, fireEvent, cleanup} from '@testing-library/react';

import Recipe from '../Recipe';

describe('<Recipe />', () => {
    afterEach(cleanup);

    test('renders link when "href" passed', () => {
        const {container} = render(<Recipe href="#my-link" />);
        const link = container.querySelector('a')!;

        expect(link).toBeTruthy();
        expect(link.getAttribute('href')).toBe('#my-link');
    });

    test('renders link when "to" passed', () => {
        const {container} = render(
            <StaticRouter context={{}}>
                <Recipe to="/my-link" />
            </StaticRouter>,
        );
        const link = container.querySelector('a')!;

        expect(link).toBeTruthy();
        expect(link.getAttribute('href')).toBe('/my-link');
    });

    test('renders button when not "to" neither "href" but "onClick" passed', () => {
        const {container} = render(<Recipe onClick={() => {}} />);
        const button = container.querySelector('button')!;

        expect(button).toBeTruthy();
    });

    test('recipe handles click', () => {
        const handleClick = jest.fn();
        const {container} = render(<Recipe onClick={handleClick} />);

        fireEvent.click(container.firstChild as HTMLElement);
        expect(handleClick).toHaveBeenCalledTimes(1);
    });
});
