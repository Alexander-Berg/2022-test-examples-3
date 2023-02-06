package data

import (
	"strings"
	"testing"

	"a.yandex-team.ru/mail/logconsumers/actdb_consumer/metrics"
	"github.com/stretchr/testify/require"
)

var tskvLog = `
tskv	uid=00000000000466242936	module=wmi	unixtime=1504472971
tskv	uid=00000000000501481128	module=wmi	date=1504472973016
tskv	uid=00000000000219899014	module=mobile	date=1504472974754	unixtime=1504472974
tskv	uid=00000000000501481128	module=wmi	date=1505472973009	unixtime=1504472975
tskv	uid=00000000000020003987	foo=bar\tba=z\n	module=wmi	date=1504472975075	unixtime=1504472975
tskv	uid=00000000000321079085	foo=bar\tmodule=baz\tdate=1504472975075\n
tskv	uid=00000000000331874432	date=1504472980384	unixtime=1504472980
tskv	uid=00000000000031464462	module=wmi	date=1504472980834	unixtime=1504472980 hidden=0
tskv	unixtime=1504472981
tskv	uid=00000000000331874432	module=settings	date=1504472982313	unixtime=1504472982
tskv	uid=00001130000012162170	module=fastsrv	date=1504472985819	unixtime=1504472985
tskv	uid=00000000000136774475	module=mobile	date=1504472988167	unixtime=1504472988 hidden=1
tskv	ip=89.223.57.232	module=wmi	unixtime=1504472978`

func TestParseData(t *testing.T) {
	lines := strings.Split(tskvLog, "\n")
	// unistatPort := 5321
	// metrics.NewYasm(&unistatPort, &zap.Logger{})
	var res []ModuleActivity
	var errors []error
	for _, line := range lines {
		item, err := ParseData(line, &metrics.Yasm{})
		if err == nil {
			res = append(res, item)
		} else {
			errors = append(errors, err)
		}
	}

	require.Equal(t, len(res), 9)
	require.Equal(t, len(errors), 5)
	require.Equal(t, res[0].Module, Module("wmi"))
	require.Equal(t, res[7].UID, UID(1130000012162170))
	require.Equal(t, res[8].Date, Date("2017-09-04"))
}
