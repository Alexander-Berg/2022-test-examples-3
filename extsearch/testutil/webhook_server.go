package testutil

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"sync"

	opb "a.yandex-team.ru/extsearch/video/robot/rt_transcoder/proto"
)

type WebhookServer struct {
	http.Handler

	srv http.Server

	mut      sync.Mutex
	payloads []*opb.TTaskInfo
}

func NewWwebhookServer(port uint32) (*WebhookServer, error) {
	res := &WebhookServer{}

	res.srv.Addr = fmt.Sprintf(":%d", port)
	res.srv.Handler = res

	go func() {
		err := res.srv.ListenAndServe()
		if err != http.ErrServerClosed {
			panic(err)
		}
	}()

	return res, nil
}

func (s *WebhookServer) Stop() error {
	return s.srv.Close()
}

func (s *WebhookServer) GetCalls() []*opb.TTaskInfo {
	s.mut.Lock()
	defer s.mut.Unlock()
	return s.payloads
}

func (s *WebhookServer) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		panic(fmt.Errorf("unexpected http method: %s", r.Method))
	}

	raw, err := ioutil.ReadAll(r.Body)
	if err != nil {
		panic(err)
	}

	s.mut.Lock()
	defer s.mut.Unlock()

	var info opb.TTaskInfo
	err = json.Unmarshal(raw, &info)
	if err != nil {
		panic(err)
	}

	s.payloads = append(s.payloads, &info)
}
