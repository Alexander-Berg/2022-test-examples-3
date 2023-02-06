import { dateLag } from '../../../ui/FormatDate';
import { buildDiff } from '../buildDiff';
import { LiteConfirmationsGetData } from '../liteConfirmationsGetData';
import { ConfirmationType, TActionType, TBuildPropositionAction, TConfirmationType } from '../types';

describe('LiteConfirmations getData', () => {
    it('role - one proposition in response', () => {
        const mockBuildAction: TBuildPropositionAction = (
            propositionId: string,
            type: TConfirmationType,
        ) => (actionType: TActionType) => undefined;
        const oneRoleResponse = {
            "report": [
                {
                    "role_id": "aaaa-2",
                    "role_is_public": "0",
                    "slave_roles": [],
                    "role_groupping_tags": "test",
                    "role_group": "1",
                    "role_optional": "0",
                    "role_description": "тестовая роль",
                    "role_is_idm": "0",
                    "actions": [],
                },
            ],
            "propositions": [
                {
                    "role_id": "aaaa-2",
                    "confirmators": [
                        "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                    ],
                    "proposition_author": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                    "role_is_public": "0",
                    "confirmations_count": 1,
                    "confirmations": [
                        {
                            "comment": "test",
                            "history_instant": 1601279401,
                            "user_id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                        },
                    ],
                    "proposition_description": "test",
                    "proposition_id": "93e6ee7d-aa911458-2de5d6a3-fb6740e7",
                    "slave_roles": [],
                    "role_groupping_tags": "test",
                    "role_group": "1",
                    "role_optional": "0",
                    "role_description": "тестовая роль test",
                    "role_is_idm": "0",
                    "confirmations_need": 1,
                    "actions": [],
                },
            ],
            "users": {
                "6c5e1925-f4fd-4fee-a56c-39170b63a475": {
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
            },
        };
        const oneRoleData = {
            "id": "aaaa-2",
            "description": "тестовая роль",
            "type": "role",
            "propositions": [
                {
                    "historyInstant": 1601279401000,
                    // eslint-disable-next-line no-magic-numbers
                    "historyInstantLag": dateLag(1601279401000),
                    "description": "test",
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
                    "diff": undefined,
                    "propositionAction": mockBuildAction('93e6ee7d-aa911458-2de5d6a3-fb6740e7', ConfirmationType.ROLE),
                },
            ],
        };
        // @ts-ignore
        oneRoleData.propositions[0].diff = buildDiff(oneRoleResponse.report[0], oneRoleResponse?.propositions?.[0]);
        expect(JSON.stringify(
            LiteConfirmationsGetData(oneRoleResponse, ConfirmationType.ROLE, mockBuildAction, oneRoleData.id),
        )).toBe(JSON.stringify(oneRoleData));
    });
    it('new role - response has other propositions', () => {
        const mockBuildAction: TBuildPropositionAction = (
            propositionId: string,
            type: TConfirmationType,
        ) => (actionType: TActionType) => undefined;
        const newRoleResponse = {
            "report": [],
            "propositions": [
                {
                    "role_id": "test_confirm_test",
                    "confirmators": [
                        "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                    ],
                    "proposition_author": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                    "role_is_public": "0",
                    "confirmations_count": 1,
                    "confirmations": [
                        {
                            "comment": "testing for test",
                            "history_instant": 1601274872,
                            "user_id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                        },
                    ],
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
                },
                {
                    "role_id": "aaaa-2",
                    "confirmators": [
                        "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                    ],
                    "proposition_author": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                    "role_is_public": "0",
                    "confirmations_count": 1,
                    "confirmations": [
                        {
                            "comment": "test",
                            "history_instant": 1601279401,
                            "user_id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                        },
                    ],
                    "proposition_description": "test",
                    "proposition_id": "93e6ee7d-aa911458-2de5d6a3-fb6740e7",
                    "slave_roles": [],
                    "role_groupping_tags": "test",
                    "role_group": "1",
                    "role_optional": "0",
                    "role_description": "тестовая роль test",
                    "role_is_idm": "0",
                    "confirmations_need": 1,
                    "actions": [],
                },
            ],
            "users": {
                "6c5e1925-f4fd-4fee-a56c-39170b63a475": {
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
            },
        };
        const newRoleData = {
            "id": "test_confirm_test",
            "description": "testing_role_for_test",
            "type": "role",
            "propositions": [{
                "historyInstant": 1601274872000,
                // eslint-disable-next-line no-magic-numbers
                "historyInstantLag": dateLag(1601274872000),
                "description": "testing for test",
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
                "diff": undefined,
                "propositionAction": mockBuildAction('19cad5ee-fdd60509-32a8d5ba-823ae64f', ConfirmationType.ROLE),
            }],
        };
        // @ts-ignore
        newRoleData.propositions[0].diff = buildDiff(undefined, newRoleResponse?.propositions?.[0]);
        expect(JSON.stringify(
            LiteConfirmationsGetData(newRoleResponse, ConfirmationType.ROLE, mockBuildAction, newRoleData.id),
        )).toBe(JSON.stringify(newRoleData));
    });

    it('role - no matching propositions', () => {
        const mockBuildAction: TBuildPropositionAction = (
            propositionId: string,
            type: TConfirmationType,
        ) => (actionType: TActionType) => undefined;
        const noMatchRoleResponse = {
            "report": [],
            "propositions": [
                {
                    "role_id": "aaaa-2",
                    "confirmators": [
                        "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                    ],
                    "proposition_author": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                    "role_is_public": "0",
                    "confirmations_count": 1,
                    "confirmations": [
                        {
                            "comment": "test",
                            "history_instant": 1601279401,
                            "user_id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                        },
                    ],
                    "proposition_description": "test",
                    "proposition_id": "93e6ee7d-aa911458-2de5d6a3-fb6740e7",
                    "slave_roles": [],
                    "role_groupping_tags": "test",
                    "role_group": "1",
                    "role_optional": "0",
                    "role_description": "тестовая роль test",
                    "role_is_idm": "0",
                    "confirmations_need": 1,
                    "actions": [],
                },
            ],
            "users": {
                "6c5e1925-f4fd-4fee-a56c-39170b63a475": {
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
            },
        };
        const noMatchRoleId = "testing_role_for_test";
        const noMatchRoleData = null;
        expect(LiteConfirmationsGetData(noMatchRoleResponse, ConfirmationType.ROLE, mockBuildAction, noMatchRoleId))
            .toBe(noMatchRoleData);
    });

    it('action - one proposition', () => {
        const mockBuildAction: TBuildPropositionAction = (
            propositionId: string,
            type: TConfirmationType,
        ) => (actionType: TActionType) => undefined;
        const oneActionResponse = {
            "proposition_users": [
                {
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
            ],
            "report": [
                {
                    "deprecated": false,
                    "enabled": true,
                    "action_id": "test_schema_promo_bla_bla",
                    "propositions": [
                        {
                            "confirmators": [
                                "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                            ],
                            "proposition_author": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                            "deprecated": false,
                            "enabled": true,
                            "confirmations_count": 1,
                            "confirmations": [
                                {
                                    "comment": "testestest",
                                    "history_instant": 1600942535,
                                    "user_id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                                },
                            ],
                            "proposition_description": "testestest",
                            "proposition_id": "13d09e44-302cf99e-a63ef154-4fb6ce87",
                            "action_id": "test_schema_promo_bla_bla",
                            "action_description": "test schema test",
                            "action_type": "promo_generator_rides",
                            "action_meta": {
                                "user_undefined_position_policy": "AcceptUndefined",
                                "user_tags_in_point": [],
                                "groupping_tags": "",
                                "experiment": {
                                    "max_shard": 65536,
                                    "min_shard": 0,
                                },
                                "client_version_check_policy": "Default",
                                "promo_info": {
                                    "chat": {
                                        "chat_notifier": "",
                                    },
                                    "first_only": true,
                                    "attempts_count": 1,
                                    "meta": {
                                        "dictionary_context": {
                                            "parameters": [],
                                        },
                                    },
                                    "is_duplicate": false,
                                    "type": "attempts_discount",
                                    "is_disposable": true,
                                    "since": 0,
                                    "tag_name": "test_schema",
                                },
                            },
                            "confirmations_need": 1,
                            "action_revision": 4079,
                        },
                    ],
                    "action_description": "test schema",
                    "action_type": "promo_generator_rides",
                    "action_meta": {
                        "user_undefined_position_policy": "AcceptUndefined",
                        "user_tags_in_point": [],
                        "groupping_tags": "",
                        "experiment": {
                            "max_shard": 65536,
                            "min_shard": 0,
                        },
                        "client_version_check_policy": "Default",
                        "promo_info": {
                            "chat": {
                                "chat_notifier": "",
                            },
                            "first_only": true,
                            "attempts_count": 1,
                            "meta": {},
                            "is_duplicate": false,
                            "type": "attempts_discount",
                            "is_disposable": true,
                            "since": 0,
                            "tag_name": "test_schema",
                        },
                    },
                    "action_revision": 4079,
                },
            ],
            "report_deprecated": [],
        };
        const oneActionData = {
            "id": "test_schema_promo_bla_bla",
            "description": "test schema",
            "type": "action",
            "propositions": [
                {
                    "historyInstant": 1600942535000,
                    // eslint-disable-next-line no-magic-numbers
                    "historyInstantLag": dateLag(1600942535000),
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
                    "diff": undefined,
                    "propositionAction": mockBuildAction('13d09e44-302cf99e-a63ef154-4fb6ce87', ConfirmationType.ROLE),
                },
            ],
        };
        // @ts-ignore
        oneActionData.propositions[0].diff = buildDiff(
            oneActionResponse.report[0],
            oneActionResponse?.report[0]?.propositions?.[0],
        );
        expect(JSON.stringify(
            LiteConfirmationsGetData(oneActionResponse, ConfirmationType.ACTION, mockBuildAction, oneActionData.id),
        )).toBe(JSON.stringify(oneActionData));
    });

    it('action - no matching propositions', () => {
        const mockBuildAction: TBuildPropositionAction = (
            propositionId: string,
            type: TConfirmationType,
        ) => (actionType: TActionType) => undefined;
        const noMatchActionResponse = {
            "proposition_users": [
                {
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
            ],
            "report": [
                {
                    "deprecated": false,
                    "enabled": true,
                    "action_id": "test_schema_promo_bla_bla",
                    "propositions": [
                        {
                            "confirmators": [
                                "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                            ],
                            "proposition_author": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                            "deprecated": false,
                            "enabled": true,
                            "confirmations_count": 1,
                            "confirmations": [
                                {
                                    "comment": "testestest",
                                    "history_instant": 1600942535,
                                    "user_id": "6c5e1925-f4fd-4fee-a56c-39170b63a475",
                                },
                            ],
                            "proposition_description": "testestest",
                            "proposition_id": "13d09e44-302cf99e-a63ef154-4fb6ce87",
                            "action_id": "test_schema_promo_bla_bla",
                            "action_description": "test schema test",
                            "action_type": "promo_generator_rides",
                            "action_meta": {
                                "user_undefined_position_policy": "AcceptUndefined",
                                "user_tags_in_point": [],
                                "groupping_tags": "",
                                "experiment": {
                                    "max_shard": 65536,
                                    "min_shard": 0,
                                },
                                "client_version_check_policy": "Default",
                                "promo_info": {
                                    "chat": {
                                        "chat_notifier": "",
                                    },
                                    "first_only": true,
                                    "attempts_count": 1,
                                    "meta": {
                                        "dictionary_context": {
                                            "parameters": [],
                                        },
                                    },
                                    "is_duplicate": false,
                                    "type": "attempts_discount",
                                    "is_disposable": true,
                                    "since": 0,
                                    "tag_name": "test_schema",
                                },
                            },
                            "confirmations_need": 1,
                            "action_revision": 4079,
                        },
                    ],
                    "action_description": "test schema",
                    "action_type": "promo_generator_rides",
                    "action_meta": {
                        "user_undefined_position_policy": "AcceptUndefined",
                        "user_tags_in_point": [],
                        "groupping_tags": "",
                        "experiment": {
                            "max_shard": 65536,
                            "min_shard": 0,
                        },
                        "client_version_check_policy": "Default",
                        "promo_info": {
                            "chat": {
                                "chat_notifier": "",
                            },
                            "first_only": true,
                            "attempts_count": 1,
                            "meta": {},
                            "is_duplicate": false,
                            "type": "attempts_discount",
                            "is_disposable": true,
                            "since": 0,
                            "tag_name": "test_schema",
                        },
                    },
                    "action_revision": 4079,
                },
            ],
            "report_deprecated": [],
        };
        const noMatchActionId = "testing_role_for";
        const noMatchActionData = null;
        expect(LiteConfirmationsGetData(
            noMatchActionResponse,
            ConfirmationType.ACTION,
            mockBuildAction,
            noMatchActionId,
        )).toBe(noMatchActionData);
    });
});
