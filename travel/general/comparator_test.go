package point

import (
	"errors"
	"github.com/stretchr/testify/require"
	"path/filepath"
	"testing"

	"a.yandex-team.ru/library/go/test/yatest"
	geobaselib "a.yandex-team.ru/library/go/yandex/geobase"
	"a.yandex-team.ru/travel/komod/trips/internal/consts"
	"a.yandex-team.ru/travel/komod/trips/internal/helpers"
	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/references"
	"a.yandex-team.ru/travel/library/go/geobase"
	"a.yandex-team.ru/travel/library/go/testutil"
)

const (
	yekaterinburgID   = 54
	kamenskUralskiiID = 11164
	moscowID          = 213
	sheremetyevoID    = 9600213
	koltsovoID        = 9600370
	istanbulID        = 11508
	lutonID           = 9625784
)

func TestPointComparator_InSameSubject(t *testing.T) {
	geoBase, ok := createGeoBase(t)
	if !ok {
		return
	}

	reference, err := createTestReferences()
	if err != nil {
		t.Fatalf("failed to create references: %+v. geobasePath: %s", err, buildPath("geodata6.bin"))
	}
	comp := createTestPointComparator(geoBase, reference)

	require.True(
		t,
		comp.InSameSubject(
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
	require.True(
		t,
		comp.InSameSubject(
			models.NewGeoRegionPoint(kamenskUralskiiID, consts.EkbLocation, nil),
			models.NewGeoRegionPoint(yekaterinburgID, consts.EkbLocation, nil),
		),
	)
	require.True(
		t,
		comp.InSameSubject(
			models.NewSettlementPoint(yekaterinburgID, consts.EkbLocation, nil),
			models.NewGeoRegionPoint(kamenskUralskiiID, consts.EkbLocation, nil),
		),
	)
	require.True(
		t,
		comp.InSameSubject(
			models.NewStationPoint(sheremetyevoID, 0, consts.MskLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
	require.False(
		t,
		comp.InSameSubject(
			models.NewSettlementPoint(moscowID, consts.MskLocation, nil),
			models.NewGeoRegionPoint(kamenskUralskiiID, consts.EkbLocation, nil),
		),
	)
	require.False(
		t,
		comp.InSameSubject(
			models.NewStationPoint(koltsovoID, 0, consts.EkbLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
}

func TestPointComparator_InSameCountry(t *testing.T) {
	geoBase, ok := createGeoBase(t)
	if !ok {
		return
	}

	reference, err := createTestReferences()
	if err != nil {
		return
	}
	comp := createTestPointComparator(geoBase, reference)

	require.True(
		t,
		comp.InSameCountry(
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
	require.True(
		t,
		comp.InSameCountry(
			models.NewGeoRegionPoint(kamenskUralskiiID, consts.EkbLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
	require.True(
		t,
		comp.InSameCountry(
			models.NewSettlementPoint(yekaterinburgID, consts.EkbLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
	require.True(
		t,
		comp.InSameCountry(
			models.NewStationPoint(sheremetyevoID, 0, consts.MskLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
	require.False(
		t,
		comp.InSameCountry(
			models.NewSettlementPoint(istanbulID, consts.IstLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
	require.False(
		t,
		comp.InSameCountry(
			models.NewStationPoint(lutonID, 0, consts.MskLocation, nil),
			models.NewGeoRegionPoint(moscowID, consts.MskLocation, nil),
		),
	)
}

func createTestPointComparator(geoBase geobase.Geobase, reference references.References) *Comparator {
	factory := NewFactory(
		nil,
		helpers.NewCachedLocationRepository(),
		geoBase,
		reference,
	)
	resolver := NewResolver(geoBase, reference, factory)
	return NewComparator(geoBase, reference, resolver)
}

func buildPath(path string) string {
	return filepath.Join(yatest.BuildPath(filepath.Join(yatest.ProjectPath(), "data")), path)
}

func createGeoBase(t *testing.T) (geobase.Geobase, bool) {
	config := geobase.Config{
		Path: buildPath("geodata6.bin"),
	}
	geoBase, err := geobase.NewGeobase(config, testutil.NewLogger(t))
	if err != nil {
		if errors.Is(err, geobaselib.ErrNotSupported) {
			t.Fatalf("failed test: %s, %+v", config.Path, err)
			return nil, false
		}
		t.Fatalf("failed to create geobase: %+v", err)
	}
	return geoBase, true
}

func createTestReferences() (references.References, error) {
	return references.NewRegistry(references.Config{
		ResourcesPath:       buildPath("."),
		UseDynamicResources: false,
	})
}
