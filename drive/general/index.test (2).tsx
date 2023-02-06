import { mount, render } from 'enzyme';
import * as React from 'react';

import CarInfo from "../../models/car";
import CarNumber from "./index";

const carNumber = (props) => mount(<CarNumber {...props}/>);

describe('Car Number Tests', () => {
    it('Should render component', () => {
        const component = render(
            <CarNumber carInfo={{ number: 'а661аа777' } as typeof CarInfo} externalTooltipId='car-tooltip' />,
        );

        expect(component).toMatchSnapshot();
    });

    it('Should render component with empty carInfo', () => {
        const component = render(
            <CarNumber carInfo={{} as typeof CarInfo} externalTooltipId='car-tooltip' />,
        );

        expect(component).toMatchSnapshot();
    });

    it('Should copy number by click on Copy', () => {
        document.execCommand = jest.fn();

        const MOCK_PROPS = {
            carInfo: {
                number: 'а661аа777',
                former_numbers: [
                    {
                        number: 'а661аа778',
                    },
                ],
            } as typeof CarInfo,
        };

        const mountedComponent = carNumber(MOCK_PROPS);

        mountedComponent.find('Copy [text="а661аа777"]').simulate('click');
        expect(document.execCommand).toHaveBeenCalledWith('copy');
    });
});
