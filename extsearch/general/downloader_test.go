package downloader_test

import (
	"a.yandex-team.ru/extsearch/video/station/starter/downloader"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"context"
	"crypto/sha1"
	"crypto/tls"
	"encoding/base64"
	"io/ioutil"
	"net"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func generateTestClient(handler http.Handler) (*http.Client, func()) {
	ts := httptest.NewTLSServer(handler)
	cli := &http.Client{
		Transport: &http.Transport{
			DialContext: func(_ context.Context, network, _ string) (net.Conn, error) {
				return net.Dial(network, ts.Listener.Addr().String())
			},
			TLSClientConfig: &tls.Config{
				InsecureSkipVerify: true,
			},
		},
	}
	return cli, ts.Close
}

func TestRedirect(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		query := r.URL.Query()
		var body []byte
		var err error

		switch r.URL.Path {
		case "/video":
			id := query.Get("id")
			hasher := sha1.New()
			_, err := hasher.Write([]byte(id))
			if err != nil {
				panic(err)
			}
			sha := base64.URLEncoding.EncodeToString(hasher.Sum(nil))
			switch sha {
			case "6hmb2GdTp4Q9smmLbOZUX2lWwV0=":
				http.Redirect(w, r, "/video.mp4?"+r.URL.RawQuery, http.StatusFound)
			case "YrBAFyQoV4czcTo49JzaRo1ioB4=":
				http.Redirect(w, r, "/video.m3u8?"+r.URL.RawQuery, http.StatusFound)
			}
		case "/video.mp4":
			body, err = ioutil.ReadFile(r.URL.Path[1:])
		case "/video.m3u8":
			body, err = ioutil.ReadFile(r.URL.Path[1:])
		}
		require.NoError(t, err)

		_, err = w.Write(body)
		require.NoError(t, err)
	})

	client, endcall := generateTestClient(handler)
	defer endcall()

	dl := downloader.NewDownloader("1", "", client, "")
	resURL, err := dl.StartDownload("http://www.youtube.com/123", "htpsadad")
	require.NoError(t, err)
	assert.Equal(t, "https://quasar-proxy.yandex.net/video.mp4?id=ChpodHRwOi8vd3d3LnlvdXR1YmUuY29tLzEyMxIIaHRwc2FkYWQqIAhUOTHQcX7DX48ZsmR-OfNri7V1BD98MWk-9tON_KSBMAE", resURL)

	playURL := `https://yastatic.net/video-player/0xbd7fce742c/pages-common/iframe-default/iframe-default.html#` +
		`html=%3Ciframe%20src%3D%22%2F%2F24v.tv%2FembedPlayer%2F1918870%3Fautoplay%3D1%22%20frameborder` +
		`%3D%220%22%20scrolling%3D%22no%22%20allowfullscreen%3D%221%22%20allow%3D%22autoplay%3B` +
		`%20fullscreen%3B%20accelerometer%3B%20gyroscope%3B%20picture-in-picture%22%20aria-label%3D%22Video%22%3E%3C%2Fiframe%3E`

	resURL, err = dl.StartDownload("http://www.24video.sexy/video/view/1918870", playURL)
	require.NoError(t, err)
	assert.Equal(t, `https://quasar-proxy.yandex.net/video.m3u8?id=CipodHRwOi8vd3d3LjI0dmlkZW8uc2V4eS92aWRlby92aWV3LzE5MTg4NzASig`+
		`NodHRwczovL3lhc3RhdGljLm5ldC92aWRlby1wbGF5ZXIvMHhiZDdmY2U3NDJjL3BhZ2VzLWNvbW1vbi9pZnJhbWUtZGVmYXVsdC9pZnJhbWUtZGVmYXVs`+
		`dC5odG1sI2h0bWw9JTNDaWZyYW1lJTIwc3JjJTNEJTIyJTJGJTJGMjR2LnR2JTJGZW1iZWRQbGF5ZXIlMkYxOTE4ODcwJTNGYXV0b3BsYXklM0QxJTIyJT`+
		`IwZnJhbWVib3JkZXIlM0QlMjIwJTIyJTIwc2Nyb2xsaW5nJTNEJTIybm8lMjIlMjBhbGxvd2Z1bGxzY3JlZW4lM0QlMjIxJTIyJTIwYWxsb3clM0QlMjJh`+
		`dXRvcGxheSUzQiUyMGZ1bGxzY3JlZW4lM0IlMjBhY2NlbGVyb21ldGVyJTNCJTIwZ3lyb3Njb3BlJTNCJTIwcGljdHVyZS1pbi1waWN0dXJlJTIyJTIwYX`+
		`JpYS1sYWJlbCUzRCUyMlZpZGVvJTIyJTNFJTNDJTJGaWZyYW1lJTNFKiA1OZxvzaHu1cDNmIfsk2hRwJUjbQR-9g2gVO5vxT55hDAB`, resURL)
}

func TestTimeout(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(12 * time.Second)
	})

	client, endcall := generateTestClient(handler)
	defer endcall()

	dl := downloader.NewDownloader("1", "", client, "")

	start := time.Now()
	resURL, err := dl.StartDownload("http://www.youtube.com/123", "htpsadad")
	sec := time.Since(start).Seconds()
	assert.True(t, sec < 8)
	require.Error(t, err)
	assert.Equal(t, "", resURL)
}
