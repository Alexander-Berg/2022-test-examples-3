package matcher

import (
	"testing"

	"a.yandex-team.ru/travel/komod/trips/internal/helpers"
	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/point"
	"a.yandex-team.ru/travel/komod/trips/internal/references"
	"a.yandex-team.ru/travel/komod/trips/internal/span"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
	"a.yandex-team.ru/travel/library/go/geobase"
)

func TestIsCorrectSpansInterposition(t *testing.T) {
	type args struct {
		lhs models.Span
		rhs models.Span
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "(1-2)(2-3) -> true",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-16T14")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-17")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-20")),
					false,
				),
			},
			want: true,
		},
		{
			name: "(1-[2-2]-2) -> true",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-20")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-11")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-15")),
					false,
				),
			},
			want: true,
		},
		{
			name: "([1|2-2]-2) -> true",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-20")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-15")),
					false,
				),
			},
			want: true,
		},
		{
			name: "(1-[2-2|2]) -> true",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-20")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-15")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-20")),
					false,
				),
			},
			want: true,
		},
		{
			name: "(1-[3-3]-2) -> false",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-20")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-18")),
					false,
				),
			},
			want: false,
		},
		{
			name: "(1-[3-3|2]) -> false",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-20")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-20")),
					false,
				),
			},
			want: false,
		},
		{
			name: "(1-2)---(2-3) -> false",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-16T14")),
					false,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-20")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-21")),
					true,
				),
			},
			want: false,
		},
		{
			name: "(1-2)(3-2) -> false",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-16T14")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-17")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-21")),
					false,
				),
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fName := "IsCorrectSpansInterposition"
			f := createTestRuleFactory().IsCorrectSpansInterposition
			if got := f(tt.args.lhs, tt.args.rhs); got != tt.want {
				t.Errorf("%s() = %v, want %v", fName, got, tt.want)
			}
			if got := f(tt.args.rhs, tt.args.lhs); got != tt.want {
				t.Errorf("unordered spans: %s() = %v, want %v", fName, got, tt.want)
			}
		})
	}
}

func TestIsLongTransfer(t *testing.T) {
	type args struct {
		lhs models.Span
		rhs models.Span
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "(1-2)---(2-3) -> true",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-16T14")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-20")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-22")),
					true,
				),
			},
			want: true,
		},
		{
			name: "(1-2)(2-3) -> false",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-16T14")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-17")),
					models.NewFakeVisit(3, testutils.ParseTime("2022-01-20")),
					true,
				),
			},
			want: false,
		},
		{
			name: "(1-2)---(3-2) -> false",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-16T14")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(3, testutils.ParseTime("2022-12-20")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-22")),
					true,
				),
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fName := "IsLongTransfer"
			f := createTestRuleFactory().IsLongTransfer
			if got := f(tt.args.lhs, tt.args.rhs); got != tt.want {
				t.Errorf("%s() = %v, want %v", fName, got, tt.want)
			}
			if got := f(tt.args.rhs, tt.args.lhs); got != tt.want {
				t.Errorf("unordered spans: %s() = %v, want %v", fName, got, tt.want)
			}
		})
	}
}

func TestIsRoundTrip(t *testing.T) {
	type args struct {
		lhs models.Span
		rhs models.Span
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "(1-2)---(3-1) -> true",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-16T14")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-20")),
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-22")),
					true,
				),
			},
			want: true,
		},
		{
			name: "(1-2)---(3-4) -> false",
			args: args{
				lhs: models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-10")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-16T14")),
					true,
				),
				rhs: models.NewSpan(
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-20")),
					models.NewFakeVisit(4, testutils.ParseTime("2021-12-30")),
					true,
				),
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fName := "IsRoundTrip"
			f := createTestRuleFactory().IsRoundTrip
			if got := f(tt.args.lhs, tt.args.rhs); got != tt.want {
				t.Errorf("%s() = %v, want %v", fName, got, tt.want)
			}
			if got := f(tt.args.rhs, tt.args.lhs); got != tt.want {
				t.Errorf("unordered spans: %s() = %v, want %v", fName, got, tt.want)
			}
		})
	}
}

func createTestRuleFactory() *RuleFactory {
	geoBase := geobase.StubGeobase{}
	reference := references.References(nil)
	pointFactory := point.NewFactory(
		nil,
		helpers.NewCachedLocationRepository(),
		geoBase,
		reference,
	)
	pointResolver := point.NewResolver(geoBase, reference, pointFactory)
	pointComparator := point.NewComparator(geoBase, reference, pointResolver)
	spanComparator := span.NewSpanComparator(pointComparator)
	spanHelper := span.NewHelper(pointComparator, spanComparator)
	return NewRuleFactory(pointComparator, spanHelper)
}
