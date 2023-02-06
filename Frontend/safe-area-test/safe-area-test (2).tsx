import React from 'react';
import { Helmet } from 'react-helmet';

import { Button } from 'blocks/button/button';
import { List } from 'blocks/list';
import { ListItem } from 'blocks/list-item';
import { NavBar } from 'blocks/navbar';
import { ToggleSwitch } from 'blocks/toggle-switch';

import { useElementRef } from 'hooks/use-element-ref';
import { useWindowSize } from 'hooks/use-window-size';
import { classname } from 'utils/classname';

import './safe-area-test.css';

const b = classname('safe-area-test');

export const SafeAreaTest: React.FunctionComponent = () => {
    const [meta, setMeta] = React.useState(true);
    const [safeArea, setSafeArea] = React.useState(true);
    const viewportOrigin = 'width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1, user-scalable=no';
    const windowSize = useWindowSize();

    const [element, elementRef] = useElementRef<HTMLDivElement>();
    const [vhElement, vhElementRef] = useElementRef<HTMLDivElement>();

    const sizes = React.useMemo(
        () => {
            const t : { [key: string]: string } = {};

            if (element) {
                const styles = window.getComputedStyle(element);
                t.top = styles.paddingTop;
                t.left = styles.paddingLeft;
                t.bottom = styles.paddingBottom;
                t.right = styles.paddingRight;
            }
            if (vhElement) {
                const styles = window.getComputedStyle(vhElement);
                t.vh = styles.height;
            }
            t.windowHeight = String(window.innerHeight);

            return t;
        },

        // windowSize нужен для ререндера при изменении размера
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [element, vhElement, ...windowSize],
    );

    return (
        <>
            <Helmet>
                <meta name="viewport" content={viewportOrigin + (meta ? ', viewport-fit=cover' : '')} />
            </Helmet>
            <div className={b({ 'safe-area': safeArea })} ref={elementRef}>
                <NavBar />
                <div className={b('content')}>
                    <List>
                        <ListItem name="Использовать safeArea" control={<ToggleSwitch active={safeArea} onChange={setSafeArea} />} />
                        <ListItem name="Использовать viewport" control={<ToggleSwitch active={meta} onChange={setMeta} />} />
                    </List>
                    <Button
                        onClick={() => document.getElementsByTagName('body')[0].requestFullscreen() }
                    >
                        Перейти в полный экран
                    </Button>
                    <Button
                        theme="secondary"
                        onClick={() => document.exitFullscreen() }
                    >
                        Выйти из полного экрана
                    </Button>
                    <div>
                        { Object.entries(sizes).map(v => <div key={v[0]}>{v[0]}: {v[1]}</div>) }
                    </div>
                    <div className={b('vh')} ref={vhElementRef}/>
                </div>
            </div>
        </>
    );
};
