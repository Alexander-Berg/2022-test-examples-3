package startrek

import (
	"context"
	"github.com/stretchr/testify/assert"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_GetMySelf(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/v2/myself", req.URL.String())
		_, _ = rw.Write([]byte(`{"self":"https://st-api.yandex-team.ru/v2/users/1120000000040768","uid":1120000000040768,"login":"kemsta","firstName":"Станислав","lastName":"Кем","display":"Станислав Кем","email":"kemsta@yandex-team.ru","office":{"self":"https://st-api.yandex-team.ru/v2/offices/206","id":"206","display":"Москва, БЦ Зубовский"},"external":false,"hasLicense":true,"dismissed":false,"useNewFilters":true}`))
	}))
	defer server.Close()
	client := NewClient(server.URL, server.Client(), "token")
	self, e := client.GetMySelf(context.TODO())
	assert.NoError(t, e)
	assert.NotNil(t, self)
	assert.NotEqual(t, self, &Self{})
}

func TestClient_GetIssues(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/v2/issues?filter=queue:CSADMIN&filter=status:open", req.URL.String())
		_, _ = rw.Write([]byte(`[{"self":"https://st-api.yandex-team.ru/v2/issues/CSADMIN-30547","id":"5dcaa78229af1d001e067514","key":"CSADMIN-30547","version":3,"summary":"market_common:iptruler","statusStartTime":"2019-11-12T12:37:22.203+0000","updatedBy":{"self":"https://st-api.yandex-team.ru/v2/users/1120000000017771","id":"csadmin","display":"cs admin"},"description":"CRIT on ((https://juggler.yandex-team.ru/check_details/?service=iptruler&host=market_common market_common:iptruler))\nAt 2019-11-12 15:37:19\n\nDescription:\n  checks don't cover all children: 'market_common:UNREACHABLE'\n\nSummary:\n  CRIT: 2\n  WARN: 0\n  INFO: 0\n  OK: 0\nChildren:\n  #|\n    || !!(red)CRIT!! |monpic01vt.market.yandex.net |iptruler | %% there's no matching event in checks 'market_common:UNREACHABLE' %% ||\n    || !!(red)CRIT!! |monpic02vt.market.yandex.net |iptruler | %% there's no matching event in checks 'market_common:UNREACHABLE' %% ||\n  |#\n\n\nSent from myt-prod.juggler.search.yandex.net","type":{"self":"https://st-api.yandex-team.ru/v2/issuetypes/2","id":"2","key":"task","display":"Задача"},"priority":{"self":"https://st-api.yandex-team.ru/v2/priorities/2","id":"2","key":"normal","display":"Средний"},"tags":["market_common:iptruler","cs_duty","cs_incident","formalized"],"createdAt":"2019-11-12T12:37:22.175+0000","createdBy":{"self":"https://st-api.yandex-team.ru/v2/users/1120000000020750","id":"robot-juggling","display":"Робот Juggler"},"commentWithoutExternalMessageCount":0,"votes":0,"commentWithExternalMessageCount":0,"queue":{"self":"https://st-api.yandex-team.ru/v2/queues/CSADMIN","id":"306","key":"CSADMIN","display":"Market SRE"},"updatedAt":"2019-11-13T09:02:04.325+0000","status":{"self":"https://st-api.yandex-team.ru/v2/statuses/1","id":"1","key":"open","display":"Открыт"},"favorite":false}]`))
	}))
	defer server.Close()
	client := NewClient(server.URL, server.Client(), "token")
	issueChan := make(chan Issue, 1)
	err := client.GetIssues(context.TODO(), issueChan, map[string]string{"queue": "CSADMIN", "status": "open"})
	assert.NoError(t, err)
	assert.NotNil(t, <-issueChan)
	issue, ok := <-issueChan
	assert.Equal(t, issue, Issue{})
	assert.False(t, ok)
}

