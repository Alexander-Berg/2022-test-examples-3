package status

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects"
	"a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects/model"
)

type mockStation struct{}

var SVX = model.Station{
	ID:   1,
	Iata: "SVX",
}

var StopPointSVX = model.StopPoint{
	StationCode: "Ekaterinburg",
	StationID:   int32(SVX.ID),
}

var StopPointLED = model.StopPoint{
	StationCode: "SaintPetersburg",
	StationID:   -1,
}

func (s *mockStation) ByCode(code string) *model.Station {
	if code == SVX.Iata {
		return &SVX
	}
	return nil
}

func (s *mockStation) ByID(id model.StationID) *model.Station {
	if id == SVX.ID {
		return &SVX
	}
	return nil
}

type mockStopPoint struct{}

func (sp *mockStopPoint) ByCode(code string) *model.StopPoint {
	if code == StopPointSVX.StationCode {
		return &StopPointSVX
	}
	return nil
}

func Test_SQL_GetRoutePoint(t *testing.T) {

	objects := objects.Objects{
		Station:   &mockStation{},
		StopPoint: &mockStopPoint{},
	}

	assert.Equal(t, "SVX", GetRoutePoint("Ekaterinburg", &objects))
	assert.Equal(t, "Sverdlovsk", GetRoutePoint("Sverdlovsk", &objects))
	assert.Equal(t, "SVX", GetRoutePoint("svx", &objects))
}
