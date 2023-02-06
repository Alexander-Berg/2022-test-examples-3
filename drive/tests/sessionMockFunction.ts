import { Dict } from '../../../../../types';
import {
    DELEGATION_TYPE,
    fee_drop_zone_max,
    IModelItem,
    ISessionInfo,
    Origin,
    SESSION_TYPE,
} from '../../../../models/session';
import { AccountType, PaymentSystem } from '../../../Settings/Wallets/types';

export class MockSession {
    sessions?: ISessionInfo[];
    has_more?: boolean;
    models?: Dict<IModelItem>;

    constructor(props?: MockSession) {
        const { sessions = [], has_more = false, models = {} } = props || {};
        this.sessions = sessions;
        this.has_more = has_more;
        this.models = models;
    }
}

export class MockSessionHandler {
    sessionData: MockSession;

    constructor(sessionData: MockSession) {
        this.sessionData = sessionData;
    }

    clear() {
        delete this.sessionData;
    }

    generateCompileSession() {
        this.sessionData.sessions = [
            {
                segment: {
                    __type: SESSION_TYPE.RIDING_COMPILATION,
                },
            } as ISessionInfo,
        ];

        return this.sessionData;
    }

    generateInsurance() {
        this.sessionData.sessions && (this.sessionData.sessions[0].insurance_notifications = [
            {
                sent: 504997200,
                finish: 504997200,
                created: 504997200,
                start: 504997200,
                id: 0,
            },
        ]);
    }

    generateCommonSession() {
        this.sessionData.sessions = [
            {
                offer_proto: {},
                segment: {
                    __type: SESSION_TYPE.EVENTS_SESSION,
                    events: [{
                        timestamp: 1,
                        action: 'action',
                        event_id: '2',
                        tag_name: 'tag_name',
                        user_id: '3',
                    }],
                    meta: {
                        start: 3,
                        finish: 4,
                    },
                    session: {
                        specials: {
                            bill: [{
                                type: 'bill_type',
                                title: 'bill title',
                                cost: 111,
                            }],
                        },
                    },
                },

            },
            {
                offer_proto: {},
                segment: {
                    __type: SESSION_TYPE.EVENTS_SESSION,
                    events: [{
                        timestamp: 1,
                        action: 'action',
                        event_id: '2',
                        tag_name: 'tag_name',
                        user_id: '3',
                    }],
                    meta: {
                        start: 1,
                        finish: 2,
                    },
                    session: {
                        specials: {
                            bill: [{
                                type: 'bill_type',
                                title: 'bill title',
                                cost: 111,
                            }],
                        },
                    },
                },

            },
        ] as any[];

        return this.sessionData;
    }

    makeBluetoothBadge(sessionIndex = 0, eventsIndex = 0) {
        this.generateCommonSession();

        this.sessionData?.sessions?.[sessionIndex]?.segment
            ?.events?.[eventsIndex]
        && (this.sessionData.sessions[sessionIndex]
            .segment.events[eventsIndex].transformation_skipped_by_external_command = true);
    }

    makeDouble(sessionIndex = 0) {
        this.generateCommonSession();
        this.sessionData?.sessions?.[sessionIndex]?.segment?.meta?.start
        && (this.sessionData.sessions[sessionIndex].segment.meta.start = 1);

        this.sessionData?.sessions?.[sessionIndex + 1]?.segment?.meta?.finish
        && (this.sessionData.sessions[sessionIndex].segment.meta.finish = 2);
    }

    makeEvolution(sessionIndex = 0) {
        this.generateCommonSession();
        this.sessionData?.sessions?.[sessionIndex]?.offer_proto
        && (this.sessionData.sessions[sessionIndex].offer_proto.ParentId = '1111111');
    }

    makeCorpBadge(sessionIndex = 0) {
        this.generateCommonSession();
        this.sessionData?.sessions?.[sessionIndex]?.offer_proto
        && (this.sessionData.sessions[sessionIndex].offer_proto.SelectedCharge = AccountType.YAMONEY);
    }

    makeYac(sessionIndex = 0) {
        this.generateCommonSession();
        this.sessionData?.sessions?.[sessionIndex]?.offer_proto
        && (this.sessionData.sessions[sessionIndex].offer_proto.SelectedCharge = PaymentSystem.YANDEX_ACCOUNT);
    }

    makeTaxi(sessionIndex = 0) {
        this.generateCommonSession();
        this.sessionData?.sessions?.[sessionIndex]?.offer_proto
        && (this.sessionData.sessions[sessionIndex].offer_proto.Origin = Origin.TAXI);
    }

    makeLongTerm(sessionIndex = 0) {
        this.generateCommonSession();
        this.sessionData?.sessions?.[sessionIndex]?.offer_proto
        && (this.sessionData.sessions[sessionIndex].offer_proto['LongTermOffer'] = {});
    }

    makeFee(sessionIndex = 0, billIndex = 0) {
        this.generateCommonSession();
        this.sessionData?.sessions?.[sessionIndex]?.segment?.session?.specials?.bill?.[billIndex]
        && (this.sessionData.sessions[sessionIndex]
            .segment.session.specials.bill[billIndex].type = fee_drop_zone_max);
    }

    makeVoyage(sessionIndex = 0) {
        this.generateCommonSession();
        this.sessionData?.sessions?.[sessionIndex]?.offer_proto
        && (this.sessionData.sessions[sessionIndex].offer_proto.Marker = 'bulls_manor_5');
    }

    setTransferredFrom(sessionIndex = 0) {
        this.sessionData?.sessions?.[sessionIndex]?.offer_proto
        && (this.sessionData.sessions[sessionIndex].offer_proto.TransferredFrom = '1111');
    }

    setDelegationType(sessionIndex = 0, delegationType: DELEGATION_TYPE) {
        this.sessionData?.sessions?.[sessionIndex]?.segment
        && (this.sessionData.sessions[sessionIndex].segment.delegation_type = delegationType);
    }

    setTransferredType(sessionIndex = 0, TransferType: number) {
        this.sessionData?.sessions?.[sessionIndex]?.offer_proto
        && (this.sessionData.sessions[sessionIndex].offer_proto.TransferType = TransferType);
    }

    setTraceTags(sessionIndex = 0, traceTags: string[]) {
        this.sessionData?.sessions?.[sessionIndex]
        && (this.sessionData.sessions[sessionIndex].trace_tags = traceTags);
    }

}

export const sessionMockFunction = () => {
    const sessionsData: any = {
        sessions: [],
        has_more: false,
        models: {},
    };
    const sessionInfoCommon = {
        segment: {
            __type: SESSION_TYPE.EVENTS_SESSION,
        },
    };

    const sessionInfo = { ...sessionInfoCommon };
    const sessionInfoCompiled = {
        ...sessionInfoCommon, ...{
            segment: {
                __type: SESSION_TYPE.RIDING_COMPILATION,
            },
        },
    };

    sessionsData.sessions = [sessionInfo, sessionInfoCompiled];

    return sessionsData;
};
