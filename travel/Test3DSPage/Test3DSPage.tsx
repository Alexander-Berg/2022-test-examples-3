import React from 'react';
import {Route, Switch} from 'react-router-dom';

import {URLs} from 'constants/urls';

import Box from 'components/Box/Box';
import Test3DSDemoPage from './components/Test3DSDemoPage/Test3DSDemoPage';
import Test3DSFramePage from './components/Test3DSFramePage/Test3DSFramePage';
import Test3DSExternalDemoPage from 'projects/testControlPanel/pages/Test3DSPage/components/Test3DSExternalDemoPage/Test3DSExternalDemoPage';

interface ITest3DSPageProps {}

const Test3DSPage: React.FC<ITest3DSPageProps> = () => {
    return (
        <Box between={5}>
            <Switch>
                <Route
                    exact
                    path={URLs.testControlPanel3DSDemo}
                    component={Test3DSDemoPage}
                />

                <Route
                    path={URLs.testControlPanel3DSFrame}
                    exact
                    component={Test3DSFramePage}
                />

                <Route
                    path={URLs.testControlPanel3DSExternalDemo}
                    exact
                    component={Test3DSExternalDemoPage}
                />
            </Switch>
        </Box>
    );
};

export default Test3DSPage;
