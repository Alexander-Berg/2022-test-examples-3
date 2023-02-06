import test from 'ava';

import {
    applyTimezone,
    getTimezoneOffset,
    getCurrentTimezoneOffset,
    addHours,
    removeTimezone,
} from '../../../src/lib/date';
import { monday } from '../../helpers/date';

const makeApplyRemoveTimezoneTest = (date: Date, timezone: string) => {
    test(`Should apply ${timezone}`, (t) => {
        const newDay = applyTimezone(date, timezone);

        // Чтобы получить время в указанной таймзоне, нужно вычесть текущий оффсет и добавить оффсет таймзоны
        const newOffset = getTimezoneOffset(timezone);
        const currentOffset = getCurrentTimezoneOffset(date);

        const sameDay = addHours(date, newOffset - currentOffset);

        const originalDay = removeTimezone(sameDay, timezone);

        t.is(newDay.getTime(), sameDay.getTime());
        t.is(date.getTime(), originalDay.getTime());
    });
};

makeApplyRemoveTimezoneTest(monday, 'Asia/Atyrau'); // +5
makeApplyRemoveTimezoneTest(monday, 'Pacific/Pago_Pago'); // -11
makeApplyRemoveTimezoneTest(monday, 'America/El_Salvador'); // -6
makeApplyRemoveTimezoneTest(monday, 'Europe/Isle_of_Man'); // 0
makeApplyRemoveTimezoneTest(monday, 'Asia/Urumqi'); // 6
makeApplyRemoveTimezoneTest(monday, 'Pacific/Tarawa'); // 12
