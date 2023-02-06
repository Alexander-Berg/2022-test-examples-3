import { mount } from 'enzyme';
import * as React from 'react';
import { Provider } from 'react-redux';
import { AnyAction, Store } from 'redux';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import { initRules, initState } from '../../../../reducers/adminUserReducer';
import { comReducers } from '../../../App/store';
import BillingViewHistory from './index';

const tags = { records: [{ "tag_display_name":"Оплата платной дороги","tag_id":"3a5cb871-8da7-411e-899d-6b737de01e9a","tag_details":{ "amount":10,"tag":"toll_road_charge","operations":[],"additional_accounts":[],"links":[],"session_id":"bla-bla","comment":"test","priority":0 },"action":"add","user_id":"6c5e1925-f4fd-4fee-a56c-39170b63a475","event_id":3746107,"object_id":"1087a921-5bbb-49c7-aaae-0e1c6170073d","tag_name":"toll_road_charge","timestamp":1596531897,"user_data_full":{ "last_name":"","setup":{ "phone":{ "verified":false,"number":"" },"email":{ "verified":false,"address":"akavaleva@yandex-team.ru" } },"first_name":"","id":"6c5e1925-f4fd-4fee-a56c-39170b63a475","preliminary_payments":{ "enabled":false,"amount":0 },"status":"onboarding","pn":"","username":"akavaleva" } }, { "tag_display_name":"Штраф: Управление ТС без госзнаков","tag_id":"023729b6-7fc3-4d75-9359-a8a8993ab626","tag_details":{ "amount":1100,"tag":"fine_pdd_12_02_2","operations":[],"additional_accounts":[],"links":[],"session_id":"edbd0ccc-b6bed6e1-4883294f-71a76020","comment":"test","priority":0 },"action":"add","user_id":"6c5e1925-f4fd-4fee-a56c-39170b63a475","event_id":3746101,"object_id":"1087a921-5bbb-49c7-aaae-0e1c6170073d","tag_name":"fine_pdd_12_02_2","timestamp":1596531226,"user_data_full":{ "last_name":"","setup":{ "phone":{ "verified":false,"number":"" },"email":{ "verified":false,"address":"akavaleva@yandex-team.ru" } },"first_name":"","id":"6c5e1925-f4fd-4fee-a56c-39170b63a475","preliminary_payments":{ "enabled":false,"amount":0 },"status":"onboarding","pn":"","username":"akavaleva" } }, { "tag_display_name":"Списание штрафа за пьяное вождение","tag_id":"47e1c6c8-6e1c-454e-be50-a757ea91b246","tag_details":{ "active_since":1574888400,"operations":[],"additional_accounts":[],"amount":30000,"priority":0,"tag":"fine_drunk_driver","links":[{ "uri":"link-blink","type":"st" }] },"action":"add","user_id":"6c5e1925-f4fd-4fee-a56c-39170b63a475","event_id":337323,"object_id":"1087a921-5bbb-49c7-aaae-0e1c6170073d","tag_name":"fine_drunk_driver","timestamp":1574848500,"user_data_full":{ "last_name":"","setup":{ "phone":{ "verified":false,"number":"" },"email":{ "verified":false,"address":"akavaleva@yandex-team.ru" } },"first_name":"","id":"6c5e1925-f4fd-4fee-a56c-39170b63a475","preliminary_payments":{ "enabled":false,"amount":0 },"status":"onboarding","pn":"","username":"akavaleva" } }, { "tag_display_name":"Снять 100 рублей","tag_id":"c593ea10-39b1-4f43-b222-a8fbc494d619","tag_details":{ "comment":"test","operations":[],"additional_accounts":[],"amount":10000,"priority":0,"tag":"simple_ticket_100","links":[] },"action":"add","user_id":"6c5e1925-f4fd-4fee-a56c-39170b63a475","event_id":562817,"object_id":"1087a921-5bbb-49c7-aaae-0e1c6170073d","tag_name":"simple_ticket_100","timestamp":1584528804,"user_data_full":{ "last_name":"","setup":{ "phone":{ "verified":false,"number":"" },"email":{ "verified":false,"address":"akavaleva@yandex-team.ru" } },"first_name":"","id":"6c5e1925-f4fd-4fee-a56c-39170b63a475","preliminary_payments":{ "enabled":false,"amount":0 },"status":"onboarding","pn":"","username":"akavaleva" } }] };

