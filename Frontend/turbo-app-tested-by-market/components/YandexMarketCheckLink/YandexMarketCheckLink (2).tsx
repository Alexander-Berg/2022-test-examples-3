import * as React from 'react';
import { ECatalogPageTypes, makeLocation } from '~/libs/urls/location';
import { Icon } from '~/components/Icon';
import { NavigationLinkWithArrow } from '~/components/NavigationLinkWithArrow';
import { TNavigationTransitionType } from '../../../../components/NavigationTransition';

import { clsWrapper, clsIcon, clsText } from './YandexMarketCheckLink.cn';
import * as verifyIcon from './assets/Verify.svg';
import './YandexMarketCheckLink.scss';

interface IProps {
    shopId: string;
    onClick?: () => void;
    transition?: TNavigationTransitionType;
    text?: string;
}

export const YandexMarketCheckLink: React.FC<IProps> = ({
    shopId,
    onClick,
    transition = 'none',
    text = 'Магазин проверен Яндекс.Маркетом',
}) => {
    const locationCreator = makeLocation();

    const handleClick = React.useCallback(() => {
        onClick && onClick();
    }, [onClick]);

    return (
        <>
            <NavigationLinkWithArrow
                onClick={handleClick}
                to={locationCreator.buildCatalogLocation({
                    shopId,
                    pageType: ECatalogPageTypes.marketCheck,
                    state: {
                        transition: transition,
                    },
                })}
                className={clsWrapper}
            >
                <div className={clsIcon}>
                    <Icon svg={verifyIcon} />
                </div>
                <div className={clsText}>
                    {text}
                </div>
            </NavigationLinkWithArrow>
        </>
    );
};
