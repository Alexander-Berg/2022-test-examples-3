import React from 'react';

import ClearPreset from './ClearPreset';
import StartPreset from './StartPreset';

import styles from './Presets.module.css';

const Preset: React.FC = ({ children }) => <div className={styles.preset}>{children}</div>;

const presets = [<ClearPreset key="clear" />, <StartPreset key="start" />];

const Presets: React.FC = () => {
    return (
        <div className={styles.presets}>
            {presets.map((preset, index) => (
                <Preset key={index}>{preset}</Preset>
            ))}
        </div>
    );
};

export default Presets;
