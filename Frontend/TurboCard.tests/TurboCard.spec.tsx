import * as React from 'react';
import { shallow } from 'enzyme';

import { TurboCard } from '../TurboCard';
import { ITurboCardProps } from '../TurboCard.types';

describe('Компонент TurboCard', () => {
    test('Соответствует снепшоту', () => {
        const props: ITurboCardProps = {
            onClick: jest.fn(),
            id: 'id',
            rounding: 'default',
            className: 'example',
        };
        const component = shallow(
            <TurboCard {...props}>
                <div>Hello World !!!</div>
            </TurboCard>
        );

        expect(component).toMatchSnapshot();
    });

    test('Передает свой идентификатор в обработчик клика', () => {
        const onClick = jest.fn();
        const props: ITurboCardProps = {
            onClick,
            id: 'id',
        };
        const component = shallow(<TurboCard {...props} />);
        component.simulate('click');

        expect(onClick).toBeCalledWith('id');
    });

    test('Передает undefined в обработчик клика', () => {
        const onClick = jest.fn();
        const props: ITurboCardProps = {
            onClick,
        };
        const component = shallow(<TurboCard {...props} />);
        component.simulate('click');

        expect(onClick).toBeCalledWith(undefined);
    });

    test('Становится интерактивной при передаче обработчика клика', () => {
        const onClick = jest.fn();
        const props: ITurboCardProps = {
            onClick,
        };
        const component = shallow(<TurboCard {...props} />);

        expect(component.is('.turbo-card_interactive')).toBe(true);
    });

    test('Становится интерактивной при передаче флага', () => {
        const props: ITurboCardProps = {
            interactive: true,
        };
        const component = shallow(<TurboCard {...props} />);

        expect(component.is('.turbo-card_interactive')).toBe(true);
    });

    test('Прокидывает className в html', () => {
        const props: ITurboCardProps = {
            className: 'example',
        };
        const component = shallow(<TurboCard {...props} />);

        expect(component.is('.example')).toBe(true);
    });

    test('Меняет скругление на top', () => {
        const props: ITurboCardProps = {
            rounding: 'top',
        };
        const component = shallow(<TurboCard {...props} />);

        expect(component.is('.turbo-card_rounding_top')).toBe(true);
    });

    test('Меняет скругление на bottom', () => {
        const props: ITurboCardProps = {
            rounding: 'bottom',
        };
        const component = shallow(<TurboCard {...props} />);

        expect(component.is('.turbo-card_rounding_bottom')).toBe(true);
    });

    test('Меняет скругление на default', () => {
        const props: ITurboCardProps = {
            rounding: 'default',
        };
        const component = shallow(<TurboCard {...props} />);

        expect(component.is('.turbo-card_rounding_default')).toBe(true);
    });
});
