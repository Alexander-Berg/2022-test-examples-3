import React, { PureComponent } from 'react';
import { render } from 'react-dom';
import { compose, composeU } from '@bem-react/core';

import { BPage } from '../../internal/components/BPage/BPage';
import { Hermione } from '../../internal/components/Hermione/Hermione';

import { configureRootTheme, cnTheme } from '../../Theme';
import { theme as themeDefault } from '../../Theme/presets/default';
import { theme as themeInverse } from '../../Theme/presets/inverse';
import { theme as themeBrand } from '../../Theme/presets/brand';

import { TabsMenu as TabsMenuBase } from '../TabsMenu';
import { withLayoutHoriz } from '../_layout/TabsMenu_layout_horiz';
import { withThemeNormal } from '../_theme/TabsMenu_theme_normal@desktop';
import { withViewDefault } from '../_view/TabsMenu_view_default@desktop';
import { withSizeS } from '../_size/TabsMenu_size_s';
import { withSizeM } from '../_size/TabsMenu_size_m';
import { withAdaptive } from '../_adaptive/TabsMenu_adaptive@desktop';

configureRootTheme({ theme: themeDefault });

const TabsMenu = compose(
    withLayoutHoriz,
    withThemeNormal,
    withViewDefault,
    withAdaptive,
    composeU(withSizeS, withSizeM),
)(TabsMenuBase);

const themes = [themeDefault, themeInverse, themeBrand];

const TabsExample = class TabsBase extends PureComponent {
    onTabClick = (tabId: string) => () => {
        this.setState({ activeTab: tabId });
    };

    initialTabs = [
        { id: 'tab1', onClick: this.onTabClick('tab1'), content: 'Поиск' },
        { id: 'tab2', onClick: this.onTabClick('tab2'), content: 'Картинки' },
        { id: 'tab3', onClick: this.onTabClick('tab3'), disabled: true, content: 'Видео' },
        { id: 'tab4', onClick: this.onTabClick('tab4'), content: 'Карты' },
        { id: 'tab5', onClick: this.onTabClick('tab5'), content: 'Музыка' },
    ];

    state = {
        activeTab: 'tab1',
        tabs: this.initialTabs,
    };

    render() {
        return (
            <BPage>
                <Hermione className="Classic">
                    {['s', 'm'].map((size: any) => (
                        <Hermione key={size} element="Item" modifiers={{ size }}>
                            <TabsMenu
                                theme="normal"
                                layout="horiz"
                                size={size}
                                activeTab={this.state.activeTab}
                                tabs={this.initialTabs}
                            />
                        </Hermione>
                    ))}
                </Hermione>
                <Hermione className="New">
                    {themes.map((theme, index) => (
                        <div key={index} className={cnTheme(theme)}>
                            {['m', 's'].map((size: any) => (
                                <Hermione key={size} element="Item" modifiers={{ size, color: theme.color }}>
                                    <TabsMenu
                                        view="default"
                                        layout="horiz"
                                        size={size}
                                        activeTab={this.state.activeTab}
                                        tabs={this.initialTabs}
                                    />
                                </Hermione>
                            ))}
                        </div>
                    ))}
                </Hermione>
                <Hermione className="Adaptive">
                    {['m', 's'].map((size: any) => (
                        <Hermione key={size} element="Item" modifiers={{ size }}>
                            <TabsMenu
                                view="default"
                                layout="horiz"
                                theme="normal"
                                size={size}
                                activeTab={'tab6'}
                                tabs={[
                                    ...this.initialTabs,
                                    { id: 'tab6', onClick: this.onTabClick('tab6'), content: 'Характеристики' },
                                    { id: 'tab7', onClick: this.onTabClick('tab7'), content: 'Главная' },
                                    { id: 'tab8', onClick: this.onTabClick('tab8'), content: 'Эфир' },
                                ]}
                                adaptive
                                staticMoreText
                            />
                        </Hermione>
                    ))}
                </Hermione>
                <Hermione className="AdaptiveWithAddingAndDeleting">
                    {['m'].map((size: any) => (
                        <Hermione key={size} element="Item" modifiers={{ size }}>
                            <TabsMenu
                                view="default"
                                layout="horiz"
                                theme="normal"
                                size={size}
                                activeTab={this.state.activeTab}
                                tabs={this.state.tabs}
                                adaptive
                            />
                            <button
                                className="ButtonAdd"
                                onClick={
                                    () => {
                                        const tabName = `tab${this.state.tabs.length}`;
                                        this.setState({
                                            ...this.state,
                                            tabs: [
                                                ...this.state.tabs,
                                                {
                                                    id: tabName,
                                                    onClick: this.onTabClick(tabName),
                                                    content: tabName,
                                                }
                                            ]
                                        });
                                    }
                                }
                            >
                                Добавить
                            </button>
                            <button
                                className="ButtonDelete"
                                onClick={
                                    () => {
                                        this.setState({
                                            ...this.state,
                                            tabs: this.state.tabs.slice(0, this.state.tabs.length - 1)
                                        });
                                    }
                                }
                            >
                                Удалить
                            </button>
                        </Hermione>
                    ))}
                </Hermione>
            </BPage>
        );
    }
};

render(<TabsExample />, document.getElementById('root'));