func TestClient_CommentIssue(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/v2/issues/TEST-16379", req.URL.String())
		body, err := ioutil.ReadAll(req.Body)
		assert.NoError(t, err)
		assert.Equal(t, []byte("{\"comment\":\"message\"}\n"), body)
		_, _ = rw.Write([]byte(`{"self":"https://st-api.yandex-team.ru/v2/issues/TEST-16379","id":"5b895a1108d16b001ac58e79","key":"TEST-16379","version":36,"lastCommentUpdatedAt":"2019-11-14T11:45:39.532+0000","summary":"Бесхозные ресурсы yt-account","statusStartTime":"2019-11-14T11:45:15.837+0000","updatedBy":{"self":"https://st-api.yandex-team.ru/v2/users/1120000000040768","id":"kemsta","display":"Станислав Кем"},"sla":[{"id":"5b895a1108d16b001ac58e78","settingsId":58,"clockStatus":"STOPPED","violationStatus":"NOT_VIOLATED","warnThreshold":null,"failedThreshold":null,"warnAt":null,"failAt":null,"startedAt":"2018-08-31T15:09:05.594+0000","pausedAt":null,"pausedDuration":0,"toFailTimeWorkDuration":null,"spent":0,"previousSLAs":[]}],"description":"Найдены не привязанные ресурсы yt-account:\n* three@some\n* one\n* two\n\n\nа так же привязанные, но более не существующие:\n* three@some\n* one\n\n\nПожалуйста, актуализируйте список ресурсов сервисов в ((https://tsum.yandex-team.ru/registry реестре компонентов))\n\n<{ машиночитаемо\n##\nunlinkedResources=three@some,one,two\ndeletedResource=three@some,one\n##\n}>","type":{"self":"https://st-api.yandex-team.ru/v2/issuetypes/1","id":"1","key":"bug","display":"Ошибка"},"priority":{"self":"https://st-api.yandex-team.ru/v2/priorities/2","id":"2","key":"normal","display":"Средний"},"previousStatusLastAssignee":{"self":"https://st-api.yandex-team.ru/v2/users/1120000000040768","id":"kemsta","display":"Станислав Кем"},"tags":["market_resources:yt-account"],"createdAt":"2018-08-31T15:09:05.594+0000","followers":[{"self":"https://st-api.yandex-team.ru/v2/users/1120000000066684","id":"robot-mrk-infra-tst","display":"Пёс Тестовый"},{"self":"https://st-api.yandex-team.ru/v2/users/1120000000040768","id":"kemsta","display":"Станислав Кем"}],"createdBy":{"self":"https://st-api.yandex-team.ru/v2/users/1120000000040768","id":"kemsta","display":"Станислав Кем"},"commentWithoutExternalMessageCount":5,"votes":0,"commentWithExternalMessageCount":0,"assignee":{"self":"https://st-api.yandex-team.ru/v2/users/1120000000040768","id":"kemsta","display":"Станислав Кем"},"queue":{"self":"https://st-api.yandex-team.ru/v2/queues/TEST","id":"138","key":"TEST","display":"Песочница @ test"},"updatedAt":"2019-11-14T11:45:39.532+0000","status":{"self":"https://st-api.yandex-team.ru/v2/statuses/1","id":"1","key":"open","display":"Открыт"},"previousStatus":{"self":"https://st-api.yandex-team.ru/v2/statuses/3","id":"3","key":"closed","display":"Закрыт"},"bugDetectionMethod":"Manually","parent":{"self":"https://st-api.yandex-team.ru/v2/issues/CSADMIN-25082","id":"5b97889e083c43001ca5fb51","key":"CSADMIN-25082","display":"Бесхозные ресурсы yt-account"},"favorite":false}`))
	}))
	defer server.Close()
	client := NewClient(server.URL, server.Client(), "token")
	err := client.CommentIssue(context.TODO(), "TEST-16379", "message")
	assert.NoError(t, err)
}
