import * as React from 'react';
import { shallow } from 'enzyme';

import { ApiIotUnit } from 'types/api';

import { IotUnit } from './index';

describe('IotUnit', () => {
    it('Должно корректно отрендериться величина в градусах Цельсия', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.CELSIUS} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в кельвинах', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.KELVIN} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в процентах', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.PERCENT} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина на миллион', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.PPM} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в вольтах', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.VOLT} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в амперах', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.AMPERE} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в ваттах', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.WATT} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в атмосферах', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.ATM} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в барах', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.BAR} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в Паскалях', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.PASCAL} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в миллиметрах ртутного столба', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.MMHG} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в мг/м3', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.MG_M3} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в мкг/м3', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.MCG_M3} />)).toMatchSnapshot();
    });
    it('Должно корректно отрендериться величина в люксах', () => {
        expect(shallow(<IotUnit unit={ApiIotUnit.LUX} />)).toMatchSnapshot();
    });
});
