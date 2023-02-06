import { checkRestrictions, GroupChatConfig, PrivateChatConfig } from '../config';

const ON = 'enabled';
const OFF = 'disabled';

const privateChat: PrivateChatConfig = { private: true, is_robot: false };
const robotChat: PrivateChatConfig = { private: true, is_robot: true };
const publicChat1: GroupChatConfig = { private: false, channel: false, namespace: 1, is_business: false };
const channel0: GroupChatConfig = { private: false, channel: true, namespace: 0, is_business: false };

describe('Feature config', () => {
    describe('Private chat', () => {
        describe('Default disabled', () => {
            it('Not other config', () => {
                expect(checkRestrictions(privateChat, { default: OFF })).toBeFalsy();
            });
            it('Private disabled', () => {
                expect(checkRestrictions(privateChat, { default: OFF, private: OFF })).toBeFalsy();
            });
            it('Private enabled', () => {
                expect(checkRestrictions(privateChat, { default: OFF, private: ON })).toBeTruthy();
            });
        });

        describe('Default enabled', () => {
            it('Not other config', () => {
                expect(checkRestrictions(privateChat, { default: ON })).toBeTruthy();
            });
            it('Private disabled', () => {
                expect(checkRestrictions(privateChat, { default: ON, private: OFF })).toBeFalsy();
            });
            it('Private enabled', () => {
                expect(checkRestrictions(privateChat, { default: ON, private: ON })).toBeTruthy();
            });
        });
    });

    describe('Robot chat', () => {
        describe('Default disabled', () => {
            it('Not other config', () => {
                expect(checkRestrictions(robotChat, { default: OFF })).toBeFalsy();
            });
            it('Robots disabled', () => {
                expect(checkRestrictions(robotChat, { default: OFF, robots: OFF })).toBeFalsy();
            });
            it('Robots enabled', () => {
                expect(checkRestrictions(robotChat, { default: OFF, robots: ON })).toBeTruthy();
            });
        });

        describe('Default enabled', () => {
            it('Not other config', () => {
                expect(checkRestrictions(robotChat, { default: ON })).toBeTruthy();
            });
            it('Robots disabled', () => {
                expect(checkRestrictions(robotChat, { default: ON, robots: OFF })).toBeFalsy();
            });
            it('Robots enabled', () => {
                expect(checkRestrictions(robotChat, { default: ON, robots: ON })).toBeTruthy();
            });
        });
    });

    describe('Chat', () => {
        describe('Default disabled', () => {
            it('Not other config', () => {
                expect(checkRestrictions(publicChat1, { default: OFF })).toBeFalsy();
            });
            it('Enabled channel 1', () => {
                expect(checkRestrictions(publicChat1, { default: OFF, enabled: { channelsNS: [1] } })).toBeFalsy();
            });
            it('Enabled chat 0', () => {
                expect(checkRestrictions(publicChat1, { default: OFF, enabled: { groupsNS: [0] } })).toBeFalsy();
            });
            it('Enabled chat 1', () => {
                expect(checkRestrictions(publicChat1, { default: OFF, enabled: { groupsNS: [1] } })).toBeTruthy();
            });
        });

        describe('Default enabled', () => {
            it('Not other config', () => {
                expect(checkRestrictions(publicChat1, { default: ON })).toBeTruthy();
            });
            it('Enabled channel 1', () => {
                expect(checkRestrictions(publicChat1, { default: ON, disabled: { channelsNS: [1] } })).toBeTruthy();
            });
            it('Enabled chat 0', () => {
                expect(checkRestrictions(publicChat1, { default: ON, disabled: { groupsNS: [0] } })).toBeTruthy();
            });
            it('Enabled chat 1', () => {
                expect(checkRestrictions(publicChat1, { default: ON, disabled: { groupsNS: [1] } })).toBeFalsy();
            });
        });
    });

    describe('Channel', () => {
        describe('Default disabled', () => {
            it('Not other config', () => {
                expect(checkRestrictions(channel0, { default: OFF })).toBeFalsy();
            });
            it('Enabled group 0', () => {
                expect(checkRestrictions(channel0, { default: OFF, enabled: { groupsNS: [0] } })).toBeFalsy();
            });
            it('Enabled channel 1', () => {
                expect(checkRestrictions(channel0, { default: OFF, enabled: { channelsNS: [1] } })).toBeFalsy();
            });
            it('Enabled channel 0', () => {
                expect(checkRestrictions(channel0, { default: OFF, enabled: { channelsNS: [0] } })).toBeTruthy();
            });
        });

        describe('Default enabled', () => {
            it('Not other config', () => {
                expect(checkRestrictions(channel0, { default: ON })).toBeTruthy();
            });
            it('Disabled chat 0', () => {
                expect(checkRestrictions(channel0, { default: ON, disabled: { groupsNS: [0] } })).toBeTruthy();
            });
            it('Disabled channel 1', () => {
                expect(checkRestrictions(channel0, { default: ON, disabled: { channelsNS: [1] } })).toBeTruthy();
            });
            it('Disabled channel 0', () => {
                expect(checkRestrictions(channel0, { default: ON, disabled: { channelsNS: [0] } })).toBeFalsy();
            });
        });
    });
});
