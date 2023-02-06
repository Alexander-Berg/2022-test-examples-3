import {ChangeEventHandler, FC, memo, useMemo, useState} from 'react';

import {PAGE_API_REQUESTS_ENABLED} from 'projects/testControlPanel/pages/TestRequestManagerPage/constants';

import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';

import {useApiRequests} from 'projects/testControlPanel/pages/TestRequestManagerPage/utilities/useApiRequests';
import {deviceMods} from 'utilities/stylesUtils';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import useImmutableCallback from 'utilities/hooks/useImmutableCallback';
import {useBoolean} from 'utilities/hooks/useBoolean';
import {useCookie} from 'utilities/hooks/useCookie';

import RequestRow from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestRow/RequestRow';
import Modal from 'components/Modal/Modal';
import RequestViewer from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/RequestModal';
import Button from 'components/Button/Button';
import Flex from 'components/Flex/Flex';
import Checkbox from 'components/Checkbox/Checkbox';

import cx from './RequestManager.scss';

interface IRequestManagerProps {
    filterByPage: boolean;
}

const RequestManager: FC<IRequestManagerProps> = props => {
    const {filterByPage} = props;

    const deviceType = useDeviceType();
    const {apiRequests, clearRequests} = useApiRequests({filterByPage});
    const {value: pageApiRequestsEnabled, setValue: setPageApiRequestsEnabled} =
        useCookie(PAGE_API_REQUESTS_ENABLED);

    const [selectedRequestId, setSelectedRequestId] = useState<string | null>(
        null,
    );
    const {
        value: isRequestModalVisible,
        setTrue: openRequestModal,
        setFalse: closeRequestModal,
    } = useBoolean(false);

    const selectedRequest = useMemo(() => {
        return apiRequests.find(({id}) => id === selectedRequestId);
    }, [apiRequests, selectedRequestId]);

    const selectRequest = useImmutableCallback((request: IApiRequestInfo) => {
        setSelectedRequestId(request.id);
        openRequestModal();
    });
    const handlePageApiRequestsEnabledChange: ChangeEventHandler<HTMLInputElement> =
        useImmutableCallback(e => {
            setPageApiRequestsEnabled(String(e.target.checked));
        });

    return (
        <div className={cx('root')}>
            <Flex inline between={2}>
                <Button onClick={clearRequests}>Очистить запросы</Button>

                <Flex className={cx('settings')} justifyContent="flex-end">
                    <Checkbox
                        label="Отладка запросов на странице"
                        checked={pageApiRequestsEnabled === 'true'}
                        onChange={handlePageApiRequestsEnabledChange}
                    />
                </Flex>
            </Flex>

            <table
                className={cx(
                    'requestsTable',
                    deviceMods('requestsTable', deviceType),
                )}
            >
                <thead>
                    <tr className={cx('header')}>
                        <th className={cx('column', 'backend')}>Бэкенд</th>
                        <th className={cx('column', 'url')}>Имя</th>
                        <th className={cx('column', 'status')}>Статус</th>
                        <th className={cx('column', 'method')}>Метод</th>
                        <th className={cx('column', 'source')}>Источник</th>
                        <th className={cx('column', 'duration')}>
                            Длительность
                        </th>
                    </tr>
                </thead>

                <tbody>
                    {apiRequests.map(apiRequest => {
                        return (
                            <RequestRow
                                key={apiRequest.id}
                                apiRequest={apiRequest}
                                onClick={selectRequest}
                            />
                        );
                    })}
                </tbody>
            </table>

            <Modal
                isVisible={isRequestModalVisible}
                onClose={closeRequestModal}
            >
                <Modal.Content>
                    {selectedRequest && (
                        <RequestViewer apiRequest={selectedRequest} />
                    )}
                </Modal.Content>
            </Modal>
        </div>
    );
};

export default memo(RequestManager);
