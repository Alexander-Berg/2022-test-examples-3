package dtutil

import (
	"math/rand"
	"reflect"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestStrDateToInt_ValidValue(t *testing.T) {
	result := StringDate("20190212").ToIntDate()

	assert.Equal(t, IntDate(20190212), result)
}

func TestStrDateToInt_ValueWithDashes(t *testing.T) {
	result := StringDate("2019-02-12").ToIntDate()

	assert.Equal(t, IntDate(20190212), result)
}

func TestStrDateToInt_LongValue(t *testing.T) {
	result := StringDate("2019-02-12T14:34:32").ToIntDate()

	assert.Equal(t, IntDate(20190212), result)
}

func TestStrDateToInt_InvalidValue(t *testing.T) {
	result := StringDate("2019.02129").ToIntDate()

	assert.Equal(t, IntDate(0), result)
}

func TestAddDaysP_Basic(t *testing.T) {
	result := StringDate("2020-02-28").AddDaysP(2)

	assert.Equal(t, StringDate("2020-03-01"), result)
}

func TestIntTime_String(t *testing.T) {
	tests := []struct {
		name string
		time IntTime
		want string
	}{
		{
			"midnight",
			0,
			"00:00:00",
		},
		{
			"00:01:00",
			1,
			"00:01:00",
		},
		{
			"00:10:00",
			10,
			"00:10:00",
		},
		{
			"01:10:00",
			110,
			"01:10:00",
		},
		{
			"11:10:00",
			1110,
			"11:10:00",
		},
		{
			"23:59:00",
			2359,
			"23:59:00",
		},
		{
			"invalid time",
			2400,
			"<Invalid time 2400>",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.time.String(); got != tt.want {
				t.Errorf("String() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFormatDateIso(t *testing.T) {

	tests := []struct {
		name string
		t    time.Time
		want StringDate
	}{
		{
			"20th century",
			time.Date(1999, 12, 31, 0, 0, 0, 0, time.UTC),
			"1999-12-31",
		},
		{
			"21st century",
			time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC),
			"2000-01-01",
		},
		{
			"sequential jibbrish",
			time.Date(9876, 10, 21, 0, 0, 0, 0, time.UTC),
			"9876-10-21",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := FormatDateIso(tt.t); got != tt.want {
				t.Errorf("FormatDateIso() = %v, want %v", got, tt.want)
			}
		})
	}
}

func BenchmarkFormatDateTimeISO(b *testing.B) {
	for i := 0; i < b.N; i++ {
		FormatDateTimeISO(time.Date(9876, 10, 21, 0, 0, 0, 0, time.UTC))
	}
}

func BenchmarkFormatDateTimeISOSlow(b *testing.B) {
	for i := 0; i < b.N; i++ {
		time.Date(9876, 10, 21, 0, 0, 0, 0, time.UTC).Format(IsoDateTime)
	}
}

func BenchmarkFormatDateISO(b *testing.B) {
	for i := 0; i < b.N; i++ {
		FormatDateIso(time.Date(9876, 10, 21, 0, 0, 0, 0, time.UTC))
	}
}

func FormatDateIsoSlow(t time.Time) string {
	return t.Format(IsoDate)
}

func BenchmarkFormatDateISOSlow(b *testing.B) {
	for i := 0; i < b.N; i++ {
		FormatDateIsoSlow(time.Date(9876, 10, 21, 0, 0, 0, 0, time.UTC))
	}
}

func TestFormatTimeIso(t *testing.T) {
	type args struct {
		t time.Time
	}
	tests := []struct {
		name string
		t    time.Time
		want string
	}{
		{
			"12:34:56",
			time.Date(1234, 11, 22, 12, 34, 56, 9999, time.UTC),
			"12:34:56",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := FormatTimeIso(tt.t); got != tt.want {
				t.Errorf("FormatTimeIso() = %v, want %v", got, tt.want)
			}
		})
	}
}

func BenchmarkFormatTimeIso(b *testing.B) {
	for i := 0; i < b.N; i++ {
		FormatTimeIso(time.Date(1234, 11, 22, 12, 34, 56, 9999, time.UTC))
	}
}

func TestStringDate_ToTime(t *testing.T) {
	tests := []struct {
		name string
		date StringDate
		tz   *time.Location
		want time.Time
	}{
		{
			"1999-12-31",
			"1999-12-31",
			time.UTC,
			time.Date(1999, 12, 31, 0, 0, 0, 0, time.UTC),
		},
		{
			"1999-12-31 compact",
			"19991231",
			time.UTC,
			time.Date(1999, 12, 31, 0, 0, 0, 0, time.UTC),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.date.ToTime(tt.tz); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ToTime() = %v, want %v", got, tt.want)
			}
		})
	}
}

func BenchmarkStringDateToTime(b *testing.B) {
	for i := 0; i < b.N; i++ {
		StringDate("2006-01-02").ToTime(time.UTC)
	}
}
func BenchmarkStringDateToTimeSlow(b *testing.B) {
	for i := 0; i < b.N; i++ {
		StringDateToTimeSlow("2006-01-02", time.UTC)
	}
}

func StringDateToTimeSlow(stringDate string, tz *time.Location) time.Time {
	dt, _ := time.ParseInLocation(
		"20060102",
		strings.ReplaceAll(string(stringDate), "-", ""),
		tz,
	)
	return dt
}

func Benchmark_Itoa(b *testing.B) {
	data := make([]int, b.N)
	for i := range data {
		data[i] = int(rand.Uint32())
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		strconv.Itoa(data[i])
	}
}

func Benchmark_strconv_FormatUint(b *testing.B) {
	data := make([]int, b.N)
	for i := range data {
		data[i] = int(rand.Uint32())
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		strconv.FormatUint(uint64(data[i]), 10)
	}
}

func TestIntDate_StringDateDashed(t *testing.T) {
	tests := []struct {
		name string
		date IntDate
		want StringDate
	}{
		{
			"2020-01-02",
			20200102,
			"2020-01-02",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.date.StringDateDashed(); got != tt.want {
				t.Errorf("StringDateDashed() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIntDate_StringDate(t *testing.T) {
	tests := []struct {
		name string
		date IntDate
		want StringDate
	}{
		{
			"2020-01-02",
			20200102,
			"20200102",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.date.StringDate(); got != tt.want {
				t.Errorf("StringDate() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFormatDateTimeISO(t *testing.T) {
	type TEST struct {
		name string
		t    time.Time
		want string
	}
	var tests []TEST

	var i int64 = 0
	rand.Seed(0)
	for i = 0; i < (2 << 31); i += int64(rand.Intn(100000000) + rand.Intn(10000000)) {
		dt := time.Unix(i, 0)
		want := dt.Format(IsoDateTime)
		tests = append(tests, TEST{
			name: want,
			t:    dt,
			want: want,
		})
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := FormatDateTimeISO(tt.t); got != tt.want {
				t.Errorf("FormatDateTimeISO() = %v, want %v", got, tt.want)
			}
		})
	}
}
