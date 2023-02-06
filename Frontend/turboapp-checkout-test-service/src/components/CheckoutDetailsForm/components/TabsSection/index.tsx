import React, { useMemo } from 'react';
import { classnames } from '@bem-react/classnames';

import { TabsMenu } from '../../../TabsMenu';
import { Button } from '../../../Button';
import { Icon } from '../../../Icon';

import styles from './TabsSection.module.css';

type Tab = {
    id: string;
    title: string;
    onClose?: () => void;
};

type Props = {
    tabs: Tab[];
    activeTab: string;
    onChange: (tab: string) => void;
    onAdd?: () => void;
    className?: string;
};

function PlusIcon(className: string) {
    return <Icon glyph="type-cross" size="m" className={classnames(className, styles.icon)} />;
}

function CrossIcon(className: string) {
    return <Icon glyph="type-cross" size="m" className={className} />;
}

export const TabsSection: React.FC<Props> = ({ tabs, activeTab, onChange, onAdd, className }) => {
    const tabsMenu = useMemo(() => {
        return tabs.map(tab => {
            return {
                id: tab.id,
                onClick: () => onChange(tab.id),
                className: styles.tab,
                content: (
                    <>
                        {tab.title}
                        {tab.onClose && (
                            <Button
                                view="clear"
                                size="s"
                                onClick={tab.onClose}
                                icon={CrossIcon}
                                className={styles.close}
                            />
                        )}
                    </>
                ),
            };
        });
    }, [tabs, onChange]);

    return (
        <div className={classnames(styles.container, className)}>
            <TabsMenu
                size="m"
                view="default"
                layout="horiz"
                className={styles.tabs}
                activeTab={activeTab}
                tabs={tabsMenu}
            />

            {onAdd && <Button view="clear" size="s" onClick={onAdd} icon={PlusIcon} />}
        </div>
    );
};
