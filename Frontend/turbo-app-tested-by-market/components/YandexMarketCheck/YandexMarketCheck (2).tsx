import * as React from 'react';
import { Button } from '~/components/Button';
import { Link } from '~/components/Link';
import { getFosUrl } from '~/libs/fosUrl';

import { clsButton, clsFooter, clsImage, clsTitle, clsWrapper, clsText, clsImageWrap } from './YandexMarketCheck.cn';
import './YandexMarketCheck.scss';

interface IProps {
    originalShopUrl: string;
}

export const YandexMarketCheck: React.FC<IProps> = React.memo(({ originalShopUrl }: IProps) => {
    const fosUrlWithParams = React.useMemo(() => {
        return getFosUrl(originalShopUrl);
    }, [originalShopUrl]);

    return (
        <div className={clsWrapper}>
            <div className={clsImageWrap}>
                <img
                    className={clsImage}
                    src="https://avatars.mds.yandex.net/get-turbo/3659220/2a000001757421484c42d1608608e4cd335a/orig"
                    alt="Проверено Яндекс.Маркетом"
                />
            </div>
            <h2 className={clsTitle}>
                Магазин проверен
                Яндекс.Маркетом
            </h2>
            <div className={clsText}>
                Мы специально протестировали работу с заказами. Но если вам что-то не понравится, обязательно поделитесь
            </div>
            <div className={clsFooter}>
                В случае возникновения проблем можно пожаловаться на магазин
            </div>
            <Button
                size="l"
                width="max"
                view="action"
                as={Link}
                href={fosUrlWithParams}
                target="_blank"
                className={clsButton}
            >
                Сообщить о проблеме
            </Button>
        </div>
    );
});
