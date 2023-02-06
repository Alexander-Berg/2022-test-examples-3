package direction

import (
	"github.com/golang/protobuf/proto"
	"testing"

	"a.yandex-team.ru/travel/library/go/dicts/repository"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/rasp/wizards/go/wizard_proxy_api/internal/geomodel"
	"a.yandex-team.ru/travel/rasp/wizards/go/wizard_proxy_api/internal/storage"
)

func NewMockedStorage() (*storage.Storage, error) {
	storageMock := &storage.Storage{}
	storageMock.SettlementRepo = repository.NewSettlementRepository()
	cityMock := &rasp.TSettlement{
		Id:           213,
		TitleDefault: "Москва",
	}
	protoCityMock, err := proto.Marshal(cityMock)
	if err != nil {
		return nil, err
	}
	_, err = storageMock.SettlementRepo.Write(protoCityMock)
	return storageMock, err
}

func TestReprPoint(t *testing.T) {
	s, err := NewMockedStorage()
	if err != nil {
		t.Errorf("Error cannot create storage mock %v", err)
	}
	point, err := s.GetSettlementByGeoID(213, true)
	if err != nil {
		t.Error("Error cannot find Moskow")
	}
	repr := point.ReprPoint()
	expectedRepr := geomodel.PointRepr{Key: 213, Title: "Москва"}
	if repr != expectedRepr {
		t.Errorf("Error repr %+v is different from expected %+v", repr, expectedRepr)
	}
}
