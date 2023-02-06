import { BackendConfig } from '../../../typings/config';

export function configMockFactory() {
    return {
        createState: (state: Partial<BackendConfig> = {}): BackendConfig => ({
            hidden_namespaces: [],
            namespaces_without_phone_requirement: [],
            hidden_invite_link_namespaces: [],
            reactions_enabled: true,
            reactions_by_namespace: {},
            voice_messages: {
                max_duration_s: 100,
            },
            ...state,
        }),
    };
}
