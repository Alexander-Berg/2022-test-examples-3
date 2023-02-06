package displaytime

import (
	"testing"
	"time"

	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
)

func TestBuilder_Build(t *testing.T) {
	userNow := testutils.ParseTime("2021-11-05T23:00:00")

	type args struct {
		start   time.Time
		end     time.Time
		useYear bool
	}

	tests := []struct {
		name string
		want string
		args args
	}{
		{
			"one day",
			"10 декабря",
			args{
				start: testutils.ParseTime("2021-12-10"),
				end:   testutils.ParseTime("2021-12-10"),
			},
		},
		{
			"one day use years",
			"10 декабря, 2021",
			args{
				start:   testutils.ParseTime("2021-12-10"),
				end:     testutils.ParseTime("2021-12-10"),
				useYear: true,
			},
		},
		{
			"many days",
			"10 — 14 декабря",
			args{
				start: testutils.ParseTime("2021-12-10"),
				end:   testutils.ParseTime("2021-12-14"),
			},
		},
		{
			"many days use years",
			"10 — 14 декабря, 2021",
			args{
				start:   testutils.ParseTime("2021-12-10"),
				end:     testutils.ParseTime("2021-12-14"),
				useYear: true,
			},
		},
		{
			"many month",
			"10 ноя — 10 дек",
			args{
				start: testutils.ParseTime("2021-11-10"),
				end:   testutils.ParseTime("2021-12-10"),
			},
		},
		{
			"many month use years",
			"10 ноя — 10 дек, 2021",
			args{
				start:   testutils.ParseTime("2021-11-10"),
				end:     testutils.ParseTime("2021-12-10"),
				useYear: true,
			},
		},
		{
			"many years",
			"10 дек — 10 фев",
			args{
				start: testutils.ParseTime("2021-12-10"),
				end:   testutils.ParseTime("2022-02-10"),
			},
		},
		{
			"many years",
			"10 дек, 2021 — 10 фев, 2022",
			args{
				start:   testutils.ParseTime("2021-12-10"),
				end:     testutils.ParseTime("2022-02-10"),
				useYear: true,
			},
		},
	}
	for _, tt := range tests {
		builder := NewBuilder()
		args := NewDatesPairArgs(userNow, tt.args.start, tt.args.end)
		args = args.SetUseYear(tt.args.useYear)

		t.Run(tt.name, func(t *testing.T) {
			if got := builder.BuildDatesPair(args); got != tt.want {
				t.Errorf("BuildDatesPair() = %s, expected %s", got, tt.want)
			}
		})
	}
}

func TestBuilder_Build_ShortMonth(t *testing.T) {
	userNow := testutils.ParseTime("2021-11-05T23:00:00")

	type args struct {
		start   time.Time
		end     time.Time
		useYear bool
	}

	tests := []struct {
		name string
		want string
		args args
	}{
		{
			"one day",
			"10 дек",
			args{
				start: testutils.ParseTime("2021-12-10"),
				end:   testutils.ParseTime("2021-12-10"),
			},
		},
		{
			"one day use years",
			"10 дек, 2021",
			args{
				start:   testutils.ParseTime("2021-12-10"),
				end:     testutils.ParseTime("2021-12-10"),
				useYear: true,
			},
		},
		{
			"many days",
			"10 — 14 дек",
			args{
				start: testutils.ParseTime("2021-12-10"),
				end:   testutils.ParseTime("2021-12-14"),
			},
		},
		{
			"many days use years",
			"10 — 14 дек, 2021",
			args{
				start:   testutils.ParseTime("2021-12-10"),
				end:     testutils.ParseTime("2021-12-14"),
				useYear: true,
			},
		},
		{
			"many month",
			"10 ноя — 10 дек",
			args{
				start: testutils.ParseTime("2021-11-10"),
				end:   testutils.ParseTime("2021-12-10"),
			},
		},
		{
			"many month use years",
			"10 ноя — 10 дек, 2021",
			args{
				start:   testutils.ParseTime("2021-11-10"),
				end:     testutils.ParseTime("2021-12-10"),
				useYear: true,
			},
		},
		{
			"many years",
			"10 дек — 10 фев",
			args{
				start: testutils.ParseTime("2021-12-10"),
				end:   testutils.ParseTime("2022-02-10"),
			},
		},
		{
			"many years",
			"10 дек, 2021 — 10 фев, 2022",
			args{
				start:   testutils.ParseTime("2021-12-10"),
				end:     testutils.ParseTime("2022-02-10"),
				useYear: true,
			},
		},
	}
	for _, tt := range tests {
		builder := NewBuilder()
		args := NewDatesPairArgs(userNow, tt.args.start, tt.args.end)
		args = args.SetUseYear(tt.args.useYear).SetShortMonth()

		t.Run(tt.name, func(t *testing.T) {
			if got := builder.BuildDatesPair(args); got != tt.want {
				t.Errorf("BuildDatesPair() = %s, expected %s", got, tt.want)
			}
		})
	}
}

func TestBuilder_Build_Relative(t *testing.T) {
	tests := []struct {
		name    string
		userNow time.Time
		want    string
	}{
		{
			"relative day after tomorrow",
			testutils.ParseTime("2021-12-08T01:00:00+05:00"),
			"Послезавтра",
		},
		{
			"relative tomorrow",
			testutils.ParseTime("2021-12-09T01:00:00+05:00"),
			"Завтра",
		},
		{
			"relative today",
			testutils.ParseTime("2021-12-10T01:00:00+05:00"),
			"Сегодня",
		},
		{
			"relative yesterday (corner case)",
			testutils.ParseTime("2021-12-11T01:00:00+05:00"),
			"10 — 16 декабря, 2021",
		},
	}
	for _, tt := range tests {
		args := NewDatesPairArgs(
			tt.userNow,
			testutils.ParseTime("2021-12-10T02:00:00+05:00"),
			testutils.ParseTime("2021-12-16"),
		).SetUseRelative().SetUseYear()
		builder := NewBuilder()

		t.Run(tt.name, func(t *testing.T) {
			if got := builder.BuildDatesPair(args); got != tt.want {
				t.Errorf("BuildDatesPair() = %s, expected %s", got, tt.want)
			}
		})
	}
}
