import {IBadge} from 'src/types/common';
import {addBadge} from 'src/helpers/common.helpers';

export default function addThiriumBadge({
    status,
    text,
    url,
}: Pick<IBadge, 'status' | 'text' | 'url'>): Promise<void> {
    return addBadge({
        id: 'thirium',
        module: 'THIRIUM',
        status,
        text,
        url,
    });
}
