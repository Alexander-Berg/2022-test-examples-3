import React, { useCallback, useState } from 'react';
import { cn } from '@bem-react/classname';

import { Button } from 'components/Tests/Button/Button';
import { Spinner } from 'components/Spinner/Spinner';
import { ProgressBar } from 'components/Tests/ProgressBar/ProgressBar';

import './Test.scss';

import data from './data.json';

const b = cn('LookAround');

const { pages } = data;

interface Props {
    onRetry: () => void;
}

export const LookAroundInteractive: React.FC<Props> = ({ onRetry }) => {
    const [page, setPage] = useState(0);
    const [showSecondary, setShowSecondary] = useState(false);
    const [imageLoading, setImageLoading] = useState(true);

    const handleButtonClick = () => {
        setImageLoading(true);

        if (showSecondary) {
            if (page === pages.length - 1) {
                return onRetry();
            }

            setPage(page + 1);
            setShowSecondary(false);
        } else {
            setShowSecondary(true);
        }
    };

    const onImageLoad = useCallback(() => {
        setImageLoading(false);
    }, []);

    const image = showSecondary ? pages[page].images.secondary : pages[page].images.primary;
    const text = showSecondary ? pages[page].texts.secondary : pages[page].texts.primary;

    return (
        <div className={b()}>
            <div className={b('Progress')}>
                <div className={b('Score')}>
                    {page + 1} / {pages.length}
                </div>
                <ProgressBar progress={((page - 1) / pages.length) * 100} />
            </div>

            <div className={b('Question')}>
                <div className={b('ImageWrap')}>
                    <Spinner progress={imageLoading} />
                    <img className={b('Image')} src={image} onLoad={onImageLoad} />
                </div>
                <div className={b('Text')} dangerouslySetInnerHTML={{ __html: text }} />
            </div>

            <div className={b('Controls')}>
                <Button onClick={handleButtonClick} className={b('Button')}>
                    {showSecondary ? 'Едем дальше' : 'Посмотреть другой сенсор'}
                </Button>
            </div>
        </div>
    );
};
