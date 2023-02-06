package pusher_test

import (
	"encoding/json"
	"io/ioutil"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/extsearch/video/station/starter/pusher"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
)

func TestGenJson(t *testing.T) {
	t.Skip("need fix")

	args := pusher.TVideoMessage{}
	args.IsAvaliable = 1
	args.Description = "Поиск. "
	args.Duration = 784
	args.Name = "Приколы с кошками и котами #4.  Подборка смешных и интересных видео с котиками и кошечками"
	args.PlayURI = "https://yastatic.net/video-player/0x0fd98704a3/pages-common/ok/ok.html" +
		"#html=%3Ciframe%20src%3D%22//ok.ru/videoembed/574529014512%3Fautoplay%3D1%26amp;ya%3D1%22%20f" +
		"rameborder%3D%220%22%20scrolling%3D%22no%22%20allowfullscreen%3D%221%22%20allow%3D%22autoplay" +
		";%20fullscreen%22%20aria-label%3D%22Video%22%3E%3C/iframe%3E&autoplay=yes&tv=1&clean=true"
	args.ProviderItemID = "http://ok.ru/video/574529014512"
	args.ProviderName = "yavideo"
	args.SourceHost = "ok.ru"
	args.ThumbnailURL = "https://avatars.mds.yandex.net/get-vthumb/937544/dded0a2e288230f2f82384c8335b6a2f/800x360"
	args.ThumbnailURLSmall = "https://avatars.mds.yandex.net/get-vthumb/937544/dded0a2e288230f2f82384c8335b6a2f/800x360"
	args.Type = "video"
	args.ViewCount = "104"
	args.NextItems = make([]int32, 0)
	args.PreviousItems = make([]int32, 0)

	argsV := pusher.TXivaData{}
	argsV.VideoMessage = args
	argsV.DeviceID = "9400503441102c1d098b"

	_, _ = zap.New(zap.JSONConfig(log.TraceLevel))
	got := pusher.GenerateJSON(argsV)

	rawExpected, err := ioutil.ReadFile("msg.json")
	assert.NoError(t, err)

	var expectedMsg map[string]interface{}
	err = json.Unmarshal(rawExpected, &expectedMsg)
	assert.NoError(t, err)

	expectedStr, _ := json.Marshal(expectedMsg)
	gotStr, _ := json.Marshal(got)

	assert.Equal(t, string(expectedStr), string(gotStr))
}
