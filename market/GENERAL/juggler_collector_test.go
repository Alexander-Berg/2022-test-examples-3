package runner

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/market/sre/library/proto/juggler_pb"
	"a.yandex-team.ru/market/sre/library/proto/remon_pb"
	"context"
	"crypto/x509"
	"encoding/json"
	"fmt"
	"github.com/go-resty/resty/v2"
	"github.com/golang/protobuf/proto"
	"github.com/google/go-cmp/cmp"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"strconv"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

func setupTestServer(ssl bool, handler http.Handler) (*httptest.Server, *url.URL, Server) {
	var ts *httptest.Server
	if ssl {
		ts = httptest.NewTLSServer(handler)
	} else {
		ts = httptest.NewServer(handler)
	}
	u := new(url.URL)
	u, _ = u.Parse(ts.URL)
	p, _ := strconv.Atoi(u.Port())
	server := Server{
		Name:     "test",
		HostName: u.Hostname(),
		Port:     uint16(p),
		SSL:      &ssl,
	}
	return ts, u, server
}

func setupSyncs(poolSize uint16) (context.Context, context.CancelFunc, *PoolGroup, *sync.WaitGroup) {
	ctx, cancel := context.WithCancel(context.Background())
	pool := NewPoolGroup(ctx, poolSize)
	wg := sync.WaitGroup{}
	return ctx, cancel, pool, &wg
}

func setupCollector(ts *httptest.Server, u *url.URL, logger log.Logger) *JugglerCollector {
	c := new(JugglerCollector)
	c.Logger = logger
	c.RestyClient = resty.NewWithClient(ts.Client())
	c.JugglerRetries = 1
	c.JugglerURL = u
	return c
}

func writeOrError(logger log.Logger, w http.ResponseWriter, c int, b []byte) {
	w.Header()["Content-Type"] = []string{"application/json"}
	w.WriteHeader(c)
	_, err := w.Write(b)
	if err != nil {
		logger.Errorf("%s", err)
		os.Exit(1)
	}
}

func compare(t *testing.T, left, right interface{}) {
	if !cmp.Equal(left, right, cmp.Comparer(proto.Equal)) {
		t.Error(cmp.Diff(left, right, cmp.Comparer(proto.Equal)))
	}
}

func TestJugglerCollectorPoolAlreadyDone(t *testing.T) {
	logger := setupLogger()
	requestDone := false

	ts, u, server := setupTestServer(false, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requestDone = true
		writeOrError(logger, w, 200, []byte(""))
	}))
	defer ts.Close()

	c := setupCollector(ts, u, logger)

	c.Servers = []*Server{&server}

	ctx, cancel, pool, _ := setupSyncs(uint16(3))

	pool.AddToPool()
	cancel()
	c.Collect(ctx, pool)
	time.Sleep(time.Millisecond * 250)

	assert.NotEqual(t, true, requestDone)
}

func TestJugglerCollectorCancelDuringFetch(t *testing.T) {
	logger := setupLogger()
	requestCtx, requestCancel := context.WithCancel(context.Background())
	defer requestCancel()
	var requestCounter = new(int32)

	ts, u, server := setupTestServer(false, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		select {
		case <-time.After(time.Millisecond * 200):
		case <-requestCtx.Done():
			return
		}
		atomic.AddInt32(requestCounter, 1)
		writeOrError(logger, w, 200, []byte(""))
	}))
	defer ts.Close()

	c := setupCollector(ts, u, logger)

	c.Servers = []*Server{&server, &server, &server, &server, &server}

	ctx, cancel, pool, _ := setupSyncs(uint16(3))
	defer cancel()

	pool.AddToPool()
	go runCollect(c, ctx, pool)
	time.Sleep(time.Millisecond * 300)

	// Pool size 3, one reserved for main collect task, for 300ms can be completed only 2 requests(200ms each)
	assert.Equal(t, int32(2), *requestCounter)
}

