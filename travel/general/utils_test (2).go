package span

import (
	"reflect"
	"testing"
	"time"

	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
)

func TestGetEndTime(t *testing.T) {
	tests := []struct {
		name  string
		spans []models.Span
		want  time.Time
	}{
		{
			name: "last time is result",
			spans: []models.Span{
				models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-12")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-13")),
					true,
				),
				models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-14")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15T04")),
					true,
				),
			},
			want: testutils.ParseTime("2021-12-15T04"),
		},
		{
			name: "unordered input",
			spans: []models.Span{
				models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-14")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15T04")),
					true,
				),
				models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-12")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-13")),
					true,
				),
			},
			want: testutils.ParseTime("2021-12-15T04"),
		},
		{
			name: "crossing spans",
			spans: []models.Span{
				models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-12")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-14")),
					true,
				),
				models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-13")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15T04")),
					true,
				),
			},
			want: testutils.ParseTime("2021-12-15T04"),
		},
		{
			name: "get one of maximum results",
			spans: []models.Span{
				models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-12")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-15T04")),
					true,
				),
				models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-13")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15T04")),
					true,
				),
			},
			want: testutils.ParseTime("2021-12-15T04"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetEndTime(tt.spans); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetEndTime() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetStartTime(t *testing.T) {
	tests := []struct {
		name  string
		spans []models.Span
		want  time.Time
	}{
		{
			name: "first time is result",
			spans: []models.Span{
				models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-12T03")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-13")),
					true,
				),
				models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-14")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15")),
					true,
				),
			},
			want: testutils.ParseTime("2021-12-12T03"),
		},
		{
			name: "unordered input",
			spans: []models.Span{
				models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-14")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15T04")),
					true,
				),
				models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-12T03")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-13")),
					true,
				),
			},
			want: testutils.ParseTime("2021-12-12T03"),
		},
		{
			name: "crossing spans",
			spans: []models.Span{
				models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-12T03")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-14")),
					true,
				),
				models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-13")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15T04")),
					true,
				),
			},
			want: testutils.ParseTime("2021-12-12T03"),
		},
		{
			name: "get one of minimum results",
			spans: []models.Span{
				models.NewSpan(
					models.NewFakeVisit(1, testutils.ParseTime("2021-12-12T03")),
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-14")),
					true,
				),
				models.NewSpan(
					models.NewFakeVisit(2, testutils.ParseTime("2021-12-12T03")),
					models.NewFakeVisit(3, testutils.ParseTime("2021-12-15T04")),
					true,
				),
			},
			want: testutils.ParseTime("2021-12-12T03"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetStartTime(tt.spans); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetStartTime() = %v, want %v", got, tt.want)
			}
		})
	}
}
