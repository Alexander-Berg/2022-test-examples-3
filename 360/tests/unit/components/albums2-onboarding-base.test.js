import '../noscript';
import React from 'react';
import { shallow } from 'enzyme';

import { OnboardingBase } from '../../../components/redux/components/onboarding';
import Logo from '../../../components/redux/components/header/logo-360';
import { Button } from '@ps-int/ufo-rocks/lib/components/lego-components/Button';

import { METRIKA_PATH } from '../../../components/redux/components/albums2/consts';

jest.mock('../../../components/helpers/metrika', () => ({
    count: jest.fn()
}));

import { count } from '../../../components/helpers/metrika';

describe('Albums 2 OnboardingBase', () => {
    const getProps = (props = {}) => {
        const defaultProps = {
            isSmartphone: false,
            saveSettings: jest.fn(),
            updateSettings: jest.fn(),

            cls: 'externalComponentClass',

            metrikaPath: METRIKA_PATH,
            metrikaContext: 'TestContext',
            settingName: 'testOnboardingSettings',

            titleContent: 'Бодрящий заголовок',
            textContent: 'Привлекательная надпись',
            buttonContent: 'Жми уже скорее',

            onButtonClick: jest.fn(),
            onClose: jest.fn()
        };

        return Object.assign({}, defaultProps, props);
    };

    beforeEach(() => {
        jest.resetAllMocks();
    });

    it('should mount on desktop', () => {
        const wrapper = shallow(<OnboardingBase {...getProps()} />);

        expect(count).toHaveBeenCalledWith(...METRIKA_PATH, 'TestContext', 'show');
        expect(wrapper).toMatchSnapshot();
    });

    it('should mount on smart phone', () => {
        const wrapper = shallow(<OnboardingBase {...getProps({ isSmartphone: true })} />);

        expect(count).toHaveBeenCalledWith(...METRIKA_PATH, 'TestContext', 'show');
        expect(wrapper).toMatchSnapshot();
    });

    it('on main button click should send metrika, save settings and call onButtonClick callback', () => {
        const props = getProps();

        const wrapper = shallow(<OnboardingBase {...props} />);

        const mainButton = wrapper.find(Button).at(1);
        mainButton.simulate('click');

        expect(props.onButtonClick).toBeCalledTimes(1);
    });

    it('on close button click should send metrika, save settings and call onClose callback', () => {
        jest.useFakeTimers();

        const props = getProps();

        const wrapper = shallow(<OnboardingBase {...props} />);

        // После рендеринга посылается метрика показа онбординга. Сбросим ее и будем ждать метрику закрытия.
        count.mockClear();

        wrapper.find(Button).at(0).simulate('click');

        expect(props.onClose).toBeCalledTimes(1);
        expect(count).toHaveBeenCalledWith(...METRIKA_PATH, 'TestContext', 'close');

        jest.runOnlyPendingTimers();

        expect(props.saveSettings).toBeCalledTimes(1);
        expect(props.saveSettings).toHaveBeenCalledWith({ key: 'testOnboardingSettings', value: '1' });

        expect(props.updateSettings).toBeCalledTimes(1);
        expect(props.updateSettings).toHaveBeenCalledWith({ wasShownOnboardingInSession: true });
    });

    it('on logo click should NOT send metrika, save settings and call onClose callback', () => {
        const props = getProps();

        const wrapper = shallow(<OnboardingBase {...props} />);

        // После рендеринга посылается метрика показа онбординга. Сбросим ее
        count.mockClear();

        wrapper.find(Logo).simulate('click');

        expect(props.onClose).not.toBeCalled();
        expect(count).not.toBeCalled();
        expect(props.saveSettings).not.toBeCalled();
    });
});
