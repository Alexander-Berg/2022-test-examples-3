import {shallow} from 'enzyme';

import SearchControl, {
    ISearchControlProps,
    ISearchControlState,
} from '../SearchControl';

describe('Components/SearchControl', () => {
    const componentPlaceholder = 'SearchControlPlaceholder';

    it('should render valid and check snapshot', () => {
        const controlNode = <div>Control node text</div>;

        const wrapper = shallow(
            <SearchControl
                controlNode={controlNode}
                placeholder={componentPlaceholder}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('should has canUseTransition at state after mount component', () => {
        const wrapper = shallow<
            SearchControl,
            ISearchControlProps,
            ISearchControlState
        >(<SearchControl placeholder={componentPlaceholder} />);

        expect(wrapper.state().canUseTransition).toEqual(true);
    });

    it('should has "visible" mod value for falsy "isEmpty" property', () => {
        const wrapper = shallow(
            <SearchControl
                placeholder={componentPlaceholder}
                isEmpty={false}
            />,
        );
        const labelPlaceholderNode = wrapper.find('.labelPlaceholder');

        expect(
            labelPlaceholderNode.hasClass('labelPlaceholder_visible'),
        ).toEqual(true);
    });

    it('should has placeholder text from props', () => {
        const wrapper = shallow(
            <SearchControl placeholder={componentPlaceholder} />,
        );

        const labelPlaceholderNode = wrapper.find('.labelPlaceholder');

        expect(labelPlaceholderNode.text()).toEqual(componentPlaceholder);
    });
});
