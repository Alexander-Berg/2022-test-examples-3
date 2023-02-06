package biggeneration

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/ytutil"
)

// DummyReader implements RecentGenerationMetaReader interface
type freshReader struct {
	proxy string
	name  string
}

var _ RecentGenerationMetaReader = &freshReader{}

func (reader *freshReader) ReadMeta(*BigGeneration) (*BigGeneration, error) {
	return &BigGeneration{
		Proxy:      reader.proxy,
		Generation: ytutil.Generation{Version: reader.name},
	}, nil
}

func (reader *freshReader) ReadDaysOffByDyn(_ string, _ int64) (*daysoff.ServiceDaysOff, int64, error) {
	return nil, 0, nil
}

func TestFindFreshGeneration_Error(t *testing.T) {
	res, err := FindFreshGeneration(&FindFreshGenerationOptions{})
	assert.Equal(t, ErrNoFreshGeneration, err)
	assert.Equal(t, (*BigGeneration)(nil), res.BigGeneration)
}

func TestFindFreshGeneration_Start(t *testing.T) {
	// logging.Setup(nil)
	proxyList := []string{"primary", "secondary"}
	tests := []struct {
		primary   string
		secondary string
		reason    string
		want      int
		err       error
	}{
		{
			primary:   "20200522_112233",
			secondary: "bad date",
			want:      0,
			reason:    ReasonOnlyOneCandidate,
		},
		{
			primary:   "20200522_120000",
			secondary: "20200522_110000",
			want:      0,
			reason:    ReasonPrimaryIsFresherThanSecondary,
		},
		{
			primary:   "20200522_110000",
			secondary: "20200522_112200",
			want:      0,
			reason:    ReasonPrimaryIsFreshEnough,
		},
		{
			primary:   "20200522_110000",
			secondary: "20200522_120000",
			want:      1,
			reason:    ReasonPrimaryIsTooOld,
		},
	}
	for _, tt := range tests {
		r0 := &freshReader{proxy: proxyList[0], name: tt.primary}
		r1 := &freshReader{proxy: proxyList[1], name: tt.secondary}
		names := []string{tt.primary, tt.secondary}
		bg, err := FindFreshGeneration(&FindFreshGenerationOptions{PrimaryReader: r0, SecondaryReader: r1})
		// assert.NoError(t, err)
		require.Equal(t, tt.err, err)
		require.Equal(t, names[tt.want], bg.Version)
		require.Equal(t, proxyList[tt.want], bg.Proxy)
		require.Equal(t, tt.reason, bg.Reason, tt)
	}
}

func TestFindFreshGeneration_Update(t *testing.T) {
	// logging.Setup(nil)
	proxyList := []string{"primary", "secondary"}
	tests := []struct {
		primary   string
		secondary string
		curName   string
		curProxy  int
		reason    string
		want      int
		err       error
	}{
		{
			primary:   "20200522_120000",
			secondary: "20200522_130000",
			curProxy:  0,
			curName:   "20200522_110000",
			want:      0,
			reason:    ReasonCurrentIsPrimary,
		},
		{
			primary:   "20200522_120000",
			secondary: "20200522_130000",
			curProxy:  1,
			curName:   "20200522_110000",
			want:      0,
			reason:    ReasonPrimaryIsFresherThanCurrent,
		},
		{
			primary:   "20200522_001100",
			secondary: "20200522_003300",
			curName:   "20200522_002200",
			curProxy:  1,
			want:      1,
			reason:    ReasonPrimaryIsOlderThanCurrent,
		},
	}
	for _, tt := range tests {
		r0 := &freshReader{proxy: proxyList[0], name: tt.primary}
		r1 := &freshReader{proxy: proxyList[1], name: tt.secondary}
		curGen := &BigGeneration{Proxy: proxyList[tt.curProxy], Generation: ytutil.Generation{Version: tt.curName}}
		names := []string{tt.primary, tt.secondary}
		bg, err := FindFreshGeneration(&FindFreshGenerationOptions{PrimaryReader: r0, SecondaryReader: r1, CurGen: curGen})
		require.Equal(t, tt.err, err, tt)
		require.Equal(t, names[tt.want], bg.Version, tt)
		require.Equal(t, proxyList[tt.want], bg.Proxy, tt)
		require.Equal(t, tt.reason, bg.Reason, tt)
	}
}
