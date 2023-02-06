import initStoryshots from '@storybook/addon-storyshots';

initStoryshots({
    suit: 'Snapshots',
    storyNameRegex: /^((?!.*?DontTest).)*$/
});
