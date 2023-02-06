package runtimeinfo

import (
	"testing"

	"github.com/stretchr/testify/require"
)

var (
	hostnameInfo = map[string]Info{
		"production-market-combinator-man-1.man.yp-c.yandex.net": {
			DataCenter:    "man",
			MachineNumber: 1,
		},
		"production-market-combinator-sas-1.sas.yp-c.yandex.net": {
			DataCenter:    "sas",
			MachineNumber: 1,
		},
		"production-market-combinator-vla-1.vla.yp-c.yandex.net": {
			DataCenter:    "vla",
			MachineNumber: 1,
		},
		"testing-market-combinator-sas-2.sas.yp-c.yandex.net": {
			DataCenter:    "sas",
			MachineNumber: 2,
		},
		"man0-1828-6ee-man-market-prod--ed4-28250.gencfg-c.yandex.net": {
			DataCenter:    "man",
			MachineNumber: 0,
		},
		"man11-1828-6ee-man-market-prod--ed4-28250.gencfg-c.yandex.net": {
			DataCenter:    "man",
			MachineNumber: 11,
		},
		"production-market-combinator-vla-11.vla.yp-c.yandex.net": {
			DataCenter:    "vla",
			MachineNumber: 11,
		},
	}
)

func TestHostParse(t *testing.T) {
	i := Info{}

	for hostname, parsedInfo := range hostnameInfo {
		require.Equal(t, parsedInfo.DataCenter, i.getDataCenter(hostname))
		require.Equal(t, parsedInfo.MachineNumber, i.getMachineNumber(hostname))
	}
}
