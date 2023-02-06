import React from 'react';
import {render, screen} from '@testing-library/react';

import MyComponent from './MyComponent';

describe('<MyComponent />', () => {
    test('renders root', () => {
        render(<MyComponent />);
        expect(screen.getByRole('root')).toBeInTheDocument();
    });

    test('renders children when passed in', () => {
        const wrapper = render(<MyComponent />);
        expect(wrapper.getByRole('unique')).toBeInTheDocument();
    });
});