const data = [{ "timestamp":1582970992,"transactions":[],"billing_type":"ticket","meta":{},"bill":16680,"payments":[],"session_id":"59e01a2f-3f79-4e71-ac48-161d7a5e2b56" },{ "timestamp":1584528726,"transactions":[],"billing_type":"ticket","meta":{},"bill":1000,"payments":[],"session_id":"ad0a525e-7b34-4f78-915b-45c0bbfb612c" },{ "timestamp":1584528806,"transactions":[],"billing_type":"ticket","meta":{},"bill":10000,"payments":[],"session_id":"c593ea10-39b1-4f43-b222-a8fbc494d619" },{ "timestamp":1596531230,"transactions":[],"billing_type":"ticket","meta":{ "comment":"test","real_session_id":"edbd0ccc-b6bed6e1-4883294f-71a76020" },"bill":1100,"payments":[{ "payment_error":"","sum":"1100","account_id":"0","rrn":"256493","pay_method":"card-x55a40927ca8278e84324d7c0","billing_type":"ticket","id":"841474","session_id":"023729b6-7fc3-4d75-9359-a8a8993ab626","card_mask":"510000****9768","meta":"{\"cashback_percent\":0}","refunded":0,"cleared":"1100","status":"cleared","last_update_ts":"1596790439","payment_id":"6d7e1941131eb9e94545b49ab28efce2","wait_refund":0,"created_at_ts":"1596531230","order_id":"122125733","payment_type":"card","payment_error_desc":"" }],"session_id":"023729b6-7fc3-4d75-9359-a8a8993ab626" },{ "timestamp":1604917057,"transactions":[],"billing_type":"car_usage","meta":{ "real_session_id":"1e0681f2-fed82fb6-8f247a56-e8459971" },"bill":12860,"payments":[{ "payment_error":"","sum":"6600","account_id":"16605","rrn":"","pay_method":"bonus","billing_type":"car_usage","id":"1069929","session_id":"1e0681f2-fed82fb6-8f247a56-e8459971","card_mask":"","meta":"[{\"source\":\"add_bonus_tag\",\"sum\":6600}]","refunded":0,"cleared":"6600","status":"cleared","last_update_ts":"1604917057","payment_id":"b_f38716e2-ba839beb-ecbdd6e5-d94bb094","wait_refund":0,"created_at_ts":"1604917057","order_id":"","payment_type":"bonus","payment_error_desc":"" },{ "payment_error":"","sum":"6260","account_id":"16605","rrn":"","pay_method":"bonus","billing_type":"car_usage","id":"1070142","session_id":"1e0681f2-fed82fb6-8f247a56-e8459971","card_mask":"","meta":"[{\"source\":\"add_bonus_tag\",\"sum\":6260}]","refunded":0,"cleared":"6260","status":"cleared","last_update_ts":"1604918638","payment_id":"b_819e10e0-db5f1fc1-975954f8-ffa2655a","wait_refund":0,"created_at_ts":"1604918638","order_id":"","payment_type":"bonus","payment_error_desc":"" }],"session_id":"1e0681f2-fed82fb6-8f247a56-e8459971" },{ "timestamp":1596531898,"transactions":[],"billing_type":"toll_road","meta":{ "comment":"test","real_session_id":"bla-bla" },"bill":10,"payments":[],"session_id":"3a5cb871-8da7-411e-899d-6b737de01e9a" },{ "timestamp":1592837434,"transactions":[],"billing_type":"ticket_DTP","meta":{},"bill":1,"payments":[],"session_id":"410e5347-cb60-40b8-8451-be8e6291e090" },{ "timestamp":1592837384,"transactions":[],"billing_type":"ticket","meta":{},"bill":1,"payments":[],"session_id":"8604fa43-bdbe-4df4-8984-dad7f8bdffd0" },{ "timestamp":1592827768,"transactions":[],"billing_type":"car_usage","meta":{},"bill":1100,"payments":[{ "payment_error":"","sum":"12222","account_id":"16605","rrn":"","pay_method":"bonus","billing_type":"car_usage","id":"663614","session_id":"a8ff5d70-2bc41bc8-ff514582-fb2b51a0","card_mask":"","meta":"[{\"deadline\":1592427600,\"source\":\"add_bonus_tag_2week\",\"sum\":1100}]","refunded":0,"cleared":"1100","status":"cleared","last_update_ts":"1592827843","payment_id":"b_e1052108-61faf5c4-b7e7c94-64724533","wait_refund":0,"created_at_ts":"1592827768","order_id":"","payment_type":"bonus","payment_error_desc":"" },{ "payment_error":"","sum":"14878","account_id":"0","rrn":"194059","pay_method":"card-x55a40927ca8278e84324d7c0","billing_type":"car_usage","id":"663615","session_id":"a8ff5d70-2bc41bc8-ff514582-fb2b51a0","card_mask":"510000****9768","meta":"{\"cashback_percent\":0}","refunded":0,"cleared":"0","status":"canceled","last_update_ts":"1592827845","payment_id":"2eb5927e12a932ac07bda1978cfc1882","wait_refund":0,"created_at_ts":"1592827776","order_id":"117947679","payment_type":"card","payment_error_desc":"" }],"session_id":"a8ff5d70-2bc41bc8-ff514582-fb2b51a0" },{ "timestamp":1593678848,"transactions":[],"billing_type":"car_usage","meta":{},"bill":300,"payments":[{ "payment_error":"","sum":"16480","account_id":"16605","rrn":"","pay_method":"bonus","billing_type":"car_usage","id":"694609","session_id":"af09efdf-9dbf6a1a-761d85b4-17707335","card_mask":"","meta":"[{\"deadline\":1592427600,\"source\":\"add_bonus_tag_2week\",\"sum\":300}]","refunded":0,"cleared":"300","status":"cleared","last_update_ts":"1593678885","payment_id":"b_f56c09ca-7567fe70-accce02-b4524676","wait_refund":0,"created_at_ts":"1593678848","order_id":"","payment_type":"bonus","payment_error_desc":"" },{ "payment_error":"","sum":"10620","account_id":"0","rrn":"996228","pay_method":"card-x55a40927ca8278e84324d7c0","billing_type":"car_usage","id":"694612","session_id":"af09efdf-9dbf6a1a-761d85b4-17707335","card_mask":"510000****9768","meta":"{\"cashback_percent\":0}","refunded":0,"cleared":"0","status":"canceled","last_update_ts":"1593678887","payment_id":"ddbfcec1b32f7902942de10bcea004f4","wait_refund":0,"created_at_ts":"1593678850","order_id":"118484258","payment_type":"card","payment_error_desc":"" }],"session_id":"af09efdf-9dbf6a1a-761d85b4-17707335" },{ "timestamp":1592837288,"transactions":[],"billing_type":"ticket","meta":{},"bill":500000,"payments":[{ "payment_error":"","sum":"500000","account_id":"0","rrn":"959552","pay_method":"card-x55a40927ca8278e84324d7c0","billing_type":"ticket","id":"673656","session_id":"ba68f8a1-e67d-4bc5-9702-edf8ddb541a2","card_mask":"510000****9768","meta":"{\"cashback_percent\":0}","refunded":0,"cleared":"500000","status":"cleared","last_update_ts":"1593099492","payment_id":"2fc8db745378a96fb7cbd014a07b3b0e","wait_refund":0,"created_at_ts":"1592837288","order_id":"117952949","payment_type":"card","payment_error_desc":"" }],"session_id":"ba68f8a1-e67d-4bc5-9702-edf8ddb541a2" },{ "timestamp":1593678709,"transactions":[],"billing_type":"car_usage","meta":{},"bill":0,"payments":[],"session_id":"e10eec2e-726feb91-5112325b-4623de01" }];

