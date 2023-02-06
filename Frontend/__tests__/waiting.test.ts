import { ConfigWaitingState } from '../waiting';
import MediaSessionContextMock from '../../../__mocks__/context';
import { MediaAcquiringState } from '../../media/acquiring';
import { OfferWaitingState } from '../../offer/waiting';

describe('ConfigWaitingState', () => {
    let context;
    let state;

    beforeEach(() => {
        context = new MediaSessionContextMock();
        state = new ConfigWaitingState(context);
    });

    describe('#enter', () => {
        it('Subscribe to onConfig event', () => {
            state.enter();

            expect(context.mediator.onConfig.addListener).toBeCalledWith(state.handleConfig);
        });
    });

    describe('#exit', () => {
        it('Unsubscribe to onConfig event', () => {
            state.exit();

            expect(context.mediator.onConfig.removeListener).toBeCalledWith(state.handleConfig);
        });
    });

    describe('#handleConfig', () => {
        let config;

        beforeEach(() => {
            config = {
                rtc_configuration: {
                    ice_servers: ['server1', 'server2'],
                    ice_transport_policy: 'all',
                },
            };
        });

        it('Log config', () => {
            state.handleConfig({ config });

            expect(context.log).toBeCalledWith({
                action: 'setConfig',
                data: {
                    rtc_configuration: {
                        ice_servers: ['server1', 'server2'],
                        ice_transport_policy: 'all',
                    },
                },
            });
        });

        it('Open connection with correct params', () => {
            state.handleConfig({ config });

            expect(context.connection.open).toBeCalledWith({
                iceServers: ['server1', 'server2'],
                iceTransportPolicy: 'all',
                sdpSemantics: 'unified-plan',
            });
        });

        it('Set semantics from config', () => {
            config.rtc_configuration.sdp_semantics = 'plan-b';

            state.handleConfig({ config });

            expect(context.connection.open).toBeCalledWith({
                iceServers: ['server1', 'server2'],
                iceTransportPolicy: 'all',
                sdpSemantics: 'plan-b',
            });
        });

        it('Set semantics from context', () => {
            context.sdpSemantics = 'plan-b';

            state.handleConfig({ config });

            expect(context.connection.open).toBeCalledWith({
                iceServers: ['server1', 'server2'],
                iceTransportPolicy: 'all',
                sdpSemantics: 'plan-b',
            });
        });

        it('Do not set interval if no media session config', () => {
            state.handleConfig({ config });

            expect(context.statsSender.setInterval).not.toBeCalled();
        });

        it('Set interval from media session config', () => {
            config.mediasession_configuration = { keepalive_interval: 20000 };

            state.handleConfig({ config });

            expect(context.statsSender.setInterval).toBeCalledWith(20000);
        });

        it('Change state to MediaAcquiringState for outgoing call', () => {
            state.handleConfig({ config });

            expect(context.state).toBeInstanceOf(MediaAcquiringState);
        });

        it('Change state to OfferWaitingState for incoming call', () => {
            context.isOutgoing = false;

            state.handleConfig({ config });

            expect(context.state).toBeInstanceOf(OfferWaitingState);
        });
    });
});
