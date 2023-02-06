/**
 * @class ScenariosV3
 * @type {{methods: {getScenario: (function(*): {path: string}), updateScenario: (function({id: *, name: *, icon: *, triggers: *, steps: *, effectiveTime?: *}): {path: string, options: {method: string, body: string}}), validateTrigger: (function({scenario: {id: *, name: *, icon: *, triggers: *, steps: *, effectiveTime?: *}, trigger: *}): {path: string, options: {method: string, body: string}}), createScenario: (function({name: *, icon: *, triggers: *, steps: *, effectiveTime?: *}): {path: string, options: {method: string, body: string}}), validateCapability: (function({scenario: {id: *, name: *, icon: *, triggers: *, steps: *, effectiveTime?: *}, capabilityType?: *, instance: *, value: *}): {path: string, options: {method: string, body: string}})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/v3/user/scenarios/',
    methods: {
        createScenario: ({ name, icon, triggers, steps, effectiveTime }) => ({
            path: '',
            options: {
                method: 'POST',
                body: JSON.stringify({
                    name,
                    icon,
                    triggers,
                    steps,
                    effective_time: effectiveTime,
                }),
            },
        }),

        updateScenario: ({ id, name, icon, triggers, steps, effectiveTime }) => ({
            path: `/${id}`,
            options: {
                method: 'PUT',
                body: JSON.stringify({
                    name,
                    icon,
                    triggers,
                    steps,
                    effective_time: effectiveTime,
                }),
            },
        }),

        getScenario: (scenarioId) => ({
            path: `/${scenarioId}/edit`,
        }),

        validateTrigger: ({ scenario: { id, name, icon, triggers, steps, effectiveTime }, trigger }) => ({
            path: '/validate/trigger',
            options: {
                method: 'POST',
                body: JSON.stringify({
                    scenario: {
                        id,
                        name,
                        icon,
                        triggers,
                        steps,
                        effective_time: effectiveTime,
                    },
                    trigger,
                }),
            },
        }),

        validateCapability: ({ scenario: { id, name, icon, triggers, steps, effectiveTime }, capabilityType, instance, value }) => ({
            path: '/validate/capability',
            options: {
                method: 'POST',
                body: JSON.stringify({
                    scenario: {
                        id,
                        name,
                        icon,
                        triggers,
                        steps,
                        effective_time: effectiveTime,
                    },
                    capability: {
                        type: capabilityType,
                        state: {
                            instance,
                            value,
                        },
                    },
                }),
            },
        }),
    },
};