const buildMockStore = (): Store<any, AnyAction> => {
    const mockStore = configureMockStore([thunk]);
    const globalState: any = {
        AdminUser: initState,
    };
    const state = comReducers(globalState, initRules({ Refound: true }, null));

    return mockStore(state);
};

describe('BillingViewHistory', () => {
    it('should display all data with required-only props', () => {
        const component = mount(
            <Provider store={buildMockStore()}>
                <BillingViewHistory data={data}
                                    reloadData={jest.fn()}
                                    userId={'1087a921-5bbb-49c7-aaae-0e1c6170073d'}/>
            </Provider>,
        );

        expect(component).toMatchSnapshot();
    });

    it('should display all data with required-only props and empty data', () => {
        const component = mount(
            <Provider store={buildMockStore()}>
                <BillingViewHistory data={[]}
                                    reloadData={jest.fn()}
                                    userId={'1087a921-5bbb-49c7-aaae-0e1c6170073d'}/>
            </Provider>,
        );

        expect(component).toMatchSnapshot();
    });

    it('should display all data with all props', () => {
        const component = mount(
            <Provider store={buildMockStore()}>
                <BillingViewHistory tags={tags}
                                    timelapse={'c 1 по 42 апреля'}
                                    currentFilter={'ticket_gibdd'}
                                    data={data}
                                    reloadData={jest.fn()}
                                    userId={'1087a921-5bbb-49c7-aaae-0e1c6170073d'}/>
            </Provider>,
        );

        expect(component).toMatchSnapshot();
    });

    it('BlockRules is connected', () => {
        const component = mount(
            <Provider store={buildMockStore()}>
                <BillingViewHistory data={data}
                                    reloadData={jest.fn()}
                                    userId={'1087a921-5bbb-49c7-aaae-0e1c6170073d'}/>
            </Provider>,
        );

        expect(component).toMatchSnapshot();
    });
});
