package daysoff

import (
	"bytes"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	yu "a.yandex-team.ru/market/combinator/pkg/ytutil"
)

func TestReadYtDaysOff(t *testing.T) {
	testDaysOff := []string{
		`{"service_id":1991298,"daysoff_dates":{"days":[{"dayOff":"2021-05-05","created":"2021-07-05T15:41:00.385072"},{"dayOff":"2021-05-06","created":"2021-07-05T15:42:00.385072"},{"dayOff":"2021-05-09","created":"2021-07-05T15:43:00.385072"},{"dayOff":"2021-05-10","created":"2021-07-05T15:44:00.385072"},{"dayOff":"2021-05-11","created":"2021-07-05T15:45:00.385072"},{"dayOff":"2021-05-12","created":"2021-07-05T15:46:00.385072"}],"endDate":"2021-07-11","startDate":"2021-05-02"}}`,
		`{"service_id":1991300,"daysoff_dates":{"days":[{"dayOff":"2021-05-05","created":"2021-07-04T15:31:00.385072"},{"dayOff":"2021-05-06","created":"2021-07-04T15:32:00.385072"}],"endDate":"2021-07-07","startDate":"2021-04-28"}}`,
		`{"service_id":1984271,"daysoff_dates":{"endDate":"2021-07-11","startDate":"2021-05-02","days":[{"dayOff":"2021-05-11"},{"dayOff":"2021-05-12"}]}}`,
		// next one has empty list of dates hence will be ignored
		`{"service_id":1984273,"daysoff_dates":{"endDate":"2021-07-08","startDate":"2021-04-29","days":[]}}`,
		`{"service_id":1991200,"daysoff_dates":{"dates":["2021-05-05","2021-05-06","2021-05-09","2021-05-10","2021-05-11","2021-05-12"],"endDate":"2021-07-11","startDate":"2021-05-02","days":[{"dayOff":"2021-05-05","created":"2021-07-06T15:40:01.385072"},{"dayOff":"2021-05-06","created":"2021-07-06T15:40:06.385072"},{"dayOff":"2021-05-09","created":"2021-07-06T15:40:05.385072"},{"dayOff":"2021-05-10","created":"2021-07-06T15:40:04.385072"},{"dayOff":"2021-05-11","created":"2021-07-06T15:40:03.385072"},{"dayOff":"2021-05-12","created":"2021-07-06T15:40:02.385072"}]}}`,
	}

	wantDaysOff := &ServiceDaysOff{
		Services: map[int64]DisabledDatesMap{
			1991298: {
				125: NewDisabledDate(time.Date(2021, 7, 5, 15, 41, 0, 385072000, time.UTC)),
				126: NewDisabledDate(time.Date(2021, 7, 5, 15, 42, 0, 385072000, time.UTC)),
				129: NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
			1991300: {
				125: NewDisabledDate(time.Date(2021, 7, 4, 15, 31, 0, 385072000, time.UTC)),
				126: NewDisabledDate(time.Date(2021, 7, 4, 15, 32, 0, 385072000, time.UTC)),
			},
			1984271: {
				131: DisabledDate{},
				132: DisabledDate{},
			},
			1991200: {
				125: NewDisabledDate(time.Date(2021, 7, 6, 15, 40, 01, 385072000, time.UTC)),
				126: NewDisabledDate(time.Date(2021, 7, 6, 15, 40, 06, 385072000, time.UTC)),
				129: NewDisabledDate(time.Date(2021, 7, 6, 15, 40, 05, 385072000, time.UTC)),
				130: NewDisabledDate(time.Date(2021, 7, 6, 15, 40, 04, 385072000, time.UTC)),
				131: NewDisabledDate(time.Date(2021, 7, 6, 15, 40, 03, 385072000, time.UTC)),
				132: NewDisabledDate(time.Date(2021, 7, 6, 15, 40, 02, 385072000, time.UTC)),
			},
		},
	}

	do := NewServiceDaysOff()

	for _, yson := range testDaysOff {
		buf := bytes.NewBufferString(yson)
		reader, err := yu.NewFileReader2(buf)
		require.NoError(t, err)

		err = do.readDaysOff(reader)
		require.NoError(t, err)
	}

	require.Equal(t, len(wantDaysOff.Services), len(do.Services))
	require.Equal(t, wantDaysOff, do)
}
