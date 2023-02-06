package lib_test

import (
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/util"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/nitc/lib"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

func TestMetrics_TaskStarted(t *testing.T) {
	reg := newTestRegistry()

	m := newTestMetrics(reg)

	user := "test_user"

	lastDump := getSnapshot(reg)
	m.TaskStarted(user)

	require.Equal(t, reflect.DeepEqual(lastDump, getSnapshot(reg)), false, "invalid active tasks count. Expected 1")

	lastDump = getSnapshot(reg)

	m.TaskStarted(user)

	require.Equal(t, reflect.DeepEqual(lastDump, getSnapshot(reg)), true, "One task started twice, but counter changed. Expected value 1")
}

func TestMetrics_TaskProceed_IgnoreAlreadyProceeded(t *testing.T) {
	reg := newTestRegistry()
	m := newTestMetrics(reg)

	user := "test_user"

	m.TaskStarted(user)

	lastDump := getSnapshot(reg)

	m.TaskProceed(user, time.Second, nil, false)
	require.Equal(t,
		false, reflect.DeepEqual(lastDump, getSnapshot(reg)), "Invalid task count. Task proceeded, but counter not changed")

	lastDump = getSnapshot(reg)

	m.TaskProceed(user, time.Second, nil, false)

	require.Equal(t, true, reflect.DeepEqual(lastDump, getSnapshot(reg)), "Invalid task count. One task proceeded twice, but counter changed")
}

func getSnapshot(reg *solomon.Registry) *solomon.Metrics {
	metrics, err := reg.Gather()
	if err != nil {
		panic(err)
	}
	return metrics
}

func newTestMetrics(reg *solomon.Registry) lib.Metrics {
	lg := util.MakeConsoleLogger()
	return *lib.NewMetrics(reg, lg)
}

func newTestRegistry() *solomon.Registry {
	opts := solomon.NewRegistryOpts()
	opts.AddTags(map[string]string{
		"project": "myproj",
		"service": "myservice",
	})
	reg := solomon.NewRegistry(opts)
	return reg
}
