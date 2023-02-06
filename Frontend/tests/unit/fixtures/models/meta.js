module.exports = {
    _data: [
        {
            templateWorkflowId: 'abe3b44d-41f8-11e7-89a6-0025909427cc',
            templateWorkflowDevId: 'abe3b44d-41f8-11e7-89a6-0025909427c0',
            poolsList: [
                {
                    poolId: 260707,
                    sandboxId: 23252,
                    title: 'sbs_ranking',
                    platform: 'desktop',
                    region: 'ru',
                    default: true,
                },
                {
                    poolId: 262882,
                    sandboxId: 23243,
                    title: 'design_with_regions',
                    platform: 'desktop',
                    region: 'all',
                    default: false,
                },
            ],
            layoutsPoolsList: [
                {
                    poolId: 23651786,
                    sandboxId: 58432,
                    title: 'desktop_new',
                    platform: 'desktop',
                    region: 'ru',
                    label: 'Десктоп / любые картинки',
                    profile: 'default_desktop',
                    default: true,
                },
                {
                    poolId: 23651786,
                    sandboxId: 58433,
                    title: 'touch_360',
                    platform: 'touch',
                    region: 'ru',
                    label: 'Телефон, ширина экрана 360px',
                    profile: 'touch-medium',
                },
            ],
            notification: {
                id: 1496829239299,
                enabled: false,
                text: 'Все сломано. Не пишите, не звоните. Чиним.',
            },
            workflowsLocked: false,
        },
    ],

    getMeta() {
        return Promise.resolve(this._data[0]);
    },

    setNotification() {
        return Promise.resolve();
    },

    toggleWorkflows() {
        return Promise.resolve();
    },
};
