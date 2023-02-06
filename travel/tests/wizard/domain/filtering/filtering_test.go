package filtering

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/ptr"
	aviaLogging "a.yandex-team.ru/travel/avia/library/go/logging"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/filtering"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/models"
	"a.yandex-team.ru/travel/avia/wizard/tests/wizard/mocks"
	wizardProto "a.yandex-team.ru/travel/proto/avia/wizard"
)

var (
	partnerRepository = new(mocks.PartnerRepositoryMock)
	companyRepository = new(mocks.CompanyRepositoryMock)
	logger, _         = aviaLogging.NewLogger(&aviaLogging.Config{})
)

func TestExpired_ShouldFilterExpiredFares(t *testing.T) {
	expiredFare := &wizardProto.Fare{
		ExpireAt: ptr.Int32(int32(time.Date(2020, 02, 05, 0, 0, 0, 0, time.UTC).Unix())),
	}
	notExpiredFare := &wizardProto.Fare{
		ExpireAt: ptr.Int32(int32(time.Date(2020, 02, 07, 0, 0, 0, 0, time.UTC).Unix())),
	}
	filterDate := time.Date(2020, 02, 06, 0, 0, 0, 0, time.UTC)

	filter := filtering.NewFaresProtoFilter(logger, nil, "").Expired(filterDate)

	assert.False(t, filter.IsGoodFare(expiredFare, 0))
	assert.True(t, filter.IsGoodFare(notExpiredFare, 0))
}

func TestExpired_ShouldFilterPromoExpiredFares(t *testing.T) {
	expiredEnd := &wizardProto.Promo{
		Code:  ptr.String("promo"),
		EndTs: ptr.Int32(int32(time.Date(2020, 02, 05, 0, 0, 0, 0, time.UTC).Unix())),
	}

	notExpiredEnd := &wizardProto.Promo{
		Code:  ptr.String("promo"),
		EndTs: ptr.Int32(int32(time.Date(2020, 02, 07, 0, 0, 0, 0, time.UTC).Unix())),
	}

	promoExpiredFare := &wizardProto.Fare{
		Promo: expiredEnd,
	}
	notExpiredFare := &wizardProto.Fare{
		Promo: notExpiredEnd,
	}
	notPromoFare := &wizardProto.Fare{}

	filterDate := time.Date(2020, 02, 06, 0, 0, 0, 0, time.UTC)
	filter := filtering.NewFaresProtoFilter(logger, nil, "").PromoExpired(filterDate)

	assert.False(t, filter.IsGoodFare(promoExpiredFare, 0), "expired")
	assert.True(t, filter.IsGoodFare(notExpiredFare, 0), "not expired")
	assert.True(t, filter.IsGoodFare(notPromoFare, 0), "not_promo")
}

func TestDisabledPartner_ShouldFilterFaresWithDisablePartner(t *testing.T) {
	fareWithDisabledPartner := &wizardProto.Fare{Partner: ptr.String("disabled")}
	fareWithEnabledPartner := &wizardProto.Fare{Partner: ptr.String("enabled")}
	partnerRepository.On("IsEnabled", *fareWithDisabledPartner.Partner, "ru").Return(false)
	partnerRepository.On("IsEnabled", *fareWithEnabledPartner.Partner, "ru").Return(true)

	filter := filtering.NewFaresProtoFilter(logger, nil, "").DisabledPartner(partnerRepository, "ru")

	assert.False(t, filter.IsGoodFare(fareWithDisabledPartner, 0))
	assert.True(t, filter.IsGoodFare(fareWithEnabledPartner, 0))
}

func TestIncorrectCompanyIDs_ShouldFilterFaresWithIncorrectCompanyIDs(t *testing.T) {
	flightsProto := map[string]*wizardProto.Flight{
		"known":    {Company: ptr.Int32(1)},
		"unknown":  {Company: ptr.Int32(2)},
		"negative": {Company: ptr.Int32(-1)},
	}
	fareWithUnknownID := &wizardProto.Fare{
		Route: &wizardProto.RouteSegments{
			Forward: []string{"unknown", "known"},
		},
	}
	fareWithNegativeCompanyID := &wizardProto.Fare{
		Route: &wizardProto.RouteSegments{
			Forward: []string{"negative", "known"},
		},
	}
	fareWithGoodCompanyID := &wizardProto.Fare{
		Route: &wizardProto.RouteSegments{
			Forward: []string{"known"},
		},
	}
	knownCompany := models.Company{ID: 1}
	companyRepository.On("GetByID", 1).Return(&knownCompany, true)
	companyRepository.On("GetByID", 2).Return((*models.Company)(nil), false)

	filter := filtering.NewFaresProtoFilter(logger, flightsProto, "").IncorrectCompanyIDs(companyRepository)

	assert.False(t, filter.IsGoodFare(fareWithUnknownID, 0))
	assert.False(t, filter.IsGoodFare(fareWithNegativeCompanyID, 0))
	assert.True(t, filter.IsGoodFare(fareWithGoodCompanyID, 0))
}
