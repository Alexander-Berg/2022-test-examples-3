import { AliceDocResponse } from './index';
import { OpenUriAction } from '../actions';

describe('AliceDocResponse', () => {
    test('should serialize Goodwin answer', () => {
        const response = new AliceDocResponse('feature', 'Текстовый запрос', 'Голосовой запрос');
        const expected = {
            on_submit: {},
            scenario_response: {
                frame_actions: {},
                analytics_info: {
                    intent: 'feature',
                },
                layout: {
                    cards: [
                        { text: 'Голосовой запрос' },
                    ],
                    output_speech: 'Текстовый запрос',
                    should_listen: false,
                    suggests: [],
                },
                semantic_frame: {
                    name: 'feature',
                },
            },
            search_doc_meta: {
                type: 'feature',
            },
        };

        expect(response.serialize()).toMatchObject(expected);
    });

    test('should override analytics_info', () => {
        const response = new AliceDocResponse('feature', 'Текстовый запрос', 'Голосовой запрос');

        response.setAnalyticsInfo('covid');

        expect(response.serialize().scenario_response.analytics_info.intent).toBe('covid');
    });

    test('should set product scenario name', () => {
        const response = new AliceDocResponse(
            'feature',
            'Текстовый запрос',
            'Голосовой запрос',
        );

        response.setProductScenarioName('covid_scenario');

        expect(response.serialize().scenario_response.analytics_info.product_scenario_name).toBe('covid_scenario');
    });

    test('should contains correct cards', () => {
        const response = new AliceDocResponse('feature', 'Текстовый запрос', 'Голосовой запрос');
        const expected = [
            { text: 'Еще текст' },
            {
                div_card: {
                    background: { type: 'type', color: 'color' },
                    states: [{ state_id: 1 }],
                },
            },
            {
                div_card: {
                    background: { type: 'type', color: 'color' },
                    states: [{ state_id: 2 }],
                },
            },
        ];

        response.setCards([
            { text: 'Еще текст' },
            {
                div_card: {
                    background: { type: 'type', color: 'color' },
                    states: [{ state_id: 1 }],
                },
            },
        ]);
        response.addCard({
            div_card: {
                background: { type: 'type', color: 'color' },
                states: [{ state_id: 2 }],
            },
        });

        expect(response.serialize().scenario_response.layout.cards).toStrictEqual(expected);
    });

    test('should filter suggests without actions', () => {
        const response = new AliceDocResponse('feature', 'Текстовый запрос', 'Голосовой запрос');

        response
            .setActions([
                new OpenUriAction(
                    'about',
                    'https://yandex.ru/search/touch/?tmplrwr=web4:goodwin',
                    ['Что такое коронавирус', 'что это']
                ),
                new OpenUriAction(
                    'symptoms',
                    'https://yandex.ru/search/touch/?tmplrwr=web4:goodwin',
                    ['симптомы', 'расскажи о симптомах']
                ),
            ])
            .setSuggests([
                { action_id: 'about', title: 'Что такое коронавирус?' },
                { action_id: 'symptoms', title: 'Симптомы' },
                { action_id: 'isolation', title: 'Порядок самоизоляции' }
            ]);

        const result = response.serialize();
        const frameActionsExpected = {
            about: {
                directives: {
                    list: [{
                        open_uri_directive: {
                            name: 'open_uri',
                            uri: 'https://yandex.ru/search/touch/?tmplrwr=web4:goodwin',
                        },
                    }],
                },
                nlu_hint: {
                    frame_name: 'about',
                    instances: [
                        { language: 'L_RUS', phrase: 'Что такое коронавирус' },
                        { language: 'L_RUS', phrase: 'что это' },
                    ],
                },
            },
            symptoms: {
                directives: {
                    list: [{
                        open_uri_directive: {
                            name: 'open_uri',
                            uri: 'https://yandex.ru/search/touch/?tmplrwr=web4:goodwin',
                        },
                    }],
                },
                nlu_hint: {
                    frame_name: 'symptoms',
                    instances: [
                        { language: 'L_RUS', phrase: 'симптомы' },
                        { language: 'L_RUS', phrase: 'расскажи о симптомах' },
                    ],
                },
            },
        };

        expect(result.scenario_response.frame_actions).toStrictEqual(frameActionsExpected);

        const suggestsExpected = [
            { action_id: 'about', title: 'Что такое коронавирус?' },
            { action_id: 'symptoms', title: 'Симптомы' }
        ];

        expect(result.scenario_response.layout.suggests).toStrictEqual(suggestsExpected);
    });
});
