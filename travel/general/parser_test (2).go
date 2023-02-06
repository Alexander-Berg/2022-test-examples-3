package points

import (
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	dicts "a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

type regionCapitalRepo struct {
	message *dicts.TSettlement
}

func (r *regionCapitalRepo) GetRegionCapital(int) *dicts.TSettlement {
	return r.message
}

func newTestParser(t *testing.T) *Parser {
	logger := testutils.NewLogger(t)
	repoRegistry := registry.NewRepositoryRegistry(logger)
	regionCapitalRepo := &regionCapitalRepo{}
	return NewParser(repoRegistry, regionCapitalRepo)
}

func TestParser_ParseByPointKey_FormatError(t *testing.T) {
	parser := newTestParser(t)

	for _, key := range []string{
		"123",
		"word",
		"g_123",
		"c_word",
		"c123word",
	} {
		_, err := parser.ParseByPointKey(key)
		assert.ErrorIsf(t, err, UnexpectedFormatErr, "ParseByPointKey should fail with key=%s", key)
	}
}

func TestParser_ParseSettlementByGeoID_FormatError(t *testing.T) {
	parser := newTestParser(t)

	for _, geoID := range []string{
		"123word",
		"word",
		"g_123",
		"c_word",
		"c123word",
	} {
		_, err := parser.ParseSettlementByGeoID(geoID)
		assert.ErrorIsf(t, err, UnexpectedFormatErr, "ParseSettlementByGeoID should fail with geoID=%s", geoID)
	}
}

func TestNotFound(t *testing.T) {
	parser := newTestParser(t)
	point, err := parser.ParseByPointKey("g123")
	assert.Nil(t, point)
	assert.ErrorIs(t, err, NotExistsErr)

	point, err = parser.ParseSettlementByGeoID("123")
	assert.Nil(t, point)
	assert.ErrorIs(t, err, NotExistsErr)
}

func TestParseByPointKey(t *testing.T) {
	parser := newTestParser(t)

	cases := []struct {
		repo     interface{ Add(message proto.Message) }
		message  proto.Message
		pointKey string
	}{
		{
			repo:     parser.repoRegistry.GetSettlementRepo(),
			message:  &dicts.TSettlement{GeoId: 123, Iata: "fake"},
			pointKey: "g123",
		},
		{
			repo:     parser.repoRegistry.GetSettlementRepo(),
			message:  &dicts.TSettlement{Id: 125, Iata: "fake"},
			pointKey: "c125",
		},
		{
			repo:     parser.repoRegistry.GetStationRepo(),
			message:  &dicts.TStation{Id: 126},
			pointKey: "s126",
		},
	}

	for _, c := range cases {
		t.Run(c.pointKey, func(t *testing.T) {
			c.repo.Add(c.message)
			point, err := parser.ParseByPointKey(c.pointKey)
			require.NoError(t, err)
			requirepb.Equal(t, point.Proto(), c.message)
		})
	}
}

func TestParseSettlementByGeoId(t *testing.T) {
	parser := newTestParser(t)

	cases := []struct {
		name    string
		repo    interface{ Add(message proto.Message) }
		message proto.Message
		geoID   string
	}{
		{
			name:    "simple lookup",
			repo:    parser.repoRegistry.GetSettlementRepo(),
			message: &dicts.TSettlement{GeoId: 123, Iata: "fake"},
			geoID:   "123",
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			c.repo.Add(c.message)
			point, err := parser.ParseSettlementByGeoID(c.geoID)
			require.NoError(t, err)
			requirepb.Equal(t, point.Proto(), c.message)
		})
	}
}

func TestRegionCapital(t *testing.T) {
	parser := newTestParser(t)
	settlement := &dicts.TSettlement{}

	parser.regionCapitalRepo.(*regionCapitalRepo).message = settlement

	point, err := parser.ParseSettlementByGeoID("123")
	require.NoError(t, err)
	requirepb.Equal(t, point.Proto(), settlement)
}
