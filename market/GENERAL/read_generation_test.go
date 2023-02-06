package biggeneration

import (
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/biggeneration/taskrunner"
	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/ytutil"
	"a.yandex-team.ru/yt/go/ypath"
)

// DummyReader implements RecentGenerationReader interface
type dummyReader struct {
	revision int
}

var _ RecentGenerationReader = &dummyReader{}

func (reader *dummyReader) ReadMeta(*BigGeneration) (*BigGeneration, error) {
	reader.revision += 1
	return &BigGeneration{
		Generation: ytutil.Generation{Version: strconv.Itoa(reader.revision)},
	}, nil
}

func (reader *dummyReader) ReadDaysOffByDyn(_ string, _ int64) (*daysoff.ServiceDaysOff, int64, error) {
	return nil, 0, nil
}

func (reader *dummyReader) ReadData(meta *BigGeneration, dataOld *GenerationData) (*GenerationData, error) {
	return &GenerationData{}, nil
}

func TestRace(t *testing.T) {
	reader := &dummyReader{}
	holder, err := NewRecentGenerationHolder(reader)
	assert.NoError(t, err)

	check := func(names ...string) {
		assert.Contains(t, names, holder.GetData().Meta.Version)
		assert.Contains(t, names, holder.GetMeta().Version)
	}

	check("1")

	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		err = holder.Update()
		assert.NoError(t, err)
		wg.Done()
	}()
	check("1", "2")

	wg.Wait()
	check("2")
}

func TestReadEmergencyGenerations(t *testing.T) {
	const JSON = `{
"generation_graph_hahn": "20090731_000000",
"generation_graph_arnold": "20150118_111111"}`
	settings, err := its.NewSettingsFromString(JSON, its.SettingsOptions{})
	require.NoError(t, err)
	rootDir := ypath.Path("//tmp/manushkin/combinator")
	{
		specs := MakeSourceSpecs(string(rootDir))
		generations := ytutil.EmergencyGenerations(settings, "hahn", specs)
		require.Len(t, generations, 1)
		require.Equal(t, ytutil.Generation{
			Service:   ServiceGraph,
			Version:   "20090731_000000",
			DirPath:   "//tmp/manushkin/combinator/graph/20090731_000000",
			Emergency: true,
		}, generations["graph"])
	}
	{
		specs := MakeSourceSpecs(string(rootDir))
		generations := ytutil.EmergencyGenerations(settings, "arnold", specs)
		require.Len(t, generations, 1)
		require.Equal(t, ytutil.Generation{
			Service:   ServiceGraph,
			Version:   "20150118_111111",
			DirPath:   "//tmp/manushkin/combinator/graph/20150118_111111",
			Emergency: true,
		}, generations["graph"])
	}
}

func TestSetDaysOff(t *testing.T) {
	reader := &dummyReader{}
	holder, err := NewRecentGenerationHolder(reader)
	assert.NoError(t, err)

	genDaysOff := &daysoff.ServiceDaysOff{
		Services: map[int64]daysoff.DisabledDatesMap{
			1991298: {
				125: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 41, 0, 385072000, time.UTC)),
				126: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 42, 0, 385072000, time.UTC)),
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				133: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				134: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				135: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				136: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
		},
	}

	wantDaysOff := &daysoff.ServiceDaysOff{
		Services: map[int64]daysoff.DisabledDatesMap{
			1991298: {
				125: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 41, 0, 385072000, time.UTC)),
				126: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 42, 0, 385072000, time.UTC)),
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				133: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				134: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				135: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				136: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				137: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
		},
	}

	failDaysOff := &daysoff.ServiceDaysOff{
		Services: map[int64]daysoff.DisabledDatesMap{
			1991298: {
				125: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 41, 0, 385072000, time.UTC)),
				126: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 42, 0, 385072000, time.UTC)),
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				133: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				134: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
				135: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
		},
	}

	holder.SetGeneration(
		&GenerationData{
			genDaysOff: genDaysOff,
		},
		&BigGeneration{},
	)

	newData := *holder.GetData()
	holder.SetDaysOff(&newData, wantDaysOff, 0)
	require.Equal(t, wantDaysOff, newData.daysOff)

	// TODO remove with flag
	holder.SetDaysOff(&newData, failDaysOff, 0)
	require.Equal(t, genDaysOff, newData.daysOff)
}

// Проверяем что граф зависимостей корректный:
// 1. зависимости существуют
// 2. зависимости разрешимы (нет циклов)
func TestCreateTaskRunnerSpecs(t *testing.T) {
	specs := createTaskRunnerSpecs(
		nil, // RecentGenerationDataReaderExt
		nil, // *BigGeneration
		nil, // *GenerationData
	)
	require.NoError(t, taskrunner.Check(specs))
}

// Проверяем applyNeedUpdate.
// Корректность вычисления необходимости обновления сервиса.
func TestCreateTaskRunnerSpecsAndApplyNeedUpdate(t *testing.T) {
	test := func(needUpdate map[string]bool, runner taskrunner.RunnerFunc) {
		specs := createTaskRunnerSpecs(
			nil, // RecentGenerationDataReaderExt
			nil, // *BigGeneration
			nil, // *GenerationData
		)
		fooCreated := false
		specs["foo"] = taskrunner.Spec{
			Run: func() error {
				fooCreated = true
				return nil
			},
		}
		barCreated := false
		specs["bar"] = taskrunner.Spec{
			Run: func() error {
				barCreated = true
				return nil
			},
			Depends: []string{"foo"},
		}
		var checkingLog []string
		specs = applyNeedUpdate(specs, func(service string) bool {
			if need, ok := needUpdate[service]; ok {
				checkingLog = append(checkingLog, service)
				return need
			}
			return false
		})
		result, err := runner(specs, taskrunner.RunOptions{SkipOnError: true})
		require.NoError(t, err)
		require.Len(t, result.Failed, 0)
		require.Len(t, result.Skipped, 0)
		require.Equal(t, needUpdate["foo"], fooCreated)
		require.Equal(t, needUpdate["foo"] || needUpdate["bar"], barCreated)
		if needUpdate["bar"] {
			require.Equal(t, []string{"foo", "bar"}, checkingLog)
		} else {
			require.Equal(t, []string{"foo", "bar", "foo"}, checkingLog)
		}
	}
	for _, runner := range []taskrunner.RunnerFunc{taskrunner.Run, taskrunner.RunParallel} {
		for _, foo := range []bool{false, true} {
			for _, bar := range []bool{false, true} {
				test(
					map[string]bool{
						"foo": foo,
						"bar": bar,
					},
					runner,
				)
			}
		}
	}
}
