import {FC, memo} from 'react';

import {useIntervalForceUpdate} from 'utilities/hooks/useIntervalForceUpdate';
import {getHumanDuration} from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestDuration/utilities/getHumanDuration';

interface IRequestDurationProps {
    startTime: number;
    endTime: number | null;
    localStartTime?: number;
}

const RequestDuration: FC<IRequestDurationProps> = props => {
    const {startTime, endTime, localStartTime} = props;

    useIntervalForceUpdate(endTime ? null : 250);

    return (
        <span>
            {getHumanDuration(
                endTime
                    ? endTime - startTime
                    : Date.now() - (localStartTime ?? startTime),
            )}
        </span>
    );
};

export default memo(RequestDuration);
