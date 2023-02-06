import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruBalloon } from '../BeruBalloon';

describe('BeruBalloon', () => {
    it('рендерится без ошибок', () => {
        expect(shallow(<BeruBalloon />)).toHaveLength(1);
    });

    it('отображается переданное значение счетчика', () => {
        expect(shallow(<BeruBalloon counter={15} />).text()).toEqual('15');
    });

    it('корректно прокидывается className на корневую ноду', () => {
        expect(shallow(<BeruBalloon className="test" />).hasClass('test')).toEqual(true);
    });
});
