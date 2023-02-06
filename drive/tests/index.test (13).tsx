/* eslint-disable react/jsx-indent-props */
import { shallow } from 'enzyme';
import * as React from 'react';

import { ONE_SECOND } from '../../../../constants';
import { SessionsListHeader } from '../index';

describe('Session list header', () => {
    const props = {
        isWorking: false,
        downloadPassportInfo: jest.fn(),
        downloadCarSessions: jest.fn(),
        onChangeDates: jest.fn(),
    };
    const user = {
        userId: '123',
        carId: null,
    };
    const car = {
        userId: null,
        carId: '456',
    };
    const pdn = (flag: boolean) => ({ ...{ BlockRules: { 'PassportDownload': flag } } });
    const session = (flag: boolean) => ({ ...{ BlockRules: { 'SessionsDownload': flag } } });

    it('user change date', () => {
        const date = new Date().getTime();
        const calledDate = Math.floor(date / ONE_SECOND);
        const calledTimes = 2;
        const header = shallow(<SessionsListHeader {...props} {...user} {...pdn(true)}/>);
        const instance: any = header.instance();
        expect(header.state()).toEqual({ since: null, until: null });
        instance?.changeDate('since', date);
        expect(props.onChangeDates).toBeCalledWith('since', calledDate);
        instance?.changeDate('until', date);
        expect(props.onChangeDates).toBeCalledTimes(calledTimes);
        expect(header.state()).toEqual({ since: date, until: date });
    });

    it('changed href', () => {
        const date = new Date().getTime();
        const calledDate = Math.floor(date / ONE_SECOND);
        const header = shallow(<SessionsListHeader {...props} {...user} {...pdn(true)}/>);
        const instance: any = header.instance();
        expect(instance?.getTrackUrl(calledDate))
            .toEqual(`#/tracks?since=${calledDate}&user_id=${user.userId}&status=`);
        const spyFunction = jest.spyOn(instance, 'showUserTracksHistory');
        instance.showUserTracksHistory();
        expect(spyFunction).toBeCalled();
    });

    it('user with ПДН', () => {
        const header = shallow(<SessionsListHeader {...props} {...user} {...pdn(true)}/>);
        expect(header).toMatchSnapshot();
    });

    it('user without ПДН', () => {
        const header = shallow(<SessionsListHeader {...props} {...user} {...pdn(false)}/>);
        expect(header).toMatchSnapshot();
    });
    it('car without ПДН', () => {
        const header = shallow(<SessionsListHeader {...props} {...car} {...pdn(false)}/>);
        expect(header).toMatchSnapshot();
    });
    it('car with ПДН', () => {
        const header = shallow(<SessionsListHeader {...props} {...car} {...pdn(true)}/>);
        expect(header).toMatchSnapshot();
    });

    it('car without ПДН with session', () => {
        const header = shallow(<SessionsListHeader {...props}
            {...car}
            {...pdn(false)}
            {...session(true)}/>);
        expect(header).toMatchSnapshot();
    });
});
