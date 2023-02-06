#pragma once

#include <market/library/local_delivery_mms/writer.h>

namespace Market {
    static const NDatetime::TSimpleTM TEST_TIME(1985, 6, 24);
    static const ui32 TEST_CALENDAR_TABLE_COUNT = 7;
    static const ui32 TEST_INTERVAL_TABLE_SIZE = 45;

    ui32 AddZeroTestCalendar(NDelivery::TLocalDeliveryMmsWriter& writer, ui32 calendarTableCount = TEST_CALENDAR_TABLE_COUNT, ui32 intervalTableSize = TEST_INTERVAL_TABLE_SIZE, const NDatetime::TSimpleTM baseDate = TEST_TIME) {
        NDelivery::TIntervalCalendar calendar;
        NDatetime::TSimpleTM date = baseDate;
        date.Add(NDatetime::TSimpleTM::F_DAY, -1);
        for (ui32 i1 = 0; i1 < calendarTableCount; i1++) {
            NDelivery::TIntervalTableMms<mms::Standalone> offsets;
            offsets.Date.Day = date.MDay;
            offsets.Date.Month = date.RealMonth();
            offsets.Date.Year = date.RealYear();
            for (size_t i2 = 0; i2 < intervalTableSize; i2++) {
                offsets.Intervals.push_back(i2);
            }
            calendar.push_back(offsets);
            date.Add(NDatetime::TSimpleTM::F_DAY, 1);
        }
        return writer.AddCalendar(calendar);
    }
}