func TestJugglerCollectorCollect(t *testing.T) {
	logger := setupLogger()
	var requestCounter = new(int32)
	var requestID = new(int32)

	events := []*remon_pb.JugglerEventsResponse{
		{
			Events: []*juggler_pb.Event{
				{
					Host:    "test",
					Service: "ssh",
					Status:  "ok",
				},
				{
					Host:    "test",
					Service: "mail",
					Status:  "ok",
				},
			},
		},
		{
			Events: []*juggler_pb.Event{
				{
					Host:    "test",
					Service: "ssh",
					Status:  "ok",
				},
			},
		},
		{
			Events: []*juggler_pb.Event{
				{
					Host:    "test",
					Service: "ssh",
					Status:  "ok",
					Tags:    []string{"orig_tag"},
				},
				{
					Host:    "test",
					Service: "web",
					Status:  "fail",
				},
			},
		},
		{},
		nil,
	}
	var requestEvents []*juggler_pb.Event
	mutex := sync.Mutex{}

	ts, u, server := setupTestServer(false, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/api/v1/gather/juggler" {
			atomic.AddInt32(requestCounter, 1)
			if !cmp.Equal(r.Header[remonHostHeader], []string{"failed"}) {
				id := atomic.AddInt32(requestID, 1)
				b, _ := json.Marshal(&events[id-1])
				writeOrError(logger, w, 200, b)
			} else {
				writeOrError(logger, w, 500, []byte(""))
			}
		} else {
			b, err := ioutil.ReadAll(r.Body)
			if err != nil {
				logger.Errorf("%s", err)
				os.Exit(1)
			}
			result := &juggler_pb.SendEventsRequest{}
			err = json.Unmarshal(b, result)
			if err != nil {
				logger.Errorf("%s", err)
				os.Exit(1)
			}
			mutex.Lock()
			requestEvents = result.Events
			mutex.Unlock()

			writeOrError(logger, w, 200, []byte("{}"))
		}
	}))
	defer ts.Close()

	c := setupCollector(ts, u, logger)

	ignoredServer := server
	ignoredServer.IgnoreModulesMap = map[string]bool{
		"juggler": true,
	}

	failedServer := server
	failedServer.Name = "failed"
	failedServer.Tags = []string{"failed_server"}

	c.Servers = []*Server{&server, &server, &server, &server, &ignoredServer, &failedServer}

	ctx, cancel, pool, _ := setupSyncs(uint16(3))
	defer cancel()

	pool.AddToPool()
	go runCollect(c, ctx, pool)

	pool.PoolWait()
	assert.Equal(t, int32(5), *requestCounter)
	totalEvents := 0

	for _, ev := range events {
		totalEvents += 1 // UNREACHABLE event, one for every server
		if ev != nil {
			for _, e := range ev.Events {
				totalEvents += 1
				contains := false
				for _, re := range requestEvents {
					if cmp.Equal(re, e, cmp.Comparer(proto.Equal)) {
						contains = true
						break
					}
				}
				if !contains {
					t.Errorf("mising event: %v", e)
				}
			}
		}
	}
	for _, s := range c.Servers {
		if s.IgnoreModulesMap == nil {
			contains := false
			for _, re := range requestEvents {
				if cmp.Equal(re, &juggler_pb.Event{
					Host:        s.Name,
					Service:     "UNREACHABLE",
					Status:      "CRIT",
					Description: fmt.Sprintf("remon-collector can not reach %s", s.Name),
					Tags:        []string{"failed_server"},
				}, cmp.Comparer(proto.Equal)) && s.Name == "failed" || cmp.Equal(re, &juggler_pb.Event{
					Host:        s.Name,
					Service:     "UNREACHABLE",
					Status:      "OK",
					Description: fmt.Sprintf("got response from %s", s.Name),
				}, cmp.Comparer(proto.Equal)) {
					contains = true
					break
				}
			}
			if !contains {
				t.Errorf("mising unreach event for %s", s.Name)
			}
		}
	}
	assert.Equal(t, totalEvents, len(requestEvents))
}

