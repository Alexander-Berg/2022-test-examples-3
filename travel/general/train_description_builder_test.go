package trips

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/komod/trips/internal/orders"
)

func TestTrainDescriptionBuilder_Build(t *testing.T) {
	tests := []struct {
		name  string
		train orders.Train
		want  string
	}{
		{
			name: "description for whole train",
			train: orders.Train{
				TrainInfo: orders.TrainInfo{
					Number:               "123",
					StartSettlementTitle: "Питер",
					EndSettlementTitle:   "Москва",
				},
			},
			want: "Поезд 123 Питер — Москва",
		},
		{
			name: "description for whole branded train",
			train: orders.Train{
				TrainInfo: orders.TrainInfo{
					Number:               "123",
					BrandTitle:           "Ласточка",
					StartSettlementTitle: "Питер",
					EndSettlementTitle:   "Москва",
				},
			},
			want: "Поезд 123 Питер — Москва, Ласточка",
		},
		{
			name: "either no start or no end settlement title",
			train: orders.Train{
				TrainInfo: orders.TrainInfo{
					Number:               "123",
					BrandTitle:           "Ласточка",
					StartSettlementTitle: "",
					EndSettlementTitle:   "Москва",
				},
			},
			want: "",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			b := TrainDescriptionBuilder{}
			assert.Equalf(t, tt.want, b.Build(&tt.train), "Build(%v)", tt.train)
		})
	}
}
