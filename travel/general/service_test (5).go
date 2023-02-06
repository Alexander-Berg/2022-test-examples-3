package carrier

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/carrier"
)

type fakeData struct {
}

func (f fakeData) CarrierCodes() map[int32]carrier.CarrierCodes {
	return map[int32]carrier.CarrierCodes{
		5: {Iata: "DP"},
		6: {Icao: "DP"},
	}
}

func (f fakeData) FindCarrier(iataCode, sirenaCode, flightNumber, _ string, _ int32) int32 {
	if iataCode == "SU" {
		return 1
	}
	if sirenaCode == "СУ" {
		return 2
	}
	if iataCode == "TK" && flightNumber > "600" {
		return 3
	}
	return 0
}

func (f fakeData) GetCarriersByCode(code string) []int32 {
	if code == "DP" {
		return []int32{5, 6}
	}
	return nil
}

func r(v Flight) *Flight {
	return &v
}

func TestCarrierService_GetCarriersByFlightNumbers(t *testing.T) {
	type fields struct {
		carriers      carriers
		iatacorrector iatacorrector
	}
	type args struct {
		flightNumbers []string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   map[string]*Flight
	}{
		{
			name: "Batch test",
			fields: fields{
				carriers:      fakeData{},
				iatacorrector: fakeData{},
			},
			args: args{
				flightNumbers: []string{
					" Su6002", " сУ100 ", "SU  100 ",
					"  DP404", "DP 100 ",
					"FV 1632", "TK 123", "TK 723",
					"TK6654", "TK7654", "TK 7123", "A", ""},
			},
			want: map[string]*Flight{
				" Su6002":  r(Flight{1, "SU", "6002"}),
				" сУ100 ":  r(Flight{2, "СУ", "100"}),
				"SU  100 ": r(Flight{1, "SU", "100"}),
				"  DP404":  r(Flight{5, "DP", "404"}),
				"DP 100 ":  r(Flight{5, "DP", "100"}),
				"FV 1632":  nil, "TK 123": nil,
				"TK 723":  r(Flight{3, "TK", "723"}),
				"TK6654":  r(Flight{3, "TK", "6654"}),
				"TK7654":  r(Flight{3, "TK", "7654"}),
				"TK 7123": r(Flight{3, "TK", "7123"}),
				"A":       nil, "": nil,
			},
		},
		{
			name: "Empty test",
			fields: fields{
				carriers:      fakeData{},
				iatacorrector: fakeData{},
			},
			args: args{
				flightNumbers: []string{},
			},
			want: map[string]*Flight{},
		},
		{
			name: "Nil test",
			fields: fields{
				carriers:      fakeData{},
				iatacorrector: fakeData{},
			},
			args: args{
				flightNumbers: nil,
			},
			want: map[string]*Flight{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &CarrierService{
				carriers:      tt.fields.carriers,
				iatacorrector: tt.fields.iatacorrector,
			}
			got := c.GetCarriersByFlightNumbers(tt.args.flightNumbers)
			assert.Equal(t, tt.want, got)
		})
	}
}
