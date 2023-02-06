package models

import (
	"reflect"
	"strconv"
	"testing"
	"time"

	"a.yandex-team.ru/travel/hotels/proto/data_config/promo"
)

func TestPromoEventFromProto(t *testing.T) {
	b := func(b bool) *bool {
		return &b
	}
	s := func(s string) *string {
		return &s
	}

	tests := []struct {
		name  string
		event *promo.TPromoEvent
		want  PromoEvent
	}{
		{
			name: "simple enabled",
			event: &promo.TPromoEvent{
				EventId:          s("event123"),
				OrdersCampaignId: s("campaign123"),
				Enabled:          b(true),
			},
			want: PromoEvent{
				EventID:          "event123",
				OrdersCampaignID: "campaign123",
				Enabled:          true,
			},
		},
		{
			name: "with start time",
			event: &promo.TPromoEvent{
				EventId:          s("event123"),
				OrdersCampaignId: s("campaign123"),
				Enabled:          b(true),
				StartAt:          s("2021-02-01T01:02:03Z"),
			},
			want: PromoEvent{
				EventID:          "event123",
				OrdersCampaignID: "campaign123",
				Enabled:          true,
				StartAt:          time.Date(2021, 2, 1, 1, 2, 3, 0, time.UTC),
			},
		},
		{
			name: "with end time",
			event: &promo.TPromoEvent{
				EventId:          s("event123"),
				OrdersCampaignId: s("campaign123"),
				Enabled:          b(true),
				EndAt:            s("2021-02-01T01:02:03Z"),
			},
			want: PromoEvent{
				EventID:          "event123",
				OrdersCampaignID: "campaign123",
				Enabled:          true,
				EndAt:            time.Date(2021, 2, 1, 1, 2, 3, 0, time.UTC),
			},
		},
		{
			name: "with sources",
			event: &promo.TPromoEvent{
				EventId:          s("event123"),
				OrdersCampaignId: s("campaign123"),
				Enabled:          b(true),
				Sources:          []string{"source_1", "source_2"},
			},
			want: PromoEvent{
				EventID:          "event123",
				OrdersCampaignID: "campaign123",
				Enabled:          true,
				Sources:          []string{"source_1", "source_2"},
			},
		},
	}
	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			if got := PromoEventFromProto(tt.event); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("PromoEventFromProto() = %+v, want %+v", got, tt.want)
			}
		})
	}
}

func TestPromoEvent_Active(t *testing.T) {
	type fields struct {
		EventID          string
		OrdersCampaignID string
		Enabled          bool
		StartAt          time.Time
		EndAt            time.Time
		Verticals        []string
	}
	type args struct {
		now      time.Time
		vertical string
	}
	tests := []struct {
		fields fields
		args   args
		want   bool
	}{
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Time{},
				EndAt:            time.Time{},
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: true,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Time{},
				EndAt:            time.Time{},
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "shmavia",
			},
			want: false,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          false,
				StartAt:          time.Time{},
				EndAt:            time.Time{},
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: false,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
				EndAt:            time.Date(2021, 1, 1, 0, 0, 0, 0, time.UTC),
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: true,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Time{},
				EndAt:            time.Date(2021, 1, 1, 0, 0, 0, 0, time.UTC),
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: true,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
				EndAt:            time.Time{},
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: true,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Date(2018, 1, 1, 0, 0, 0, 0, time.UTC),
				EndAt:            time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: false,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Date(2021, 1, 1, 0, 0, 0, 0, time.UTC),
				EndAt:            time.Date(2022, 1, 1, 0, 0, 0, 0, time.UTC),
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: false,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Time{},
				EndAt:            time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: false,
		},
		{
			fields: fields{
				EventID:          "",
				OrdersCampaignID: "",
				Enabled:          true,
				StartAt:          time.Date(2021, 1, 1, 0, 0, 0, 0, time.UTC),
				EndAt:            time.Time{},
				Verticals:        []string{"avia", "hotels"},
			},
			args: args{
				now:      time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
				vertical: "avia",
			},
			want: false,
		},
	}
	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			e := &PromoEvent{
				EventID:          tt.fields.EventID,
				OrdersCampaignID: tt.fields.OrdersCampaignID,
				Enabled:          tt.fields.Enabled,
				StartAt:          tt.fields.StartAt,
				EndAt:            tt.fields.EndAt,
				Verticals:        tt.fields.Verticals,
			}
			if got := e.Active(tt.args.now, tt.args.vertical); got != tt.want {
				t.Errorf("Active() = %v, want %v", got, tt.want)
			}
		})
	}
}
