package trips

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"google.golang.org/protobuf/proto"

	"a.yandex-team.ru/travel/komod/trips/internal/references"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func mustMarshal(m proto.Message) []byte {
	data, err := proto.Marshal(m)
	if err != nil {
		panic(err)
	}
	return data
}

func TestBuildRegistrationURL(t *testing.T) {
	t.Run("first test", func(t *testing.T) {
		expectedURL := "registration on Aeroflot url"
		repo := references.NewCarrierRepository()
		data := mustMarshal(&rasp.TCarrier{
			Id:              26,
			Title:           "Аэрофлот",
			RegistrationUrl: expectedURL,
		})
		_, err := repo.Write(data)
		if err != nil {
			panic(err)
		}
		airline := &rasp.TCarrier{
			Id:              9144,
			RegistrationUrl: "https://www.pobeda.aero/ru/check-in/",
		}
		url := buildRegistrationURL(repo, airline)

		assert.Equalf(t, expectedURL, url, "Airline with Id = %v", airline.Id)

		airline = &rasp.TCarrier{
			Id:              1865,
			RegistrationUrl: "https://www.swiss.com/de/RU/profile/login",
		}
		url = buildRegistrationURL(repo, airline)

		assert.Equalf(
			t,
			"https://www.swiss.com/de/RU/profile/login",
			url,
			"Airline with Id = %v", airline.Id,
		)
	})
}
