import React from 'react';
import { shallow } from 'enzyme';

import AbcGap from 'b:abc-gap';

jest.mock('../../../../common/components/DateInterval/DateInterval');

describe('Should render absences', () => {
    it('Should render absence', () => {
        const wrapper = shallow(
            <AbcGap
                type="absence"
                from={new Date(Date.UTC(2019, 1, 1))}
                to={new Date(Date.UTC(2019, 1, 14))}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should make interval for full-day gap non inclusive', () => {
        const wrapper = shallow(
            <AbcGap
                type="absence"
                from={new Date(Date.UTC(2019, 1, 1))}
                to={new Date(Date.UTC(2019, 1, 14))}
                fullDay
            />
        );

        expect(wrapper).toMatchSnapshot(); // до 2019-01-13
        wrapper.unmount();
    });

    describe('Absence with work note', () => {
        it('Note when will work', () => {
            const wrapper = shallow(
                <AbcGap
                    type="illness"
                    from={new Date(Date.UTC(2019, 1, 1))}
                    to={new Date(Date.UTC(2019, 1, 14))}
                    workInAbsence
                />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });

        it('Note when will not work', () => {
            const wrapper = shallow(
                <AbcGap
                    type="trip"
                    from={new Date(Date.UTC(2019, 1, 1))}
                    to={new Date(Date.UTC(2019, 1, 14))}
                    workInAbsence={false}
                />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });
    });
});

describe('Should return absence text representation', () => {
    it('Full caption', () => {
        const start = new Date(Date.UTC(2012, 1, 1));
        const end = new Date(Date.UTC(2012, 1, 29));

        expect(AbcGap.getCaption('vacation', start, end, true, true))
            .toEqual('1328054400000,1330473600000,false,false,words,true abc-gap:vacation (abc-gap:will-work)');
    });
});
