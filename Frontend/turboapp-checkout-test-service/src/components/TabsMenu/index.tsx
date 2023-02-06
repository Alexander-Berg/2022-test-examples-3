import { compose } from '@bem-react/core';
import {
    TabsMenu as LegoTabsMenu,
    withSizeM,
    withLayoutHoriz,
    withViewDefault,
} from '@yandex-lego/components/TabsMenu/desktop';

export const TabsMenu = compose(withSizeM, withLayoutHoriz, withViewDefault)(LegoTabsMenu);
export type TabsMenuProps = Parameters<typeof TabsMenu>[0];
