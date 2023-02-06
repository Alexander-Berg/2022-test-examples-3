import { render } from '@testing-library/react';
import React from 'react';

import { AcademyButton } from './academy-button';
import { AcademyButtonSize, AcademyButtonColor } from './academy-button.types';

describe('AcademyButton', function () {
    it('AcademyButton is rendered successfully', () => {
        const { queryByText } = render(
            <AcademyButton size={AcademyButtonSize.S} color={AcademyButtonColor.WHITE}>
                WHITE
            </AcademyButton>,
        );

        const button = queryByText('WHITE');

        expect(button).toBeTruthy();
    });
});
