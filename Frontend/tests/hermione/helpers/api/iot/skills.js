/**
 * @class Skills
 * @type {{methods: {updateSpeakerDevices: (function(): Promise), getSkills: (function(): {path: string}), unbindSkill: (function(UnbindSkillParams): {path: string, query: {save_devices: string}, options: {method: string}}), updateDevices: (function(ApiIotSkillId): {path: string, options: {method: string}}), updateYandexDevices: (function(): {path: string, options: {method: string}}), getSkillInfo: (function(string): {path: string})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/user/skills',
    methods: {
        getSkills: () => ({
            path: '',
        }),

        getSkillInfo: (id) => ({
            path: `/${id}`,
        }),

        updateDevices: (skillId) => ({
            path: `/${skillId}/discovery`,
            options: {
                method: 'POST',
            },
        }),

        updateYandexDevices: () => ({
            path: '/T/discovery',
            options: {
                method: 'POST',
            },
        }),

        updateSpeakerDevices: () => ({
            // В оригинале srcrwr отсутствует
            path: '/Q/discovery?srcrwr=QUASAR_HOST:testing.quasar.yandex.ru',
            options: {
                method: 'POST',
            },
        }),

        unbindSkill: ({ skillId, removeDevices }) => ({
            path: `/${skillId}/unbind`,
            query: removeDevices ? { } : { save_devices: 'true' },
            options: {
                method: 'POST',
            },
        }),
    },
};
