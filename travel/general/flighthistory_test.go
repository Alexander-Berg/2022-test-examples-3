package flight

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestHistory_GetHistory(t *testing.T) {
	expect := assert.New(t)

	h := NewFlightHistory(3)

	h.SaveFlight("SU 1404")
	expect.ElementsMatch([]string{"SU 1404"}, h.GetHistory())

	h.SaveFlight("DP 404")
	expect.ElementsMatch([]string{"SU 1404", "DP 404"}, h.GetHistory())

	h.SaveFlight("FV 1643")
	expect.ElementsMatch([]string{"SU 1404", "DP 404", "FV 1643"}, h.GetHistory())

	h.SaveFlight("WZ 425")
	expect.ElementsMatch([]string{"DP 404", "FV 1643", "WZ 425"}, h.GetHistory())

	h.SaveFlight("DP 404")
	expect.ElementsMatch([]string{"DP 404", "FV 1643", "WZ 425"}, h.GetHistory())

	h.SaveFlight("AA 325")
	expect.ElementsMatch([]string{"DP 404", "WZ 425", "AA 325"}, h.GetHistory())
}
