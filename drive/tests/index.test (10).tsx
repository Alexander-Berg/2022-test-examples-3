import { shallow } from 'enzyme';
import * as React from 'react';

import { LiteConfirmationsItemDiff, manipulateDiff } from '../index';

describe('LiteConfirmations item diff', (() => {
    const diff = {
        "role_id": "test_confirm_test",
        "confirmators": ["6c5e1925-f4fd-4fee-a56c-39170b63a475"],
        "proposition_author": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
        "role_is_public": "0",
        "confirmations_count": 1,
        "confirmations": [{
            "comment": "testing for test",
            "history_instant": 1601274872,
            "user_id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
        }],
        "proposition_description": "testing for test",
        "proposition_id": "19cad5ee-fdd60509-32a8d5ba-823ae64f",
        "slave_roles": [],
        "role_groupping_tags": "",
        "role_group": "0",
        "role_optional": "0",
        "role_description": "testing_role_for_test",
        "role_is_idm": "0",
        "confirmations_need": 1,
        "actions": [
            {
                "action_id": "bmw_msc_pricing",
                "role_action_meta": {},
                "role_id": "test_confirm_test",
            },
            {
                "action_id": "bonus",
                "role_action_meta": {},
                "role_id": "test_confirm_test",
            },
            {
                "action_id": "bonus_tag_promo",
                "role_action_meta": {},
                "role_id": "test_confirm_test",
            },
            {
                "action_id": "book_from_offer_holder",
                "role_action_meta": {},
                "role_id": "test_confirm_test",
            },
            {
                "action_id": "broken_hand_breake",
                "role_action_meta": {},
                "role_id": "test_confirm_test",
            },
            {
                "action_id": "build_promo_test",
                "role_action_meta": {},
                "role_id": "test_confirm_test",
            },
        ],
    };

    it('short diff snapshot', () => {
        const diffItem = shallow(<LiteConfirmationsItemDiff diff={diff}/>);
        expect(diffItem).toMatchSnapshot();
    });

    it('short diff manipulation', () => {
        const diffManipulated = manipulateDiff(diff, false);
        expect(diffManipulated).toBe('{"role_id":"test_confirm_test","confirmators":["6c');
    });

    it('long diff manipulation', () => {
        const diffManipulated = manipulateDiff(diff, true);
        const space = 2;
        expect(diffManipulated).toBe(JSON.stringify(diff, null, space));
    });

    it('manipulateDiff - number', () => {
        const number = 1980;
        const diffManipulated = manipulateDiff(number, true);
        expect(diffManipulated).toBe(number.toString());
    });

    it('manipulateDiff - zero', () => {
        const number = 0;
        const diffManipulated = manipulateDiff(number, true);
        expect(diffManipulated).toBe(number.toString());
    });

    it ('manipulateDiff - boolean - false', () => {
        const boolean = false;
        const diffManipulated = manipulateDiff(boolean, true);
        expect(diffManipulated).toBe(boolean.toString());
    });

    it ('manipulateDiff - boolean', () => {
        const boolean = true;
        const diffManipulated = manipulateDiff(boolean, true);
        expect(diffManipulated).toBe(boolean.toString());
    });
}));
