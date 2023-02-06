package tarifficator

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	its2 "a.yandex-team.ru/market/combinator/pkg/its"
)

func TestValidateCourierTariffRecall(t *testing.T) {
	fromRegion := geobase.RegionMoscowAndObl
	its := &its2.SimpleSettings{
		Values: map[string]interface{}{
			"validate_tariff_recall_regions": []interface{}{
				int(geobase.RegionMoscow),
			},
		},
		Constraints: map[string]uint8{"validate_tariff_recall_regions": its2.Global},
	}
	regionMap := geobase.RegionMap{
		geobase.RegionRussia: geobase.RegionChain{
			geobase.Region{
				ID: geobase.RegionRussia,
			},
		},
		geobase.RegionMoscow: geobase.RegionChain{
			geobase.Region{
				ID: geobase.RegionMoscow,
			},
			geobase.Region{
				ID: geobase.RegionRussia,
			},
		},
		geobase.RegionHamovniki: geobase.RegionChain{
			geobase.Region{
				ID: geobase.RegionHamovniki,
			},
			geobase.Region{
				ID: geobase.RegionMoscow,
			},
			geobase.Region{
				ID: geobase.RegionRussia,
			},
		},
		geobase.RegionKotelniki: geobase.RegionChain{
			geobase.Region{
				ID: geobase.RegionKotelniki,
			},
			geobase.Region{
				ID: geobase.RegionMoscow,
			},
			geobase.Region{
				ID: geobase.RegionRussia,
			},
		},
	}
	tf := &TariffsFinder{
		CourierDirectionDservicesMap: DirectionDservicesMap{
			FromToRegions{fromRegion, geobase.RegionRussia}: []uint64{1},
		},
	}
	assert.Error(t, validateCourierTariffsRecall(tf, regionMap, its))

	toHamovniki := FromToRegions{fromRegion, geobase.RegionHamovniki}
	tf.CourierDirectionDservicesMap[toHamovniki] = []uint64{2}
	//assert.Error(t, validateCourierTariffsRecall(tf, regionMap, its))

	toKotelniki := FromToRegions{fromRegion, geobase.RegionKotelniki}
	tf.CourierDirectionDservicesMap[toKotelniki] = []uint64{3}
	assert.NoError(t, validateCourierTariffsRecall(tf, regionMap, its))
}
