import * as React from 'react';
import { shallow } from 'enzyme';
import { LinkLayoutDefault as Link } from '@yandex-turbo/components/Link/_layout/Link_layout_default';
import { ShowMore, IProps } from '../ShowMore/DynamicComments-ShowMore';

describe('DynamicComments-ShowMore', () => {
    let defaultProps: IProps;
    beforeEach(() => {
        defaultProps = {
            text: 'Показать еще',
            loadingText: 'Загрузка...',
            restCount: 1,
            onMoreClick: jest.fn(),
        };
    });

    test('Рендерится без ошибок', () => {
        const wrapper = shallow(<ShowMore {...defaultProps} />);

        expect(wrapper.is('.turbo-dynamic-comments__show-more')).toBe(true);
        expect(wrapper.find(Link).prop('text')).toBe('Показать еще (1)');
    });

    test('Отображается индикатор загрузки', () => {
        const wrapper = shallow(<ShowMore {...defaultProps} loading />);

        expect(wrapper.text()).toEqual('Загрузка...');
    });

    test('Вызывается обработчик клика', () => {
        const wrapper = shallow(<ShowMore {...defaultProps} />);

        wrapper.find(Link).simulate('click');

        expect(defaultProps.onMoreClick).toBeCalled();
    });

    test('Проставляется модификатор isreply', () => {
        const wrapper = shallow(<ShowMore {...defaultProps} isReply />);

        expect(wrapper.is('.turbo-dynamic-comments__show-more_isreply')).toBe(true);
    });
});
