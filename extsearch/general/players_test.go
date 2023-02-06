package players_test

import (
	"a.yandex-team.ru/extsearch/video/station/starter/players"

	"github.com/stretchr/testify/assert"

	"encoding/base64"
	"testing"
)

func TestHostConfigLoader(t *testing.T) {
	whitelist, err := players.NewHostWhitelist("unban.json")
	assert.NoError(t, err)

	assert.Equal(t, whitelist.IsAllowed("youtube", "www.someembed"), true)
	assert.Equal(t, whitelist.IsAllowed("youtube_graph", "www.youtube.com"), true)
	assert.Equal(t, whitelist.IsAllowed("not playerid", "not a host"), false)
}

func TestB64(t *testing.T) {
	b64 := "eyJtc2ciOnsiYXZhaWxhYmxlIjoxLCJkZXNjcmlwdGlvbiI6ItCt0YLQviDRgtGA0LXRgtGM0LUg0L/QvtC60L7Qu9C10L3QuNC1IEJNVyBYNSDRgSDQuNC90LTQtdC60YHQvtC8IEYxNS4gIiwiZHVyYXRpb24iOjMyNzcsIm5hbWUiOiJCTVcgWDUg0LfQsCDigqw1MGsgLSDQqNCQ0KDQkCDQuNC70Lgg0J3QldCiPyAg0KfRgtC+0J/QvtGH0LXQvCBzMDdlMDgiLCJuZXh0X2l0ZW1zIjpbXSwicGxheV91cmkiOiI8aWZyYW1lIHNyYz1cIi8vd3d3LnlvdXR1YmUuY29tL2VtYmVkL0tHcGF2QjdiVkhFP2VuYWJsZWpzYXBpPTEmYW1wO3dtb2RlPW9wYXF1ZVwiIGZyYW1lYm9yZGVyPVwiMFwiIHNjcm9sbGluZz1cIm5vXCIgYWxsb3dmdWxsc2NyZWVuPVwiMVwiIGFsbG93PVwiYXV0b3BsYXk7IGZ1bGxzY3JlZW5cIiBhcmlhLWxhYmVsPVwiVmlkZW9cIj48L2lmcmFtZT4iLCJwcmV2aW91c19pdGVtcyI6W10sInByb3ZpZGVyX2l0ZW1faWQiOiJodHRwOi8vd3d3LnlvdXR1YmUuY29tL3dhdGNoP3Y9S0dwYXZCN2JWSEUiLCJwcm92aWRlcl9uYW1lIjoieW91dHViZS5jb20iLCJzb3VyY2VfaG9zdCI6Ind3dy55b3V0dWJlLmNvbSIsInRodW1ibmFpbF91cmxfMTZ4OSI6Ii8vYXZhdGFycy5tZHMueWFuZGV4Lm5ldC9nZXQtdmlkZW9fdGh1bWJfZnJlc2gvOTA5NzEzLzMzZTVkM2E5YzFkYzJhOTNmZmU1MTg0NDliZjYzZWQ0MDMzMC8zMjB4MTgwIiwidGh1bWJuYWlsX3VybF8xNng5X3NtYWxsIjoiLy9hdmF0YXJzLm1kcy55YW5kZXgubmV0L2dldC12aWRlb190aHVtYl9mcmVzaC85MDk3MTMvMzNlNWQzYTljMWRjMmE5M2ZmZTUxODQ0OWJmNjNlZDQwMzMwLzMyMHgxODAiLCJ0eXBlIjoidmlkZW8iLCJ2aWV3X2NvdW50IjoiNDYg0YLRi9GBLiDQv9GA0L7RgdC80L7RgtGA0L7QsiJ9LCJkZXZpY2UiOiI3NDEwNTAzNDQ0MGMwODBhMDRjZSJ9"

	b64Bytes, err := base64.RawStdEncoding.DecodeString(b64)
	assert.NoError(t, err)

	orig := `{"msg":{"available":1,"description":"Это третье поколение BMW X5 с индексом F15. ","duration":3277,"name":"BMW X5 за €50k - ШАРА или НЕТ?  ЧтоПочем s07e08","next_items":[],"play_uri":"<iframe src=\"//www.youtube.com/embed/KGpavB7bVHE?enablejsapi=1&amp;wmode=opaque\" frameborder=\"0\" scrolling=\"no\" allowfullscreen=\"1\" allow=\"autoplay; fullscreen\" aria-label=\"Video\"></iframe>","previous_items":[],"provider_item_id":"http://www.youtube.com/watch?v=KGpavB7bVHE","provider_name":"youtube.com","source_host":"www.youtube.com","thumbnail_url_16x9":"//avatars.mds.yandex.net/get-video_thumb_fresh/909713/33e5d3a9c1dc2a93ffe518449bf63ed40330/320x180","thumbnail_url_16x9_small":"//avatars.mds.yandex.net/get-video_thumb_fresh/909713/33e5d3a9c1dc2a93ffe518449bf63ed40330/320x180","type":"video","view_count":"46 тыс. просмотров"},"device":"74105034440c080a04ce"}`

	origB64 := base64.RawStdEncoding.EncodeToString([]byte(orig))
	assert.Equal(t, origB64, b64)
	assert.Equal(t, orig, string(b64Bytes))
}
