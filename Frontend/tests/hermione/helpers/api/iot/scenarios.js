/**
 * @class Scenarios
 * @type {{methods: {getTriggers: (function(): {path: string}), getScenarioIcons: (function(): {path: string}), getHistory: (function({status: *}): {path: string, query: {status: *}}), getTriggerSuggests: (function(*=): {path: string, query: {trigger: *}}), getCapabilitySuggests: (function({deviceId: *, capabilityType?: *, instance: *}): {path: string, query: {instance: *, type: *}}), getScenarios: (function(): {path: string}), deleteScenario: (function(*): {path: string, options: {method: string}}), activation: (function({scenarioId: *, active?: *}): {path: string, options: {method: string, body: string}}), getNameSuggests: (function(*=): {path: string, query: {trigger: *}}), activateScenario: (function(*): {path: string, options: {method: string}}), validateScenarioName: (function({scenarioId?: *, value?: *}): {path: string, query: {id: *}, options: {method: string, body: string}})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/user/scenarios',
    methods: {
        getScenarios: () => ({
            path: '',
        }),

        getTriggers: () => ({
            path: '/triggers',
        }),

        getNameSuggests: (triggerType) => ({
            path: '/add',
            query: {
                trigger: triggerType,
            },
        }),

        getTriggerSuggests: (triggerType) => ({
            path: '/add',
            query: {
                trigger: triggerType,
            },
        }),

        getCapabilitySuggests: ({ deviceId, capabilityType, instance }) => ({
            path: `/devices/${deviceId}/suggestions`,
            query: {
                type: capabilityType,
                instance,
            },
        }),

        deleteScenario: (scenarioId) => ({
            path: `/${scenarioId}`,
            options: {
                method: 'DELETE',
            },
        }),

        getScenarioIcons: () => ({
            path: '/icons',
        }),

        activateScenario: (scenarioId) => ({
            path: `/${scenarioId}/actions`,
            options: {
                method: 'POST',
            },
        }),

        validateScenarioName: ({ scenarioId, value }) => ({
            path: '/validate/name',
            query: {
                id: scenarioId,
            },
            options: {
                method: 'POST',
                body: JSON.stringify({
                    name: value,
                }),
            },
        }),

        activation: ({ scenarioId, active }) => ({
            path: `/${scenarioId}/activation`,
            options: {
                method: 'POST',
                body: JSON.stringify({
                    is_active: active,
                }),
            },
        }),

        getHistory: ({ status }) => ({
            path: '/history',
            query: {
                status,
            },
        }),
    },
};
