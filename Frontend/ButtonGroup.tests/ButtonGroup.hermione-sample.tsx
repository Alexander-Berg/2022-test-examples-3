import React from 'react';
import { render } from 'react-dom';

import { BPage } from '../../internal/components/BPage/BPage';
import { Hermione } from '../../internal/components/Hermione/Hermione';

import { configureRootTheme } from '../../Theme';
import { theme as themeDefault } from '../../Theme/presets/default';

import { ButtonGroup as ButtonGroupDesktop } from '../desktop/bundle';
import { Button } from '../../Button/desktop/bundle';

configureRootTheme({ theme: themeDefault });

render(
    <BPage>
        <Hermione id="default">
            <ButtonGroupDesktop>
                <Button view="action" size="m">
                    Button 1
                </Button>
                <Button view="action" size="m">
                    Button 2
                </Button>
                <Button view="action" size="m">
                    Button 3
                </Button>
            </ButtonGroupDesktop>
        </Hermione>
        <Hermione id="vertical">
            <Hermione element="Item">
                <ButtonGroupDesktop vertical>
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
        </Hermione>
        <Hermione id="gap">
            <Hermione element="Item" className="s">
                <ButtonGroupDesktop gap="s">
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="m">
                <ButtonGroupDesktop gap="m">
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="l">
                <ButtonGroupDesktop gap="l">
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="xl">
                <ButtonGroupDesktop gap="xl">
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="s-vertical">
                <ButtonGroupDesktop gap="s" vertical>
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="m-vertical">
                <ButtonGroupDesktop gap="m" vertical>
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="l-vertical">
                <ButtonGroupDesktop gap="l" vertical>
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="xl-vertical">
                <ButtonGroupDesktop gap="xl" vertical>
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
        </Hermione>
        <Hermione id="pin">
            <Hermione element="Item" className="pin-circle">
                <ButtonGroupDesktop pin="circle">
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="pin-round">
                <ButtonGroupDesktop pin="round">
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="pin-circle-vertical">
                <ButtonGroupDesktop pin="circle" vertical>
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="pin-round-vertical">
                <ButtonGroupDesktop pin="round" vertical>
                    <Button view="action" size="m">
                        Button 1
                    </Button>
                    <Button view="action" size="m">
                        Button 2
                    </Button>
                    <Button view="action" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>

            <Hermione element="Item" className="pin-circle-link">
                <ButtonGroupDesktop pin="circle">
                    <Button view="link" size="m">
                        Button 1
                    </Button>
                    <Button view="link" size="m">
                        Button 2
                    </Button>
                    <Button view="link" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="pin-round-link">
                <ButtonGroupDesktop pin="round">
                    <Button view="link" size="m">
                        Button 1
                    </Button>
                    <Button view="link" size="m">
                        Button 2
                    </Button>
                    <Button view="link" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="pin-circle-link-vertical">
                <ButtonGroupDesktop pin="circle" vertical>
                    <Button view="link" size="m">
                        Button 1
                    </Button>
                    <Button view="link" size="m">
                        Button 2
                    </Button>
                    <Button view="link" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
            <Hermione element="Item" className="pin-round-link-vertical">
                <ButtonGroupDesktop pin="round" vertical>
                    <Button view="link" size="m">
                        Button 1
                    </Button>
                    <Button view="link" size="m">
                        Button 2
                    </Button>
                    <Button view="link" size="m">
                        Button 3
                    </Button>
                </ButtonGroupDesktop>
            </Hermione>
        </Hermione>
    </BPage>,
    document.getElementById('root'),
);
