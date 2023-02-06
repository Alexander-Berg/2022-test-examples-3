/* eslint-disable */
import { ImageAttributes, ImageType, imageTypes } from '../../../db/tables/image';

interface MImageAttributes extends ImageAttributes {
    type?: ImageType;
    url?: string;
    skillId?: string;
    id?: string;
    size?: number;
}

export function generateImage(props: MImageAttributes): MImageAttributes {
    const {
        type = imageTypes[0],
        // 'skillSettings' ,
        url = 'https://avatars.mdst.yandex.net/get-dialogs/5182/68c495dd0ae8533283d3/orig',
        skillId = '42',
        size = 0,
    } = props;
    return {
        type,
        url,
        skillId,
        size,
    };
}
