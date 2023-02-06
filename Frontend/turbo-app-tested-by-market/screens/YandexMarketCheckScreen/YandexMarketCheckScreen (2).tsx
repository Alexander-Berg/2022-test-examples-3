import * as React from 'react';
import { Redirect } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { useSetTitle } from '~/libs/set-title/useSetTitle';
import { EcomScreen } from '~/components/EcomScreen';
import { ScreenContent } from '~/components/ScreenContent';
import { ScreenHeader } from '~/components/ScreenHeader';
import { ScreenHeaderBack } from '~/components/ScreenHeaderBack';
import { INavigationScreenProps } from '~/components/Navigation';
import {
    shopIdSelector,
    turboAppEnabledSelector,
    nameShopSelector,
    footerDataSelector
} from '~/redux/services/meta/selectors';
import { selectPrevScreen } from '~/redux/services/global/selectors';
import { ECatalogPageTypes, makeLocation } from '~/libs/urls/location';
import { YandexMarketCheck } from '../../components/YandexMarketCheck';
import { useEnter } from './hooks/useEnter';

export const YandexMarketCheckScreen: React.FC<INavigationScreenProps> = ({ pageType }) => {
    const locationCreator = makeLocation();
    const shopId = useSelector(shopIdSelector);
    const shopName = useSelector(nameShopSelector);
    const { url } = useSelector(footerDataSelector);
    const turboAppEnabled = useSelector(turboAppEnabledSelector);
    const setTitle = useSetTitle();
    const prevScreen = useSelector(selectPrevScreen);
    const handleEndered = useEnter([
        React.useCallback(() => setTitle(`Магазин «${shopName}» проверен Яндекс.Маркетом`), [shopName]),
    ]);

    if (!turboAppEnabled) {
        const toMainPage = locationCreator.buildCatalogLocation({
            shopId,
            pageType: ECatalogPageTypes.main,
            state: {
                transition: 'none',
            },
        });
        return <Redirect to={toMainPage} />;
    }

    return (
        <EcomScreen type={pageType} onEntered={handleEndered}>
            <ScreenHeader>
                <ScreenHeaderBack text={prevScreen ? prevScreen.backText : 'Назад'} />
            </ScreenHeader>
            <ScreenContent isScreenPrepared pageType={pageType}>
                <YandexMarketCheck originalShopUrl={url} />
            </ScreenContent>
        </EcomScreen>
    );
};
