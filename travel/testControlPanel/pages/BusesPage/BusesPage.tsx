import {memo, FC} from 'react';
import {Route, Switch} from 'react-router-dom';

import {URLs} from 'constants/urls';

import Box from 'components/Box/Box';
import Flex from 'components/Flex/Flex';
import NavLink from 'projects/testControlPanel/components/NavLink/NavLink';

import OrderPage from 'projects/testControlPanel/pages/BusesPage/pages/OrderPage/OrderPage';

import cx from './BusesPage.scss';

const BusesPage: FC = () => {
    return (
        <Box between={5}>
            <Flex between={3} inline>
                <NavLink
                    className={cx('link')}
                    to={URLs.testControlPanelBusesTestContext}
                    exact
                >
                    Заказ
                </NavLink>
            </Flex>

            <Switch>
                <Route path={URLs.testControlPanelBusesTestContext} exact>
                    <OrderPage />
                </Route>
            </Switch>
        </Box>
    );
};

export default memo(BusesPage);
