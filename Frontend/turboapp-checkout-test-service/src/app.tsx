import React from 'react';
import { Switch, Route } from 'react-router-dom';

import { UrlTemplate } from './lib/url-builder';
import MainScreen from './screens/MainScreen';
import EventManagerScreen from './screens/EventManagerScreen';
import TestMerchantScreen from './screens/TestMerchantScreen';
import TestPaymentFormScreen from './screens/TestPaymentFormScreen';

const App: React.FC = () => {
    return (
        <Switch>
            <Route path={UrlTemplate.TestMerchant} exact>
                <TestMerchantScreen />
            </Route>
            <Route path={UrlTemplate.EventManager} exact>
                <EventManagerScreen />
            </Route>
            <Route path={UrlTemplate.TestPaymentForm} exact>
                <TestPaymentFormScreen />
            </Route>
            <Route path="*">
                <MainScreen />
            </Route>
        </Switch>
    );
};

export default App;
