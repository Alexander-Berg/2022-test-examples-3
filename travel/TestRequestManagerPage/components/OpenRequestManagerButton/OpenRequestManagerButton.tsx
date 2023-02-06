import {FC, memo} from 'react';
import {createPortal} from 'react-dom';

import {PAGE_API_REQUESTS_ENABLED} from 'projects/testControlPanel/pages/TestRequestManagerPage/constants';

import {useBoolean} from 'utilities/hooks/useBoolean';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {deviceMods} from 'utilities/stylesUtils';
import {useCookie} from 'utilities/hooks/useCookie';

import Button from 'components/Button/Button';
import Modal from 'components/Modal/Modal';
import RequestManager from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestManager/RequestManager';
import SwapVerticalIcon from 'icons/16/SwapVertical';

import cx from './OpenRequestManagerButton.scss';

const OpenRequestManagerButton: FC = () => {
    const {value: pageApiRequestsEnabled} = useCookie(
        PAGE_API_REQUESTS_ENABLED,
    );

    const deviceType = useDeviceType();
    const {
        value: isModalVisible,
        setTrue: openModal,
        setFalse: closeModal,
    } = useBoolean(false);

    if (pageApiRequestsEnabled !== 'true') {
        return null;
    }

    return createPortal(
        <>
            <Button
                className={cx('button')}
                theme="raised"
                shape="circle"
                icon={<SwapVerticalIcon />}
                onClick={openModal}
            />

            <Modal
                isVisible={isModalVisible}
                alwaysRenderContent
                onClose={closeModal}
                fullScreen={deviceType.isMobile}
                disableAutoFocus
                returnFocus={false}
                resetScrollOnOpen={false}
            >
                <div
                    className={cx(
                        'modalContent',
                        deviceMods('modalContent', deviceType),
                    )}
                >
                    <RequestManager filterByPage />
                </div>
            </Modal>
        </>,
        document.body,
    );
};

export default memo(OpenRequestManagerButton);
