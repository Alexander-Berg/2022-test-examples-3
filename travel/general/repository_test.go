package base

import (
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/library/go/dicts/extensions"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func createSettlement() proto.Message {
	return &rasp.TSettlement{}
}

type settlementRepo struct {
	*Repository
	*extensions.SettlementByID
	*extensions.SettlementByGeoID
}

func TestAddExtensions(t *testing.T) {
	repo := &settlementRepo{
		Repository:        NewRepository("Settlement", createSettlement),
		SettlementByID:    extensions.NewSettlementByID(),
		SettlementByGeoID: extensions.NewSettlementByGeoID(),
	}

	repo.AddExtensions(repo.SettlementByID)
	repo.AddExtensions(repo.SettlementByGeoID)

	assert.Equal(t, 2, len(repo.extensions))
	assert.Equal(t, repo.SettlementByID, repo.extensions[0])
	assert.Equal(t, repo.SettlementByGeoID, repo.extensions[1])

	check(t, repo, repo.SettlementByID, repo.SettlementByGeoID)
}

func TestAddUsingPredicate(t *testing.T) {
	predicate := func(message proto.Message) bool {
		if settlement, ok := message.(*rasp.TSettlement); ok {
			return !settlement.IsHidden
		}
		return true
	}

	repo := NewRepository("Settlement", createSettlement).WithPredicate(predicate)
	extension := extensions.NewSettlementByID()
	repo.AddExtensions(extension)

	repo.Add(&rasp.TSettlement{Id: 1, IsHidden: true})
	repo.Add(&rasp.TSettlement{Id: 2, IsHidden: false})
	repo.Add(&rasp.TSettlement{Id: 3})

	settlement, found := extension.Get(1)
	assert.False(t, found)
	assert.Nil(t, settlement)

	settlement, found = extension.Get(2)
	assert.True(t, found)
	require.NotNil(t, settlement)
	assert.Equal(t, 2, int(settlement.Id))

	settlement, found = extension.Get(3)
	assert.True(t, found)
	require.NotNil(t, settlement)
	assert.Equal(t, 3, int(settlement.Id))
}

type extendable interface {
	Add(message proto.Message)
}

func check(
	t *testing.T,
	repository extendable,
	mainExtension *extensions.SettlementByID,
	geoExtension *extensions.SettlementByGeoID,
) {
	mainSettlement := &rasp.TSettlement{Id: 1}
	geoSettlement := &rasp.TSettlement{Id: 2, GeoId: 3}

	repository.Add(mainSettlement)
	repository.Add(geoSettlement)

	s, ok := mainExtension.Get(1)
	assert.True(t, ok)
	assertpb.Equal(t, mainSettlement, s)

	s, ok = mainExtension.Get(2)
	assert.True(t, ok)
	assertpb.Equal(t, geoSettlement, s)

	s, ok = geoExtension.GetByGeoID(3)
	assert.True(t, ok)
	assertpb.Equal(t, geoSettlement, s)
}
