export enum MessageType {
    MerchantInit = 'merchant-init',
    MerchantMessage = 'merchant-message',
    MerchantClose = 'merchant-close',
    EventManagerInit = 'event-manager-init',
    EventManagerMessage = 'event-manager-message',
    EventManagerClose = 'event-manager-close',
}

export enum ResponseMessageType {
    Connected = 'connected',
    Disconnected = 'disconnected',
    Event = 'event',
}

export enum ConnectionStatus {
    Success = 'success',
    Failed = 'failed',
    NotFound = 'not_found',
}
