import * as React from 'react';
import { render } from 'react-dom';
import { screen } from '@testing-library/react';

import { MenuType } from 'shared/consts/MenuType';
import { useCheckedMenu } from 'shared/hooks/useCheckedMenu/useCheckedMenu';

const items = [
    {
        label: 'Driver',
        value: 'driver',
    },
    {
        label: 'Car',
        value: 'car',
    },
    {
        label: 'Licence plate number',
        value: 'licence-number',
    },
    {
        label: 'Mileage',
        value: 'mileage',
    },
];

const Menu: React.FC<{ menuType: MenuType; value: string | string[]; callback(): void }> = function Menu({
    menuType,
    value,
    callback,
}) {
    const { checked, onMenuItemClickHandler } = useCheckedMenu(value, menuType, callback);

    return (
        <ul
            data-testid="checked"
            data-checked={checked}
        >
            {items.map(({ label, value }) => {
                return (
                    <li
                        onClick={onMenuItemClickHandler}
                        data-value={value}
                        data-testid={value}
                        key={value}
                    >
                        {label}
                    </li>
                );
            })}
        </ul>
    );
};

describe('useCheckedMenu', function () {
    beforeEach(function () {
        document.body.innerHTML = '';
    });

    it('works with menuType="check" param', async function () {
        const callback = jest.fn();
        const checked = ['driver', 'licence-number'];

        const root = document.createElement('div');

        document.body.appendChild(root);

        render(
            <Menu
                menuType={MenuType.CHECK}
                value={checked}
                callback={callback}
            />,
            root,
        );

        screen.getByTestId('mileage').click();
        expect(callback).toBeCalled();
        expect(screen.getByTestId('checked').getAttribute('data-checked')).toMatchInlineSnapshot(
            `"driver,licence-number,mileage"`,
        );

        screen.getByTestId('driver').click();
        expect(callback).toBeCalled();
        expect(screen.getByTestId('checked').getAttribute('data-checked')).toMatchInlineSnapshot(
            `"licence-number,mileage"`,
        );
    });

    it('works with menuType="radio" param', async function () {
        const callback = jest.fn();
        const checked = 'driver';

        const root = document.createElement('div');

        document.body.appendChild(root);

        render(
            <Menu
                menuType={MenuType.RADIO}
                value={checked}
                callback={callback}
            />,
            root,
        );

        screen.getByTestId('mileage').click();
        expect(callback).toBeCalled();
        expect(screen.getByTestId('checked').getAttribute('data-checked')).toMatchInlineSnapshot(`"mileage"`);

        screen.getByTestId('driver').click();
        expect(callback).toBeCalled();
        expect(screen.getByTestId('checked').getAttribute('data-checked')).toMatchInlineSnapshot(`"driver"`);
    });
});
