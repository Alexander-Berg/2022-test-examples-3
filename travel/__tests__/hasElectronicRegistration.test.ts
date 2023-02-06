import {TransportType} from '../../../transportType';

import hasElectronicRegistration from '../hasElectronicRegistration';

describe('hasElectronicRegistration', () => {
    [TransportType.train, TransportType.bus].forEach(transportType => {
        it(`Для сегмента с типом транспорта "${transportType}" вернет true, если у него есть элетроннная регистрация`, () => {
            expect(hasElectronicRegistration(transportType)).toBe(false);
            expect(hasElectronicRegistration(transportType, false)).toBe(false);
            expect(hasElectronicRegistration(transportType, true)).toBe(true);
        });
    });

    Object.values(TransportType)
        .filter(
            transportType =>
                transportType !== TransportType.train &&
                transportType !== TransportType.bus,
        )
        .forEach(transportType => {
            it(`Для типа траспорта "${transportType}" должна вернуть false вне зависимости от того,
            какой признак electronicTicket пришел с бэкенда, потому что электронная регистрация доступна только
            для поездов и автобусов`, () => {
                expect(hasElectronicRegistration(transportType, true)).toBe(
                    false,
                );
            });
        });
});
