import {render, screen} from '@testing-library/react';
import Router from 'next/router';

import SideBarDisplayModeContext from '@contexts/SideBarDisplayModeContext';

import SideBarMenuItem from './SideBarMenuItem';

import '@testing-library/jest-dom';

// @TODO Сломался импорт нэймспэйса роутера `import * as Router from 'next/router';`
// поэтому пока так
// @ts-ignore
const useRouter = jest.spyOn(Router, 'useRouter');

describe('SideBarMenuItem', () => {
    beforeAll(() => {
        // @ts-ignore
        useRouter.mockReturnValue({
            asPath: '/selectedPath',
        });
    });

    it('рендерит выбранный пункт меню в свернутом сайдбаре', () => {
        const {container} = render(
            <SideBarDisplayModeContext.Provider value={{expanded: false, toggleDisplayMode: jest.fn()}}>
                <SideBarMenuItem
                    mainPath="/mainPath"
                    label="MenuItem1"
                    icon={<div data-testid="iconNodeId">icon</div>}
                />
                <SideBarMenuItem
                    mainPath="/selectedPath"
                    label="MenuItem2"
                    icon={<div data-testid="iconNodeId">icon</div>}
                />
                <SideBarMenuItem
                    mainPath="/anotherPath"
                    label="MenuItem3"
                    icon={<div data-testid="iconNodeId">icon</div>}
                />
            </SideBarDisplayModeContext.Provider>
        );

        expect(container).toMatchSnapshot();
    });

    it('рендерит выбранный пункт меню в развернутом сайдбаре', () => {
        const {container} = render(
            <SideBarDisplayModeContext.Provider value={{expanded: true, toggleDisplayMode: jest.fn()}}>
                <SideBarMenuItem
                    mainPath="/mainPath"
                    label="MenuItem1"
                    icon={<div data-testid="iconNodeId">icon</div>}
                />
                <SideBarMenuItem
                    mainPath="/selectedPath"
                    label="MenuItem2"
                    icon={<div data-testid="iconNodeId">icon</div>}
                />
                <SideBarMenuItem
                    mainPath="/anotherPath"
                    label="MenuItem3"
                    icon={<div data-testid="iconNodeId">icon</div>}
                />
            </SideBarDisplayModeContext.Provider>
        );

        expect(container).toMatchSnapshot();
    });

    it('составляет правильную ссылку с переданным табом', () => {
        render(
            <SideBarDisplayModeContext.Provider value={{expanded: true, toggleDisplayMode: jest.fn()}}>
                <SideBarMenuItem
                    mainPath="/mainPath"
                    tab="activeTab"
                    label="MenuItem1"
                    icon={<div data-testid="iconNodeId">icon</div>}
                />
            </SideBarDisplayModeContext.Provider>
        );

        const anchor = screen.getByRole('link');

        expect(anchor).toHaveAttribute('href', '/mainPath/activeTab');
    });
});
