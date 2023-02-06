import React, { useState } from 'react';

import { useRequestHandler } from '../../../../hooks/useRequestHandler';
import { Window } from '../../../../ui/FullModal';
import { isValidJSONString } from '../../../../utils/isValidJSONString';
import { Request2 } from '../../../../utils/request';
import Spin from '../../../Spin';
import AddIntroScreenToUserModal from '../../Landings/AddIntroScreenToUserModal';
import { USER_LANDING_TAG_TYPE } from '../../Landings/constants';
import { LANDINGS_REQUESTS, REQUESTS } from '../../Landings/request';
import { ILanding } from '../../Landings/types';

interface ITestingModalProps {
    id: string;
    landingInfo: ILanding;
    onClose: () => void;
}

export const TestingModal = React.memo((props: ITestingModalProps) => {
    const { id, landingInfo, onClose } = props;
    const [tags, setTags] = useState<any>(null);
    const [, setErrorModalOpen] = useState(false);

    const request = new Request2({ requestConfigs: LANDINGS_REQUESTS });
    const requestOptions = React.useMemo(() => {
        return {
            requestName: REQUESTS.GET_TAGS,
            requestOptions: {
                queryParams: {},
            },
        };
    }, []);

    const tagsHandler = (data) => {
        if (error) {
            setErrorModalOpen(true);
        } else {
            const userLandingTagsArray = data.records?.filter(tag => {
                return tag.type === USER_LANDING_TAG_TYPE;
            });

            const tags = {};

            userLandingTagsArray.forEach(el => {
                el.meta = isValidJSONString(el.meta)
                    ? JSON.parse(el.meta)
                    : typeof(el.meta) === 'object' ? el.meta : {};
                const id = el.meta.landing_id;

                if (tags[id]) {
                    tags[id].push(el);
                } else {
                    tags[id] = [el];
                }
            });

            setTags(tags);
        }
    };

    const [isLoading, , error, getTags] = useRequestHandler(request, requestOptions, tagsHandler);

    React.useEffect(() => {
        getTags();
    }, []);

    return <>
        {tags
            ? <AddIntroScreenToUserModal clearHistoryInitialFormData={[{ landing_id: id }]}
                                         landings={[{ ...landingInfo, landing_id: id }]}
                                         userLandingTags={tags ?? {}}
                                         updateData={getTags.bind(null)}
                                         onClose={onClose.bind(null)}/>
            : <Window onClose={setErrorModalOpen.bind(null, false)} error={error}>
                {isLoading ? <Spin/> : null}
            </Window>
        }
    </>;
});
