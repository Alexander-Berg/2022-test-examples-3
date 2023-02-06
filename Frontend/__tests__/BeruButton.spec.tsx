import * as React from 'react';
import { shallow } from 'enzyme';
import { LinkLayoutDefault as Link } from '@yandex-turbo/components/Link/_layout/Link_layout_default';
import { withGoals } from '@yandex-turbo/components/withGoals/withGoals';
import { BeruButtonBase } from '../BeruButton';

let mockHOCWithGoals: ReturnType<typeof jest.fn>;

jest.mock('@yandex-turbo/components/withGoals/withGoals', () => ({
    withGoals: jest.fn(() => {
        mockHOCWithGoals = jest.fn();

        return mockHOCWithGoals;
    }),
}));

describe('BeruButton', () => {
    let goals: Record<string, string>;
    let onClickHandler: ReturnType<typeof jest.fn>;

    beforeEach(() => {
        goals = {
            'data-metrika-goals': '{"ONE": ["GOAL"]}',
        };
        onClickHandler = jest.fn();
    });

    it('по умолчанию корректно рендерится c дефолтной темой и размером', () => {
        const wrapper = shallow(<BeruButtonBase text="Кнопка" />);

        expect(wrapper.name()).toEqual('button');
        expect(wrapper.hasClass('beru-button_theme_action')).toEqual(true);
        expect(wrapper.hasClass('beru-button_size_m')).toEqual(true);
        expect(wrapper.hasClass('beru-button_fullwidth')).toEqual(false);
        expect(wrapper.find('beru-button__image').exists()).toEqual(false);
        expect(wrapper.find('span').text()).toEqual('Кнопка');
    });

    it('корректно рендерится со всеми доступными опциями(кроме load и disabled) как кнопка', () => {
        const wrapper = shallow(
            <BeruButtonBase
                text="Кнопка"
                className="additionalClass"
                theme="select"
                size="l"
                fullwidth
                imagePath="https://path/to/image"
                goals={goals}
                onClick={onClickHandler}
            />
        );

        expect(wrapper.name()).toEqual('button');
        expect(wrapper.hasClass('additionalClass')).toEqual(true);
        expect(wrapper.hasClass('beru-button_theme_select')).toEqual(true);
        expect(wrapper.hasClass('beru-button_size_l')).toEqual(true);
        expect(wrapper.hasClass('beru-button_fullwidth')).toEqual(true);
        expect(wrapper.hasClass('beru-button_load')).toEqual(false);
        expect(wrapper.hasClass('beru-button_disabled')).toEqual(false);
        expect(wrapper.find('.beru-button__spinner')).toHaveLength(0);
        // проверяем что метричные цели передаются
        expect(wrapper.prop('data-metrika-goals')).toEqual(goals['data-metrika-goals']);
        // проверяем что иконка отрисовывается
        expect(wrapper.find('.beru-button__image').props()).toMatchObject({
            className: 'beru-button__image',
            style: {
                backgroundImage: 'url(https://path/to/image)',
            },
        });

        wrapper.simulate('click');

        expect(onClickHandler).toHaveBeenCalledTimes(1);
    });

    it('корректно рендерится со всеми доступными опциями(кроме load и disabled) как ссылка', () => {
        const wrapper = shallow(
            <BeruButtonBase
                text="Кнопка"
                className="additionalClass"
                theme="select"
                size="l"
                fullwidth
                imagePath="https://path/to/image"
                goals={goals}
                url="https://yandex.ru"
                target="_self"
            />
        );
        const wrapperLink = wrapper.find(Link);

        expect(wrapperLink.exists()).toEqual(true);
        expect(wrapperLink.hasClass('additionalClass')).toEqual(true);
        expect(wrapperLink.hasClass('beru-button_theme_select')).toEqual(true);
        expect(wrapperLink.hasClass('beru-button_size_l')).toEqual(true);
        expect(wrapperLink.hasClass('beru-button_fullwidth')).toEqual(true);
        expect(wrapperLink.hasClass('beru-button_load')).toEqual(false);
        expect(wrapperLink.hasClass('beru-button_disabled')).toEqual(false);
        expect(wrapperLink.find('.beru-button__spinner')).toHaveLength(0);
        // проверяем что остальные опции передаются верно в Link
        expect(wrapperLink.props()).toMatchObject({
            goals,
            url: 'https://yandex.ru',
            target: '_self',
        });
        // проверяем что иконка отрисовывается
        expect(wrapperLink.find('.beru-button__image').props()).toMatchObject({
            className: 'beru-button__image',
            style: {
                backgroundImage: 'url(https://path/to/image)',
            },
        });

        // При рендеринки кнопки как ссылки обработчик в нее не передается
        wrapperLink.simulate('click');
        expect(onClickHandler).not.toHaveBeenCalled();
    });

    describe('Кнопка', () => {
        it('в состоянии load должна быть заблокирована, переданные хэндлеры не вызываться, спиннер отображаться', () => {
            const onClick = jest.fn();
            const wrapper = shallow(<BeruButtonBase text="Кнопка" load onClick={onClick} />);

            wrapper.simulate('click');

            expect(wrapper.hasClass('beru-button_load')).toBe(true);
            expect(wrapper.hasClass('beru-button_disabled')).toBe(true);
            expect(wrapper.find('.beru-button__spinner')).toHaveLength(1);
            expect(onClick).not.toHaveBeenCalled();
        });

        it('в состоянии disabled должна быть заблокирована, переданные хэндлеры не вызываться', () => {
            const onClick = jest.fn();
            const wrapper = shallow(<BeruButtonBase text="Кнопка" disabled onClick={onClick} />);

            wrapper.simulate('click');

            expect(wrapper.hasClass('beru-button_disabled')).toBe(true);
            expect(onClick).not.toHaveBeenCalled();
        });
    });

    describe('Ссылка', () => {
        it('в состоянии load должна быть заблокирована, url не передаваться в Link, спинер отображаться', () => {
            const wrapper = shallow(<BeruButtonBase text="Кнопка" load url="https://yandex.ru" />);
            const link = wrapper.find(Link);

            expect(link.props()).toMatchObject({
                url: undefined,
            });
            expect(link.hasClass('beru-button_load')).toBe(true);
            expect(link.hasClass('beru-button_disabled')).toBe(true);
            expect(link.find('.beru-button__spinner')).toHaveLength(1);
        });

        it('в состоянии disabled должна быть заблокирована, url не передаваться в Link', () => {
            const wrapper = shallow(<BeruButtonBase text="Кнопка" disabled url="https://yandex.ru" />);
            const link = wrapper.find(Link);

            expect(link.props()).toMatchObject({
                url: undefined,
            });
            expect(link.hasClass('beru-button_disabled')).toBe(true);
        });
    });

    it('вызывается HOC withGoals и в него передается компонента', () => {
        expect(withGoals).toHaveBeenCalled();
        expect(mockHOCWithGoals).toHaveBeenCalledWith(BeruButtonBase);
    });

    it('коррректно проставляется опция notModifyTarget у Link, если передана опция target', () => {
        const wrapper = shallow(<BeruButtonBase text="Кнопка" url="https://yandex.ru" target="_top" />);

        expect(wrapper.find(Link).props()).toMatchObject({
            target: '_top',
            notModifyTarget: true,
        });

        wrapper.setProps({ target: undefined });

        expect(wrapper.find(Link).prop('notModifyTarget')).toEqual(false);
    });
});
