import React, { useMemo } from 'react';
import { cn } from '@bem-react/classname';
import './TestsPageWrap.css';

interface Props {
    background?: string | null;
}

const b = cn('TestsPageWrap');

export const TestsPageWrap: React.FC<Props> = ({ background, children }) => {
    const styles = useMemo(
        () => ({
            ...(background && { backgroundImage: `url(${background})` }),
        }),
        [background],
    );

    return (
        <div className={b()} style={styles}>
            <div className={b('Content')}>{children}</div>
        </div>
    );
};
