import type {PartnerOrderProcessingSettingsDTO} from '~/app/bcm/mbiPartner/Client/PartnerSettingsClient/types';

export default (draft: Partial<PartnerOrderProcessingSettingsDTO> = {}): PartnerOrderProcessingSettingsDTO => ({
    partnerId: 10263850,
    isApiParamsReady: false,
    isPartnerInterface: false,
    isPartnerInterfaceProhibited: false,
    shopSchedule: {
        id: 10263850,
        lines: [
            {
                startDay: 'MONDAY',
                endDay: 'SUNDAY',
                days: 6,
                startMinute: 540,
                minutes: 600,
                startTime: '9:00',
                endTime: '19:00',
            },
        ],
    },
    maxOrdersInShipment: 50,
    ...draft,
});
