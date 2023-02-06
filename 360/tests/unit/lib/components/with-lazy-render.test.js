import React, { Component } from 'react';
import { mount } from 'enzyme';

import withLazyRender from '../../../../lib/components/with-lazy-render';

const MyComponent = ({ visible }) => (
    <div className={'my-component' + (visible ? '_visible' : '')}>text inside</div>
);

const MyComponentWithLazyRender = withLazyRender(MyComponent);

class WrapperComponent extends Component {
    constructor(...args) {
        super(...args);
        this.state = {
            isInnerVisible: false
        };
    }
    showInner() {
        this.setState({
            isInnerVisible: true
        });
    }
    hideInner() {
        this.setState({
            isInnerVisible: false
        });
    }
    render() {
        return <MyComponentWithLazyRender visible={this.state.isInnerVisible}/>;
    }
}

describe('with-lazy-render =>', () => {
    it('should not mount invisible component', () => {
        const component = mount(<MyComponentWithLazyRender/>);
        expect(component.render()).toMatchSnapshot();
        component.unmount();
    });

    it('should mount visible component', () => {
        const component = mount(<MyComponentWithLazyRender visible/>);
        expect(component.render()).toMatchSnapshot();
        component.unmount();
    });

    it('should mount component after make it visible', () => {
        const wrapper = mount(<WrapperComponent/>);
        expect(wrapper.render()).toMatchSnapshot();
        wrapper.instance().showInner();
        wrapper.update();
        expect(wrapper.render()).toMatchSnapshot();
        wrapper.unmount();
    });

    it('should not unmount component after make it invisible', () => {
        const wrapper = mount(<WrapperComponent/>);
        wrapper.instance().showInner();
        wrapper.update();
        expect(wrapper.render()).toMatchSnapshot();
        wrapper.instance().hideInner();
        wrapper.update();
        expect(wrapper.render()).toMatchSnapshot();
        wrapper.unmount();
    });
});
