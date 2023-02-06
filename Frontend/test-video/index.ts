import { storyId } from 'blocks/stories/story-id';

import { DeviceType } from 'types/device-type';
import { StoryConfig } from 'types/stories';
import { ShowContentConditionType, VideoFormat } from 'types/stories/story-background-video';
import { StoryItemCustomButtonAction, StoryItemVideoConfig } from 'types/stories/story-item-custom';
import { StoryItemType } from 'types/stories/story-item-type';

const id = storyId('test-video');

const tokyoRevengers = (): StoryItemVideoConfig => ({
    id: id('tokyo-revengers'),
    type: StoryItemType.VIDEO,
    options: {
        backgroundVideo: {
            videoSrc: {
                [VideoFormat.H264]: require('./assets/tokyo-revengers.mp4').default,
            },
            showContentCondition: {
                type: ShowContentConditionType.ENDED,
            },
        },
        title: ('Tokio Revengers'),
        paragraphs: [{ text: ('Tokyo Revengers — манга, созданная Кэном Вакуи. Публикуется с марта 2017 года в журнале Weekly Shonen Magazine издательства Коданся.') }],
        buttons: [{
            action: StoryItemCustomButtonAction.INTERNAL_LINK,
            url: '/iot/settings/groups/add',
            urlQuery: { type: DeviceType.SMART_SPEAKER },
            text: ('Стать анимешником'),
        }],
        beta: true,
    },
});

export const testVideo = (): StoryConfig => {
    const items: StoryItemVideoConfig[] = [];

    items.push(tokyoRevengers());

    return ({
        id: id(),
        miniature: {
            backgroundImage: {
                resolutions: {
                    x1: require('./assets/thumb.png').default,
                    x2: require('./assets/thumb.png').default,
                    x3: require('./assets/thumb.png').default,
                },
                thumbnails: {
                    x1: require('./assets/thumb.png').default,
                    x2: require('./assets/thumb.png').default,
                    x3: require('./assets/thumb.png').default,
                },
            },
            title: ('Tokio Revengers [AMV]'),
            shortTitle: ('Tokio Revengers'),
        },
        items,
        beta: true,
    });
};