func TestJugglerCollectorCollectServerError(t *testing.T) {
	logger := setupLogger()
	var requestCounter = new(int32)

	ts, u, server := setupTestServer(false, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := atomic.AddInt32(requestCounter, 1)
		if id == 1 {
			time.Sleep(time.Millisecond * 300)
			writeOrError(logger, w, 200, []byte("{}"))
		} else if id == 2 {
			writeOrError(logger, w, 500, []byte("{}"))
		} else {
			logger.Error("Unexpected request")
			os.Exit(1)
		}
	}))
	defer ts.Close()

	c := setupCollector(ts, u, logger)
	c.RestyClient.SetTimeout(time.Millisecond * 200)

	ctx, cancel, pool, wg := setupSyncs(uint16(3))
	defer cancel()
	results := make(chan jugglerResult)

	pool.AddToPool()
	wg.Add(1)
	go c.JugglerCollectServer(ctx, pool, wg, &server, results)

	r := <-results
	wg.Wait()
	assert.Error(t, r.Error)
	compare(t, r.Events[0], &juggler_pb.Event{
		Host:        server.Name,
		Service:     "UNREACHABLE",
		Status:      "CRIT",
		Description: fmt.Sprintf("remon-collector can not reach %s", server.Name),
	})

	pool.AddToPool()
	wg.Add(1)
	go c.JugglerCollectServer(ctx, pool, wg, &server, results)

	r = <-results
	wg.Wait()
	assert.Error(t, r.Error)
	compare(t, r.Events[0], &juggler_pb.Event{
		Host:        server.Name,
		Service:     "UNREACHABLE",
		Status:      "CRIT",
		Description: fmt.Sprintf("remon-collector can not reach %s", server.Name),
	})

	pool.AddToPool()
	wg.Add(1)
	go c.JugglerCollectServer(ctx, pool, wg, &server, results)
	cancel()

	wg.Wait()
	assert.Equal(t, 0, len(results))

	close(results)
}

func TestJugglerCollectorCollectServer(t *testing.T) {
	logger := setupLogger()

	event := juggler_pb.Event{
		Host:    "testHost",
		Service: "ssh",
		Status:  "ok",
		Tags:    []string{"host_tag"},
	}

	expectedEvent := juggler_pb.Event{
		Host:    "test",
		Service: "ssh",
		Status:  "ok",
		Tags:    []string{"host_tag", "test_tag"},
	}

	ts, u, server := setupTestServer(false, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		b, _ := json.Marshal(remon_pb.JugglerEventsResponse{Events: []*juggler_pb.Event{&event}})
		writeOrError(logger, w, 200, b)
	}))
	defer ts.Close()

	server.Tags = []string{"test_tag"}

	c := setupCollector(ts, u, logger)
	c.RestyClient.SetTimeout(time.Millisecond * 200)

	ctx, cancel, pool, wg := setupSyncs(uint16(3))
	defer cancel()
	results := make(chan jugglerResult)

	pool.AddToPool()
	wg.Add(1)
	go c.JugglerCollectServer(ctx, pool, wg, &server, results)

	r := <-results
	wg.Wait()

	contains := false
	containsStatus := false
	for _, re := range r.Events {
		if cmp.Equal(re, &expectedEvent, cmp.Comparer(proto.Equal)) {
			contains = true
		}
		if cmp.Equal(re, &juggler_pb.Event{
			Host:        server.Name,
			Service:     "UNREACHABLE",
			Status:      "OK",
			Description: fmt.Sprintf("got response from %s", server.Name),
			Tags:        []string{"test_tag"},
		}, cmp.Comparer(proto.Equal)) {
			containsStatus = true
		}
	}
	assert.True(t, contains)
	assert.True(t, containsStatus)

	cancel()

	close(results)
}

func TestJugglerCollectorPush(t *testing.T) {
	logger := setupLogger()
	var requestCounter = new(int32)

	var events []*juggler_pb.Event
	expectedEvents := []*juggler_pb.Event{
		{
			Host:    "testHost",
			Service: "ssh",
			Status:  "ok",
		},
	}

	ts, u, _ := setupTestServer(false, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := atomic.AddInt32(requestCounter, 1)
		if id == 1 || id == 2 {
			writeOrError(logger, w, 500, []byte("{}"))
		} else if id == 3 {
			time.Sleep(time.Millisecond * 300)
		} else if id == 4 {
			b, err := ioutil.ReadAll(r.Body)
			if err != nil {
				logger.Errorf("%s", err)
				os.Exit(1)
			}
			result := &juggler_pb.SendEventsRequest{}
			err = json.Unmarshal(b, result)
			if err != nil {
				logger.Errorf("%s", err)
				os.Exit(1)
			}
			events = result.Events

			writeOrError(logger, w, 200, []byte("{}"))
		} else {
			logger.Error("Unexpected request")
			os.Exit(1)
		}
	}))
	defer ts.Close()

	c := setupCollector(ts, u, logger)
	c.JugglerRetries = 2

	ctx, cancel, _, _ := setupSyncs(uint16(3))
	defer cancel()
	err := c.JugglerPushEvents(ctx, expectedEvents)

	// Push error with 2 wrapped 500 errors
	for i := 0; i < 3; i++ {
		assert.Error(t, err)
		err = xerrors.Unwrap(err)
	}
	assert.NoError(t, err)

	c.JugglerRetries = 1
	c.RestyClient.SetTimeout(time.Millisecond * 200)

	err = c.JugglerPushEvents(ctx, expectedEvents)

	// Push error with 1 wrapped http timeout error with wrapped context timeout error
	for i := 0; i < 3; i++ {
		assert.Error(t, err)
		err = xerrors.Unwrap(err)
	}
	assert.NoError(t, err)

	err = c.JugglerPushEvents(ctx, expectedEvents)
	assert.NoError(t, err)
	assert.True(t, cmp.Equal(expectedEvents, events, cmp.Comparer(proto.Equal)))

	cancel()
	err = c.JugglerPushEvents(ctx, expectedEvents)
	assert.NoError(t, err)
}

