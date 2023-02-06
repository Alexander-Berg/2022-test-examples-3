package metrics_test

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/metrics"
)

func TestEmpty(t *testing.T) {
	reg := metrics.NewRegistry(map[string]string{
		"project": "myproj",
		"service": "myservice",
	})

	require.Equal(t, `{"commonLabels":{"project":"myproj","service":"myservice"},"sensors":[]}`, string(reg.Dump()))
}

func TestRate(t *testing.T) {
	reg := metrics.NewRegistry(map[string]string{
		"project": "myproj",
		"service": "myservice",
	})

	r := reg.Rate("error_rate", map[string]string{
		"user": "user1",
	})

	require.Equal(t, `{"commonLabels":{"project":"myproj","service":"myservice"},"sensors":[{"value":0,"labels":{"sensor":"error_rate","user":"user1"},"kind":"RATE"}]}`, string(reg.Dump()))

	r.Inc()

	require.Equal(t, `{"commonLabels":{"project":"myproj","service":"myservice"},"sensors":[{"value":1,"labels":{"sensor":"error_rate","user":"user1"},"kind":"RATE"}]}`, string(reg.Dump()))
}

func TestHistogram(t *testing.T) {
	reg := metrics.NewRegistry(map[string]string{
		"project": "myproj",
		"service": "myservice",
	})

	h := reg.HistogramRate("rps", []int{100, 300, 1000, 3000, 10000}, map[string]string{
		"user": "user1",
	})

	require.Equal(t, `{"commonLabels":{"project":"myproj","service":"myservice"},"sensors":[{"hist":{"bounds":[100,300,1000,3000,10000],"buckets":[0,0,0,0,0],"inf":0},"labels":{"sensor":"rps","user":"user1"},"kind":"HIST_RATE"}]}`, string(reg.Dump()))

	h.Record(90)
	h.Record(124)
	h.Record(200)
	h.Record(300000)
	h.Record(400000)
	h.Record(500000)

	require.Equal(t, `{"commonLabels":{"project":"myproj","service":"myservice"},"sensors":[{"hist":{"bounds":[100,300,1000,3000,10000],"buckets":[1,2,0,0,0],"inf":3},"labels":{"sensor":"rps","user":"user1"},"kind":"HIST_RATE"}]}`, string(reg.Dump()))
}
