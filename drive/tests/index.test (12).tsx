/*eslint-disable*/
import * as React from 'react';
import {
    SessionCity,
    SessionDuration,
    SessionFinish,
    SessionMileage,
    SessionOfferName,
    SessionStatus,
    SessionTotalPrice
} from '../index';
import mockData from './mock-data';
import { render } from 'enzyme';
import { CITIES } from '../../../../constants';
import { deepCopy } from '../../../../utils/utils';
import { MockSession, MockSessionHandler } from './sessionMockFunction';
import { DELEGATION_TYPE } from '../../../../models/session';
import { SessionBadges } from '../badges';

let index = 0;
describe('Session list items', () => {
    describe('snapshots: badges', () => {
        let temp: MockSession;
        let mockSessionHandler: MockSessionHandler;
        beforeEach(() => {
            index = 0;
            temp = new MockSession({has_more: false});
            mockSessionHandler = new MockSessionHandler(temp);
        });
        afterEach(() => {
            mockSessionHandler.clear();
        });

        it(`0 empty`, () => {
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`1 bluetooth`, () => {
            mockSessionHandler.makeBluetoothBadge();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`2 corp`, () => {
            mockSessionHandler.makeCorpBadge();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`2.1 yandex account`, () => {
            mockSessionHandler.makeYac();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`3 delegation`, () => {
            let sessionIndex = 0;
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setDelegationType(sessionIndex, DELEGATION_TYPE.p2p_pass_offer);
            mockSessionHandler.setTransferredType(sessionIndex, 2);
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`4 delegation from`, () => {
            let sessionIndex = 0;
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setDelegationType(sessionIndex, DELEGATION_TYPE.p2p_pass_offer);
            mockSessionHandler.setTransferredType(sessionIndex, 2);
            mockSessionHandler.setTransferredFrom(sessionIndex);
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`5 free`, () => {
            let sessionIndex = 0;
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setDelegationType(sessionIndex, DELEGATION_TYPE.free);
            mockSessionHandler.setTransferredType(sessionIndex, 0);

            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`6 fee`, () => {
            mockSessionHandler.makeFee();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`7 taxi`, () => {
            mockSessionHandler.makeTaxi();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`voyage`, () => {
            mockSessionHandler.makeVoyage();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`8 long term`, () => {
            mockSessionHandler.makeLongTerm();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`9 double session`, () => {
            mockSessionHandler.makeDouble();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`offer evolution`, () => {
            mockSessionHandler.makeEvolution();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
    describe('snapshots: insurance', () => {
        let temp: MockSession;
        let mockSessionHandler: MockSessionHandler;

        beforeEach(() => {
            index = 0;
            temp = new MockSession({has_more: false});
            mockSessionHandler = new MockSessionHandler(temp);
        });

        it(`with insurance`, () => {
            let tagsIsLoading = true;
            let tagsObject = {};
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.generateInsurance();
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}
                               tagIsLoading={tagsIsLoading} tagsObject={tagsObject}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`without insurance`, () => {
            let tagsIsLoading = true;
            let tagsObject = {};
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}
                               tagIsLoading={tagsIsLoading} tagsObject={tagsObject}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        afterEach(() => {
            mockSessionHandler.clear();
        });
    });
    describe('snapshots: feedback', () => {
        let temp: MockSession;
        let mockSessionHandler: MockSessionHandler;
        let traceTags = ['1', '2', '3'];
        let sessionIndex = 0;
        beforeEach(() => {
            index = 0;
            temp = new MockSession({has_more: false});
            mockSessionHandler = new MockSessionHandler(temp);
        });
        afterEach(() => {
            mockSessionHandler.clear();
        });
        it(`without translation (still loading)`, () => {
            let tagsIsLoading = true;
            let tagsObject = {};
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setTraceTags(sessionIndex, traceTags);
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}
                               tagIsLoading={tagsIsLoading} tagsObject={tagsObject}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`with translation`, () => {
            let tagsIsLoading = false;
            let tagsObject = {'1': {comment: 'one'}, '2': {comment: 'two'}};
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setTraceTags(sessionIndex, traceTags);
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}
                               tagIsLoading={tagsIsLoading} tagsObject={tagsObject}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
    describe('snapshots: scoring trace', () => {
        let temp: MockSession;
        let mockSessionHandler: MockSessionHandler;
        let traceTags = ['1', 'scoring_trace_tag', '3'];
        let sessionIndex = 0;
        beforeEach(() => {
            index = 0;
            temp = new MockSession({has_more: false});
            mockSessionHandler = new MockSessionHandler(temp);
        });
        afterEach(() => {
            mockSessionHandler.clear();
        });
        it(`scoring trace`, () => {
            let tagsIsLoading = true;
            let tagsObject = {};
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setTraceTags(sessionIndex, traceTags);
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}
                               tagIsLoading={tagsIsLoading} tagsObject={tagsObject}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`with translation`, () => {
            let tagsIsLoading = false;
            let tagsObject = {'1': {comment: 'one'}, '2': {comment: 'two'}};
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setTraceTags(sessionIndex, traceTags);
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}
                               tagIsLoading={tagsIsLoading} tagsObject={tagsObject}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
    describe('snapshots: speeding trace tag', () => {
        let temp: MockSession;
        let mockSessionHandler: MockSessionHandler;
        let traceTags = ['1', 'speeding_trace_tag', '3'];
        let sessionIndex = 0;
        beforeEach(() => {
            index = 0;
            temp = new MockSession({has_more: false});
            mockSessionHandler = new MockSessionHandler(temp);
        });
        afterEach(() => {
            mockSessionHandler.clear();
        });
        it(`speeding trace tag`, () => {
            let tagsIsLoading = true;
            let tagsObject = {};
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setTraceTags(sessionIndex, traceTags);
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}
                               tagIsLoading={tagsIsLoading} tagsObject={tagsObject}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`with translation`, () => {
            let tagsIsLoading = false;
            let tagsObject = {'1': {comment: 'one'}, '2': {comment: 'two'}};
            mockSessionHandler.generateCommonSession();
            mockSessionHandler.setTraceTags(sessionIndex, traceTags);
            const wrapper = render(
                <SessionBadges index={index} sessionsData={temp}
                               tagIsLoading={tagsIsLoading} tagsObject={tagsObject}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
    describe('snapshots: mileage', () => {
        let temp: any;
        beforeEach(() => {
            index = 0;
            temp = deepCopy(mockData);
        });

        it('empty', () => {
            const wrapper = render(
                <SessionMileage index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('less 1km', () => {
            index = 1;
            temp.sessions[index].device_diff.hr_mileage = '0 м';
            temp.sessions[index].device_diff.mileage = 0;
            temp.sessions[index].device_diff.start.latitude = 55.73218536;
            temp.sessions[index].device_diff.finish.latitude = 54.73218536;
            const wrapper = render(
                <SessionMileage index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it('usual 10 km', () => {
            index = 2;
            temp.sessions[index].device_diff.hr_mileage = '10.0 кm';
            temp.sessions[index].device_diff.mileage = 10;
            const wrapper = render(
                <SessionMileage index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
    describe('snapshots: finish', () => {
        let temp: any;
        beforeEach(() => {
            index = 0;
            temp = deepCopy(mockData);
        });

        it('is finished', () => {
            const wrapper = render(
                <SessionFinish index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
        it(`status: session finished`, () => {
            const wrapper = render(
                <SessionStatus index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`has diff status `, () => {
            index = 1;
            const wrapper = render(
                <SessionFinish index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`status: not finished session `, () => {
            index = 1;
            const wrapper = render(
                <SessionStatus index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

    });
    describe('snapshots: duration', () => {
        let temp: any;
        beforeEach(() => {
            index = 0;
            temp = deepCopy(mockData);
        });

        it('less a day', () => {
            const wrapper = render(
                <SessionDuration index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`more then day`, () => {
            index = 2;
            const wrapper = render(
                <SessionDuration index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
    describe('snapshots: city', () => {
        let temp: any;
        beforeEach(() => {
            index = 0;
            temp = deepCopy(mockData);
        });
        let cities = Object.keys(CITIES);
        it('1 city', () => {
            const wrapper = render(
                <SessionCity index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`2 cities`, () => {
            temp.sessions[index].start_geo_tags = [cities[0]];
            temp.sessions[index].geo_tags = [cities[1]];
            const wrapper = render(
                <SessionCity index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });

        it(`new city`, () => {
            temp.sessions[index].start_geo_tags = [cities[7]];
            temp.sessions[index].geo_tags = [cities[7]];
            const wrapper = render(
                <SessionCity index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
    describe('snapshots: offer / bill', () => {

        let temp: any;
        beforeEach(() => {
            index = 5;
            temp = deepCopy(mockData);
        });
        it('some offer', () => {
            const wrapper = render(
                <SessionOfferName index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
        it('some bill', () => {
            const wrapper = render(
                <SessionTotalPrice index={index} sessionsData={temp}/>
            );
            expect(wrapper).toMatchSnapshot();
        });
    });
});


