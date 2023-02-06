package geotools

import "testing"

const (
	MskLatitude    = 55.753676
	MskLongitude   = 37.619899
	SpbLatitude    = 59.939095
	SpbLongitude   = 30.315868
	MskSpbDistance = 635153
)

func TestDistance(t *testing.T) {
	t.Run("msk-spb", func(t *testing.T) {
		d := GeoDistance(MskLatitude, MskLongitude, SpbLatitude, SpbLongitude)
		if int(d) != MskSpbDistance {
			t.Errorf("Actual=%v. expected=%v", d, MskSpbDistance)
		}
	})
}
