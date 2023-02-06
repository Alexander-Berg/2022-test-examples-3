import { ConfigUpdatingState } from '../updating';
import MediaSessionContextMock from '../../../__mocks__/context';
import { OfferCreatingState } from '../../offer/creating';

describe('ConfigUpdatingState', () => {
    let context;
    let params;
    let state;

    beforeEach(() => {
        context = new MediaSessionContextMock();
        params = {
            config: {
                rtc_configuration: {
                    ice_servers: ['server1', 'server2'],
                    ice_transport_policy: 'all',
                },
            },
        };
        state = new ConfigUpdatingState(context, params);
    });

    describe('#enter', () => {
        it('Log config', () => {
            state.enter();

            expect(context.log).toBeCalledWith({
                action: 'updateConfig',
                data: {
                    rtc_configuration: {
                        ice_servers: ['server1', 'server2'],
                        ice_transport_policy: 'all',
                    },
                },
            });
        });

        it('Update connection with correct params', () => {
            state.enter();

            expect(context.connection.update).toBeCalledWith({
                iceServers: ['server1', 'server2'],
                iceTransportPolicy: 'all',
            });
        });

        it('Do not set interval if no media session config', () => {
            state.enter();

            expect(context.statsSender.setInterval).not.toBeCalled();
        });

        it('Set interval from media session config', () => {
            params.config.mediasession_configuration = { keepalive_interval: 20000 };

            state.enter();

            expect(context.statsSender.setInterval).toBeCalledWith(20000);
        });

        it('Change state to OfferCreatingState', () => {
            state.enter();

            expect(context.state).toBeInstanceOf(OfferCreatingState);
        });
    });
});
