import React from 'react';
import { cn } from '@bem-react/classname';
import './ProgressBar.css';

const b = cn('ProgressBar');

export const ProgressBar = ({
    progress = 0, // percents
}) => {
    return (
        <div className={ b() }>
            <div className={ b('Backdrop') } />
            <div className={ b('Fill') } style={{ width: `${progress}%` }} />
        </div>
    );
};
