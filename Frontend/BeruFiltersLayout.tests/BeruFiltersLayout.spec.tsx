import * as React from 'react';
import { shallow } from 'enzyme';
import { LinkLayoutDefault as Link } from '@yandex-turbo/components/Link/_layout/Link_layout_default';
import { BeruText } from '@yandex-turbo/components/BeruText/BeruText';
import { BeruButton } from '@yandex-turbo/components/BeruButton/BeruButton';
import { BeruFiltersLayout } from '../BeruFiltersLayout';
import { IProps } from '../BeruFiltersLayout.types';

describe('BeruFiltersLayout', () => {
    const baseProps: IProps = {
        title: 'Фильтры',
        closeLink: 'https://yandex.ru',
        applyButtonText: 'Применить',
    };
    const cn = (element: string) => {
        return `.beru-filters-layout__${element}`;
    };

    it('кнопка закрытия страницы должна отображаться, если передана опция showCloseButton', () => {
        const wrapper = shallow(<BeruFiltersLayout {...baseProps} showCloseButton closeLink="https://test.test" />);
        const closeLink = wrapper.find(Link);

        expect(closeLink).toHaveLength(1);
        expect(closeLink.props()).toEqual({
            url: 'https://test.test',
            className: cn('close-button').replace('.', ''),
        });
    });

    it('кнопка "Назад" должна отображаться, если передана опция showBackButton', () => {
        const wrapper = shallow(<BeruFiltersLayout {...baseProps} showBackButton />);

        expect(wrapper.find(cn('back-button'))).toHaveLength(1);
    });

    it('при клике по кнопке "Назад", вызывает переданный обработчик', () => {
        const backHandler = jest.fn();
        const wrapper = shallow(<BeruFiltersLayout {...baseProps} showBackButton goBackHandler={backHandler} />);

        wrapper.find(cn('back-button')).simulate('click');

        expect(backHandler).toHaveBeenCalledTimes(1);
    });

    it('счетчик должен отображаться со значением переданным в опции numberSelectedFilters', () => {
        const wrapper = shallow(<BeruFiltersLayout {...baseProps} numberSelectedFilters={0} />);
        const counter = wrapper.find(cn('counter'));

        expect(counter.props()).toMatchObject({ size: '200', weight: 'medium' });
        expect(counter.render().text()).toEqual('Выбрано: 0');
    });

    it('кнопка "Сбросить" должна отображаться если переданное значение в опции numberSelectedFilters > 0', () => {
        const wrapper = shallow(<BeruFiltersLayout {...baseProps} numberSelectedFilters={1} />);
        const resetButton = wrapper.find(cn('reset-button'));

        expect(resetButton).toHaveLength(1);
        expect(resetButton.find(BeruText).props()).toMatchObject({
            size: '200',
            weight: 'medium',
            theme: 'primary',
        });
    });

    it('при клике по кнопке "Сбросить" должен вызываться переданный обработчик', () => {
        const resetHandler = jest.fn();
        const resetButton = shallow(
            <BeruFiltersLayout {...baseProps} numberSelectedFilters={1} resetHandler={resetHandler} />
        ).find(cn('reset-button'));

        resetButton.simulate('click');

        expect(resetHandler).toHaveBeenCalled();
    });

    it('при клике по кнопке "Сбросить" вызывается переданный обработчик', () => {
        const resetHandler = jest.fn();
        const wrapper = shallow(
            <BeruFiltersLayout
                {...baseProps}
                numberSelectedFilters={1}
                resetHandler={resetHandler}
            />
        );

        wrapper.find('.beru-filters-layout__reset-button').simulate('click');

        expect(resetHandler).toHaveBeenCalledTimes(1);
    });

    it('кнопка "Применить" должна отображаться если передана опция showApplyButton', () => {
        const applyButton = shallow(<BeruFiltersLayout {...baseProps} showApplyButton />)
            .find(cn('footer'))
            .find(BeruButton);

        expect(applyButton).toHaveLength(1);
        expect(applyButton.props()).toMatchObject({
            text: baseProps.applyButtonText,
            size: 'l',
        });
    });

    it('при клике по кнопке "Применить" вызывается переданный обработчик', () => {
        const applyHandler = jest.fn();
        const applyButton = shallow(<BeruFiltersLayout {...baseProps} showApplyButton applyHandler={applyHandler} />)
            .find(cn('footer'))
            .find(BeruButton);

        applyButton.simulate('click');

        expect(applyHandler).toHaveBeenCalled();
    });

    it('кнопка "Применить" должна изменить состояние на load если передана опция applyButtonInLoadState', () => {
        const applyButton = shallow(<BeruFiltersLayout {...baseProps} showApplyButton applyButtonInLoadState />)
            .find(cn('footer'))
            .find(BeruButton);

        expect(applyButton.prop('load')).toEqual(true);
    });

    it('переданных потомков должен рендерить в правильном месте', () => {
        const wrapper = shallow(
            <BeruFiltersLayout {...baseProps}>
                <div className="test">Test</div>
            </BeruFiltersLayout>
        );
        const content = wrapper.find('.beru-filters-layout__content');

        expect(content.children().html()).toEqual('<div class="test">Test</div>');
    });
});
