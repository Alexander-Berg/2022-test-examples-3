package unistat_test

import (
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.yandex-team.ru/telepath/telepath/pkg/unistat"
	. "gopkg.in/check.v1"
)

func Test(t *testing.T) { TestingT(t) }

type ValueSuite struct{}
type HistogramSuite struct{}
type RegistrySuite struct{}

var (
	_ = Suite(&ValueSuite{})
	_ = Suite(&HistogramSuite{})
	_ = Suite(&RegistrySuite{})
)

func (*ValueSuite) TestObserve(c *C) {
	m := unistat.NewValue("test_summ")
	c.Assert(m.Value(), Equals, 0.0)

	m.Observe(1)
	c.Assert(m.Value(), Equals, 1.0)

	m.Observe(99)
	c.Assert(m.Value(), Equals, 99.0)

	m.Observe(33)
	c.Assert(m.Value(), Equals, 33.0)
}

func (*HistogramSuite) TestObserve(c *C) {
	m := unistat.NewHistogram("test_hgram", 30, 40, 50)
	for _, n := range []float64{50, 40, 50, 30, 40} {
		m.Observe(n)
	}
	v := m.Value().([][]float64)
	c.Assert(v, DeepEquals, [][]float64{{30, 1}, {40, 2}, {50, 2}})
}

func (*RegistrySuite) TestHandler_servesJSON(c *C) {
	r := &unistat.Registry{}
	ts := httptest.NewServer(r.Handler())
	defer ts.Close()

	m := r.NewValue("b_summ")
	m.Observe(2)
	m = r.NewHistogram("a_hgram", 10, 20, 30)
	m.Observe(10)

	resp, err := http.Get(ts.URL)
	c.Assert(err, IsNil)

	b, err := ioutil.ReadAll(resp.Body)
	resp.Body.Close()

	c.Assert(err, IsNil)
	c.Assert(string(b), Equals, `[["a_hgram",[[10,1],[20,0],[30,0]]],["b_summ",2]]`+"\n")
}