func TestJugglerCollectorFilterEvents(t *testing.T) {
	logger := setupLogger()

	c := JugglerCollector{}
	c.Logger = logger

	sendEvents := []*juggler_pb.Event{
		{
			Host:    "test",
			Service: "ssh",
			Status:  "ok",
		},
		{
			Host:    "test2",
			Service: "mail",
			Status:  "ok",
		},
		{
			Host:    "test3",
			Service: "web",
			Status:  "fail",
		},
	}
	receivedEvents := []*juggler_pb.EventResponse{
		{
			Code: 200,
		},
		{
			Code:    429,
			Message: "To many events",
		},
		{
			Code:    500,
			Message: "To many events",
		},
	}
	expectedEvents := []*juggler_pb.Event{
		{
			Host:    "test2",
			Service: "mail",
			Status:  "ok",
		},
	}

	events, hasErrors := c.filterEvents(sendEvents, receivedEvents)

	compare(t, expectedEvents, events)
	assert.True(t, hasErrors)

	events, hasErrors = c.filterEvents([]*juggler_pb.Event{}, []*juggler_pb.EventResponse{receivedEvents[1]})
	assert.Equal(t, 0, len(events))
	assert.True(t, hasErrors)
}

func TestJugglerCollectorCollectServerSSL(t *testing.T) {
	logger := setupLogger()

	description := func(format, desk string, expire int) string {
		// Date can change during test run...
		if newDesk := fmt.Sprintf(format, expire-1); desk == newDesk {
			return newDesk
		}
		return fmt.Sprintf(format, expire)
	}

	ts, u, server := setupTestServer(true, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		writeOrError(logger, w, 200, []byte("{}"))
	}))
	defer ts.Close()

	cert, err := x509.ParseCertificate(ts.TLS.Certificates[0].Certificate[0])
	require.NoError(t, err)

	expire := int(time.Until(cert.NotAfter).Hours() / 24)

	c := setupCollector(ts, u, logger)
	c.RestyClient.SetTimeout(time.Millisecond * 200)

	ctx, cancel, pool, wg := setupSyncs(uint16(3))
	defer cancel()
	results := make(chan jugglerResult)

	c.ServerCertificateExpireDays = -10
	pool.AddToPool()
	wg.Add(1)
	go c.JugglerCollectServer(ctx, pool, wg, &server, results)

	r := <-results
	wg.Wait()
	assert.NoError(t, r.Error)
	compare(t, r.Events[0], &juggler_pb.Event{
		Host:        server.Name,
		Service:     "UNREACHABLE",
		Status:      "OK",
		Description: fmt.Sprintf("got response from %s", server.Name),
	})
	compare(t, r.Events[1], &juggler_pb.Event{
		Host:        server.Name,
		Service:     "ssl_expire",
		Status:      "OK",
		Description: description("OK: remon agent ssl certificate expire in %d days", r.Events[1].Description, expire),
	})

	c.ServerCertificateExpireDays = expire + 10
	pool.AddToPool()
	wg.Add(1)
	go c.JugglerCollectServer(ctx, pool, wg, &server, results)

	r = <-results
	wg.Wait()
	assert.NoError(t, r.Error)
	compare(t, r.Events[0], &juggler_pb.Event{
		Host:        server.Name,
		Service:     "UNREACHABLE",
		Status:      "OK",
		Description: fmt.Sprintf("got response from %s", server.Name),
	})
	compare(t, r.Events[1], &juggler_pb.Event{
		Host:        server.Name,
		Service:     "ssl_expire",
		Status:      "CRIT",
		Description: description("CRIT: remon agent ssl certificate expire in %d days. How-To: https://nda.ya.ru/t/pp4GN7oJ4TPkax", r.Events[1].Description, expire),
	})

	close(results)
}
