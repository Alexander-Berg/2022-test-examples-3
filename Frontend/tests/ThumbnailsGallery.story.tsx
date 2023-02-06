import React from 'react';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import type { IProductImage } from '@src/typings/productImage';
import { ThumbnailsGalleryTouch } from '../ThumbnailsGallery@touch';

const images: IProductImage[] = [
    { src: 'https://avatars.mds.yandex.net/get-mpic/4754204/img_id4085559111223247146.png/200x200' },
    { src: 'https://avatars.mds.yandex.net/get-mpic/4754204/img_id4085559111223247146.png/orig' },
    { src: 'https://avatars.mds.yandex.net/get-mpic/5256693/img_id3877727792880414375.jpeg/orig' },
    { src: 'https://avatars.mds.yandex.net/get-mpic/5256693/img_id3877727792880414375.jpeg/600x600' },
];
const ThumbnailsGalleryStoryCss = `
    .ThumbnailsGalleryStory {
        max-height: 317px;
    }

    .ThumbnailsGalleryStory-Item {
        min-width: 242px;
        max-width: 90%;
    }

    .ThumbnailsGalleryStory-Item + .ThumbnailsGalleryStory-Item {
        margin-left: 4px;
    }
`;

createPlatformStories(
    'Tests/ThumbnailsGallery',
    {
        touch: ThumbnailsGalleryTouch,
    },
    stories => {
        stories
            .add('plain', ThumbnailsGallery => {
                return (
                    <>
                        <style>{ThumbnailsGalleryStoryCss}</style>
                        <ThumbnailsGallery
                            className={'ThumbnailsGalleryStory'}
                            itemClassName={'ThumbnailsGalleryStory-Item'}
                            images={images}
                        />
                    </>
                );
            });
    },
);
