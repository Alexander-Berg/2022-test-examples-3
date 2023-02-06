import * as React from 'react';
import { shallow } from 'enzyme';
import { ESportResultStatus, ESportTeamType, ESportEndStatus } from '@yandex-turbo/components/SportResult/SportResult.types';
import { SportCompetition } from '../SportCompetition';

const match = {
    url: '#',
    teams: [
        {
            name: 'Ривер Плейт',
            logo: '//storage.mds.yandex.net/get-sport/67389/8ff44b0a7c5928686a9ec8d7fe999fee.png',
            type: ESportTeamType.CLUB,
        },
        {
            name: 'Ривер Плейт',
            logo: '//storage.mds.yandex.net/get-sport/67389/8ff44b0a7c5928686a9ec8d7fe999fee.png',
            type: ESportTeamType.CLUB,
        },
    ],
    result: [0, 0],
    resultAdditional: [0, 0],
    status: ESportResultStatus.IN_PROGRESS,
    endStatus: ESportEndStatus.REGULAR,
    startDate: 1550755230000,
};

describe('SportCompetition component', () => {
    it('should render title', () => {
        const wrapper = shallow(
            <SportCompetition
                title="Чемпионат России по футболу"
                matches={[match]}
            />
        );
        expect(wrapper.find('.sport-competition__title').childAt(0).text()).toEqual('Чемпионат России по футболу');
    });

    it('should render phase', () => {
        const wrapper = shallow(
            <SportCompetition
                title="Чемпионат России по футболу"
                phase="1/32 финала"
                matches={[match]}
            />
        );
        expect(wrapper.find('.sport-competition__phase').childAt(0).text()).toEqual('1/32 финала');
    });
});
