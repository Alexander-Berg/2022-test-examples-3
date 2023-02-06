import { shallow } from 'enzyme';
import * as React from 'react';

import { LiteConfirmationsItem } from '../index';

describe('LiteConfirmationsItem', () => {
    const proposition = {
        "historyInstant": 1600942535000,
        "historyInstantLag": "3.9 дн.",
        "description": "testestest",
        "author": {
            "driving_license_revision": "",
            "last_name": "",
            "setup": {
                "phone": {
                    "verified": false,
                    "number": "",
                },
                "email": {
                    "verified": false,
                    "address": "akavaleva@yandex-team.ru",
                },
            },
            "uid": "1120000000133252",
            "is_first_riding": true,
            "first_name": "",
            "id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
            "passport_revision": "",
            "preliminary_payments": {
                "enabled": false,
                "amount": 0,
            },
            "is_mechanic_transmission_allowed": true,
            "registration": {
                "approved_at": 0,
                "joined_at": 1570017562,
            },
            "status": "onboarding",
            "pn": "",
            "username": "akavaleva",
        },
        "diff": [
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
        ],
        propositionAction: jest.fn(),
    };

    const longProposition = {
        "historyInstant": 1600942535000,
        "historyInstantLag": "3.9 дн.",
        "description": "testestest",
        "author": {
            "driving_license_revision": "",
            "last_name": "",
            "setup": {
                "phone": {
                    "verified": false,
                    "number": "",
                },
                "email": {
                    "verified": false,
                    "address": "akavaleva@yandex-team.ru",
                },
            },
            "uid": "1120000000133252",
            "is_first_riding": true,
            "first_name": "",
            "id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
            "passport_revision": "",
            "preliminary_payments": {
                "enabled": false,
                "amount": 0,
            },
            "is_mechanic_transmission_allowed": true,
            "registration": {
                "approved_at": 0,
                "joined_at": 1570017562,
            },
            "status": "onboarding",
            "pn": "",
            "username": "akavaleva",
        },
        "diff": [
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
            {
                "kind": "E",
                "path": [
                    "action_description",
                ],
                "lhs": "test schema",
                "rhs": "test schema test",
            },
            {
                "kind": "N",
                "path": [
                    "action_meta",
                    "promo_info",
                    "meta",
                    "dictionary_context",
                ],
                "rhs": {
                    "parameters": [],
                },
            },
        ],
        propositionAction: jest.fn(),
    };

    it('snapshot', () => {
        const diffItem = shallow(<LiteConfirmationsItem proposition={proposition}/>);
        expect(diffItem).toMatchSnapshot();
    });

    it('snapshot with long diff', () => {
        const diffItem = shallow(<LiteConfirmationsItem proposition={longProposition}/>);
        expect(diffItem).toMatchSnapshot();
    });

});
