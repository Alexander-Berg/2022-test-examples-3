package data

import (
	"errors"
	"testing"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/mail/logconsumers/actdb_consumer/metrics"
	"a.yandex-team.ru/mail/logconsumers/actdb_consumer/storage"
	"github.com/stretchr/testify/require"
)

type TestStorage struct {
	storage.IStorage
	callTimes int
}

func (s *TestStorage) Run(sql string, yasm *metrics.Yasm) (bool, error) {
	s.callTimes += 1
	if s.callTimes <= 3 {
		return false, errors.New("storage error")
	}
	return true, nil
}

var l = zap.Must(zap.CLIConfig(log.InfoLevel))

func TestBuffer(t *testing.T) {
	var buffer Buffer
	var storage TestStorage

	require.Equal(t, buffer.Length(), 0)

	item1 := ModuleActivity{"mod", 1, "2021-10-10", false}
	item2 := ModuleActivity{"foo", 1, "2021-10-10", true}
	item3 := ModuleActivity{"mod", 2, "2021-10-11", false}
	item4 := ModuleActivity{"mod", 3, "2021-10-11", true}
	item5 := ModuleActivity{"mod", 3, "2021-10-11", false}
	item6 := ModuleActivity{"mod", 3, "2021-10-12", false}

	buffer.Add(&item1)
	require.Equal(t, buffer.Length(), 1)

	buffer.Add(&item2)
	buffer.Add(&item3)
	buffer.Add(&item4)
	buffer.Add(&item5)
	require.Equal(t, buffer.Length(), 3)

	invalidItem := ModuleActivity{}
	buffer.Add(&invalidItem)
	require.Equal(t, buffer.Length(), 3)

	buffer.Add(&item6)
	sql1 := buffer.makeSQL("2021-10-10")
	require.Contains(t, sql1, "INSERT INTO history.user_activity VALUES")
	require.Contains(t, sql1, "(1, 'mod', '2021-10-10')")
	require.Contains(t, sql1, "(1, 'foo', '2021-10-10')")
	require.Contains(t, sql1, "ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2021-10-10' WHERE history.user_activity.last_dt != '2021-10-10'")
	sql2 := buffer.makeSQL("2021-10-11")
	require.Contains(t, sql2, "INSERT INTO history.user_activity VALUES")
	require.Contains(t, sql2, "(2, 'mod', '2021-10-11')")
	require.Contains(t, sql2, "(3, 'mod', '2021-10-11')")
	require.Contains(t, sql2, "ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2021-10-11' WHERE history.user_activity.last_dt != '2021-10-11'")
	sql3 := buffer.makeSQL("2021-10-12")
	require.Equal(t, sql3, "INSERT INTO history.user_activity VALUES (3, 'mod', '2021-10-12') ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2021-10-12' WHERE history.user_activity.last_dt != '2021-10-12'")

	buffer.Flush(&storage, l, &metrics.Yasm{})
	require.Equal(t, storage.callTimes, 3)
	require.Equal(t, buffer.Length(), 4)

	buffer.Flush(&storage, l, &metrics.Yasm{})
	require.Equal(t, storage.callTimes, 6)
	require.Equal(t, buffer.Length(), 0)
}
