package references

import (
	"sync/atomic"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	aviaLogging "a.yandex-team.ru/travel/avia/library/go/logging"
	backendPartners "a.yandex-team.ru/travel/avia/library/go/services/backend/partners"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/caches/references"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/models"
)

type backendClientMock struct {
	partners atomic.Value
}

func (client *backendClientMock) GetPartners() ([]*backendPartners.WizardPartner, error) {
	var wizardPartners []*backendPartners.WizardPartner
	for _, p := range client.partners.Load().([]*models.Partner) {
		wizardPartner := backendPartners.WizardPartner(*p)
		wizardPartners = append(wizardPartners, &wizardPartner)
	}
	return wizardPartners, nil
}

var (
	logger, _ = aviaLogging.NewLogger(&aviaLogging.Config{
		Level: log.InfoLevel,
	})
)

func TestPrecache_FinishedSuccessfully_CacheShouldBeReady(t *testing.T) {
	backendClientMock := &backendClientMock{partners: atomic.Value{}}
	backendClientMock.partners.Store([]*models.Partner{})
	partnersCache := references.NewPartners(backendClientMock, time.Minute, logger)

	assert.NoError(t, partnersCache.Precache())
}

func TestGetPartners_CacheIsReady_ShouldReturnsPartners(t *testing.T) {
	expectedPartners := []*models.Partner{{
		ID:              1,
		Code:            "code",
		Enabled:         true,
		EnabledInWizard: map[string]bool{"nv": true},
		IsAviacompany:   false,
		SiteURL:         "http://url.com",
		LogosSvg:        map[string]string{"nv": "logo"},
		Titles:          map[string]string{"nv": "title"},
	}}
	backendClientMock := &backendClientMock{partners: atomic.Value{}}
	backendClientMock.partners.Store(expectedPartners)
	partnersCache := references.NewPartners(backendClientMock, time.Minute, logger)
	_ = partnersCache.Precache()

	partners := partnersCache.GetAll()

	assert.Equal(t, expectedPartners, partners)
}

func TestPrecache_ShouldRunsRepeatedPrecache(t *testing.T) {
	expectedPartners := []*models.Partner{}
	backendClientMock := &backendClientMock{partners: atomic.Value{}}
	backendClientMock.partners.Store(expectedPartners)
	partnersCache := references.NewPartners(backendClientMock, 500*time.Millisecond, logger)
	_ = partnersCache.Precache()

	partners := partnersCache.GetAll()

	assert.Equal(t, expectedPartners, partners)

	expectedPartners = []*models.Partner{{
		ID:              1,
		Code:            "code",
		Enabled:         true,
		EnabledInWizard: map[string]bool{"nv": true},
		IsAviacompany:   false,
		SiteURL:         "http://url.com",
		LogosSvg:        map[string]string{"nv": "logo"},
		Titles:          map[string]string{"nv": "title"},
	}}
	backendClientMock.partners.Store(expectedPartners)

	time.Sleep(time.Second)

	partners = partnersCache.GetAll()

	assert.Equal(t, expectedPartners, partners)
}
