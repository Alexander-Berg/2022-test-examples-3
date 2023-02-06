import { shallow } from 'enzyme';
import * as React from 'react';

import { CarIndicatorsBlock, ICarIndicatorsBlockProps } from './index';

const mockProps = {
    'carInfo': {
        'telematics': {
            acc_voltage: 12,
            ext_voltage: 12,
            fuel_level: 10,
            is_engine_on: false,
            mileage: 110,
        },
        'models': [{ fuel_type: 95 }],
    },
    'wrapperStyle': 'info_row',
    'itemStyle': 'telematics_info_item',
    'iconStyle': 'telematics_icon',
    'hasTooltip': true,
    'allItems': true,
};

const mockPropsWithGas = {
    'carInfo': {
        'telematics': {
            acc_voltage: 12,
            ext_voltage: 12,
            fuel_level: 10,
            second_fuel_level: 10,
            second_fuel_type: 'gas',
            is_engine_on: false,
            mileage: 110,
        },
        'models': [{ fuel_type: 95 }],
    },
    'wrapperStyle': 'info_row',
    'itemStyle': 'telematics_info_item',
    'iconStyle': 'telematics_icon',
    'hasTooltip': true,
    'allItems': true,
};

const setUp = (props?: ICarIndicatorsBlockProps) => shallow(<CarIndicatorsBlock {...props}/>);

describe('CarIndicatorsBlock', () => {
    it('Should render CarIndicatorsBlock with all indicators', () => {
        const component = setUp(mockProps);
        expect(component).toMatchSnapshot();
    });

    it('Should render CarIndicatorsBlock with all indicators and gas', () => {
        const component = setUp(mockPropsWithGas);
        expect(component).toMatchSnapshot();
    });

    it('Should render CarIndicatorsBlock without indicators', () => {
        const component = setUp();
        expect(component).toMatchSnapshot();
    });

});
