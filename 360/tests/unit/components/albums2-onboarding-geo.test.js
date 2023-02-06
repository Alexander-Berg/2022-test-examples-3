import '../noscript';
import React from 'react';
import { shallow } from 'enzyme';

import { OnboardingGeo } from '../../../components/redux/components/onboarding/onboarding-geo';
import OnboardingBase from '../../../components/redux/components/onboarding';

describe('Albums 2 Onboarding', () => {
    const defaultProps = {
        isSmartphone: false,

        staticHost: 'https://yandex.static.net',
        goToUrl: jest.fn()
    };

    const getProps = (props = {}) => Object.assign({}, defaultProps, props);

    beforeEach(() => {
        jest.resetAllMocks();
    });

    it('should mount on desktop', () => {
        const wrapper = shallow(<OnboardingGeo {...getProps()} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should mount on smart phone', () => {
        const wrapper = shallow(<OnboardingGeo {...getProps({ isSmartphone: true })} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('on button click should go to albums url', () => {
        const props = getProps();

        const wrapper = shallow(<OnboardingGeo {...props} />);
        wrapper.find(OnboardingBase).props().onButtonClick();

        expect(props.goToUrl).toBeCalledTimes(1);
        expect(props.goToUrl).toHaveBeenCalledWith('/client/albums/geo');
    });
});
