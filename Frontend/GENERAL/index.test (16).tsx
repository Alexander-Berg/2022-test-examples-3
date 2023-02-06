import * as React from 'react';
import { shallow } from 'enzyme';

import { ApiIotUnit } from 'types/api';

import { IotValueWithUnit } from './index';

describe('IotValueWithUnit', () => {
    it('Должно корректно отрендериться безразмерная величина', () => {
        expect(shallow(<IotValueWithUnit value={123.456} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться значение в градусах Цельсия', () => {
        expect(shallow(<IotValueWithUnit value={238.333} unit={ApiIotUnit.CELSIUS} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться значение в кельвинах', () => {
        expect(shallow(<IotValueWithUnit value={291.111001} unit={ApiIotUnit.KELVIN} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться значение в процентах', () => {
        expect(shallow(<IotValueWithUnit value={89} unit={ApiIotUnit.PERCENT} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться частиц на миллион', () => {
        expect(shallow(<IotValueWithUnit value={2336} unit={ApiIotUnit.PPM} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться значение в вольтах', () => {
        expect(shallow(<IotValueWithUnit value={223.1} unit={ApiIotUnit.VOLT} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться значение в амперах', () => {
        expect(shallow(<IotValueWithUnit value={0.32} unit={ApiIotUnit.AMPERE} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться значение в ваттах', () => {
        expect(shallow(<IotValueWithUnit value={1344.82} unit={ApiIotUnit.WATT} />)).toMatchSnapshot();
    });
});
