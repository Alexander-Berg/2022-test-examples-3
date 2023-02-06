import {FC, memo} from 'react';

import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';

import {deviceMods} from 'utilities/stylesUtils';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import useImmutableCallback from 'utilities/hooks/useImmutableCallback';
import {getDisplayedMethod} from 'projects/testControlPanel/pages/TestRequestManagerPage/utilities/getDisplayedMethod';

import RequestDuration from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestDuration/RequestDuration';

import cx from './RequestRow.scss';

interface IRequestRowProps {
    apiRequest: IApiRequestInfo;

    onClick(apiRequest: IApiRequestInfo): void;
}

const RequestRow: FC<IRequestRowProps> = props => {
    const {
        apiRequest,
        apiRequest: {
            request,
            response,
            isAborted,
            apiHostType,
            source,
            startTime,
            endTime,
            localStartTime,
        },
        onClick,
    } = props;

    const deviceType = useDeviceType();

    const handleClick = useImmutableCallback(() => {
        onClick(apiRequest);
    });

    const {pathname} = new URL(request.url);
    const status = response?.status ?? NaN;

    return (
        <tr
            className={cx('root', deviceMods('root', deviceType), {
                root_success: status < 300,
                root_pending: !response,
                root_error: status >= 400 || isAborted,
            })}
            onClick={handleClick}
        >
            <td className={cx('column', 'backend')}>{apiHostType}</td>
            <td className={cx('column', 'url')}>{pathname}</td>
            <td className={cx('column', 'status')}>
                {isAborted ? 'aborted' : status || '-'}
            </td>
            <td className={cx('column', 'method')}>
                {getDisplayedMethod(request.method)}
            </td>
            <td className={cx('column', 'source')}>{source.type}</td>
            <td className={cx('column', 'duration')}>
                <RequestDuration
                    startTime={startTime}
                    endTime={endTime}
                    localStartTime={localStartTime}
                />
            </td>
        </tr>
    );
};

export default memo(RequestRow);
