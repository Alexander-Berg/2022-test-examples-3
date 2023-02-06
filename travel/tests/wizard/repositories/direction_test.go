package repositories

import (
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/caches/references"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/repositories"
	dictsWizard "a.yandex-team.ru/travel/proto/avia/wizard"
)

func TestCache_GetPopularity(t *testing.T) {
	repository := getDirectionRepository(t, getDirection())

	require.Equal(t, repository.GetPopularity(1, 2), 33)
	require.Equal(t, repository.GetPopularity(2, 1), 0)
	require.Equal(t, repository.GetPopularity(10, 2), 0)
}

func TestCache_IsPossibleVariant(t *testing.T) {
	repository := getDirectionRepository(t, getDirection())

	require.True(t, repository.IsPossibleVariant(1, 2, "ru"))
	require.True(t, repository.IsPossibleVariant(2, 1, "ru"))

	require.False(t, repository.IsPossibleVariant(10, 1, "ru"))
	require.False(t, repository.IsPossibleVariant(1, 10, "ru"))
	require.False(t, repository.IsPossibleVariant(1, 2, "no_ru"))
}

func getDirection() *dictsWizard.TDirectionNational {
	return &dictsWizard.TDirectionNational{
		DepartureSettlementID: 1,
		ArrivalSettlementID:   2,
		Popularity:            33,
		NationalVersion:       "ru",
	}
}

func getDirectionRepository(t *testing.T, directions ...proto.Message) repositories.Direction {
	directionReference := references.NewDirection()
	cacheReference(t, directionReference, directions)

	return repositories.NewDirectionRepository(directionReference)
}
