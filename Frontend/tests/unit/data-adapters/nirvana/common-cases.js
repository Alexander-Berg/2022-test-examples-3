module.exports = {
    profileAndTolokaParams: [
        {
            description: 'Тач',
            device: 'touch',
            iphone: 'no',
            poolTitle: 'touch_360_comments',
            profile: 'android-medium',
            tolokaParams: { zoom: 0, templateName: 'touch_360_comments', platform: 'touch' },
            tolokaCloneParams: { 'assessment-service': 'toloka', 'prod-assessment-env-type': 'prod', template: { production: { 'pool-id': 1 }, sandbox: { 'pool-id': 1 } } },
        },
        {
            description: 'Десктоп',
            device: 'desktop',
            iphone: 'yes',
            poolTitle: 'desktop_zoom_wide',
            profile: 'wide',
            tolokaParams: { zoom: 1, templateName: 'desktop_zoom_wide', platform: 'desktop' },
            tolokaCloneParams: { 'assessment-service': 'toloka', 'prod-assessment-env-type': 'prod' ,template: { production: { 'pool-id': 2 }, sandbox: { 'pool-id': 2 } } },
        },
    ],
};
