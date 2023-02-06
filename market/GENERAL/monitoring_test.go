package monitoring

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/util/envtype"
	"a.yandex-team.ru/market/combinator/pkg/ytutil"
)

func TestCheckGenerations(t *testing.T) {
	tests := []struct {
		generations map[string]string
		wantCode    Code
		wantMsg     string
		want        string
		now         time.Time
	}{
		// ok
		{
			generations: map[string]string{
				bg.ServiceGraph:   "20200626_220000",
				bg.ServiceTariffs: "20200626_220000",
			},
			wantCode: CodeOk,
			wantMsg:  GenerationsOk,
			now:      time.Date(2020, 06, 26, 23, 0, 0, 0, time.Local), // Friday
		},
		// warn (bad generation)
		{
			generations: map[string]string{
				bg.ServiceGraph: "lala",
			},
			wantCode: CodeWarn,
			wantMsg:  "cant parse gen=lala service=graph cluster=",
			now:      time.Date(2020, 06, 26, 23, 0, 0, 0, time.Local), // Friday
		},
		// warn (not work time, Friday)
		{
			generations: map[string]string{
				bg.ServiceGraph:   "20200626_220000",
				bg.ServiceTariffs: "20200625_100000",
			},
			wantCode: CodeWarn,
			wantMsg:  "old service=tariffs, diff=37h0m0s(>24h0m0s), cluster=",
			now:      time.Date(2020, 06, 26, 23, 0, 0, 0, time.Local),
		},
		// warn (not work time, Friday)
		{
			generations: map[string]string{
				bg.ServiceTariffs: "20200605_220000",
			},
			wantCode: CodeWarn,
			wantMsg:  "old service=tariffs, diff=505h0m0s(>24h0m0s), cluster=",
			now:      time.Date(2020, 06, 26, 23, 0, 0, 0, time.Local), // Friday
		},
		// warn (not work time, Friday)
		{
			generations: map[string]string{
				bg.ServiceGraph:   "20200625_010000",
				bg.ServiceTariffs: "20200605_220000",
			},
			wantCode: CodeWarn,
			wantMsg:  "old service=graph, diff=46h0m0s(>1h0m0s), cluster=; old service=tariffs, diff=505h0m0s(>24h0m0s), cluster=",
			now:      time.Date(2020, 06, 26, 23, 0, 0, 0, time.Local),
		},
		// error(work time, Friday)
		{
			generations: map[string]string{
				bg.ServiceGraph:   "20200625_010000",
				bg.ServiceTariffs: "20200605_220000",
			},
			wantCode: CodeError,
			wantMsg:  "too old service=graph, diff=43h0m0s(>6h0m0s), cluster=; too old service=tariffs, diff=502h0m0s(>336h0m0s), cluster=",
			now:      time.Date(2020, 06, 26, 20, 0, 0, 0, time.Local),
		},
		// Warn (WeekEnd, Saturday)
		{
			generations: map[string]string{
				bg.ServiceGraph:   "20200625_010000",
				bg.ServiceTariffs: "20200605_220000",
			},
			wantCode: CodeWarn,
			wantMsg:  "old service=graph, diff=59h0m0s(>1h0m0s), cluster=; old service=tariffs, diff=518h0m0s(>24h0m0s), cluster=",
			now:      time.Date(2020, 06, 27, 12, 0, 0, 0, time.Local),
		},
	}

	makeGenerations := func(gens map[string]string) ytutil.Generations {
		result := make(ytutil.Generations)
		for service, name := range gens {
			result[service] = ytutil.Generation{Service: service, Version: name}
		}
		return result
	}

	for i, tt := range tests {
		result := checkGenerations(makeGenerations(tt.generations), &tt.now, envtype.Unknown, "")
		code, msg := result.GetCodeAndMessage()
		t.Log(i)
		t.Logf("want code: %d, msg: %s", tt.wantCode, tt.wantMsg)
		t.Logf("real code: %d, msg: %s", code, msg)
		require.Equal(t, tt.wantCode, code)
		require.Equal(t, tt.wantMsg, msg)
		if tt.want != "" {
			require.Equal(t, tt.want, result.String())
		}
	}
}
