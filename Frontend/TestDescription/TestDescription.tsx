import React, { useCallback, useState } from 'react';
import { cn } from '@bem-react/classname';

import { DescriptionButton } from 'components/DescriptionButton/DescriptionButton';
import { CollapsibleText } from 'components/CollapsibleText/CollapsibleText';
import { DescriptionMobile } from 'components/DescriptionMobile/DescriptionMobile';

import './TestDescription.scss';

const b = cn('TestDescription');

export const TestDescription: React.FC = ({ children }) => {
    const [descriptionModalVisible, setDescriptionModalVisible] = useState(false);
    const isMobile = window.innerWidth <= 970;

    const onShowDescriptionModal = useCallback(() => setDescriptionModalVisible(true), []);
    const onCloseDescriptionModal = useCallback(() => setDescriptionModalVisible(false), []);

    return (
        <div className={b()}>
            {isMobile ? (
                <>
                    <DescriptionButton
                        onClick={onShowDescriptionModal}
                        value="На что обратить внимание"
                    />
                    <DescriptionMobile
                        className={b('DescriptionMobile')}
                        onClose={onCloseDescriptionModal}
                        visible={descriptionModalVisible}
                        closeButtonText="Понятно"
                    >
                        <div className={b('MobileContent')}>{children}</div>
                    </DescriptionMobile>
                </>
            ) : (
                <CollapsibleText
                    className={b('CollapsibleText')}
                    title="На что обратить внимание"
                    defaultCollapsed
                    needWrapper
                >
                    {children}
                </CollapsibleText>
            )}
        </div>
    );
};
