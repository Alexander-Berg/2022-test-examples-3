package counter_test

import (
	"testing"

	"github.yandex-team.ru/telepath/telepath/service/counter"
	. "gopkg.in/check.v1"
)

func Test(t *testing.T) { TestingT(t) }

type CounterSuite struct {
	cnt counter.Counter
}

var (
	_ = Suite(&CounterSuite{})
)

func (s *CounterSuite) SetUpTest(c *C) {
	db := &Database{mem: map[string]int64{}}
	s.cnt = counter.New(counter.Config{DB: db})
}

func (s *CounterSuite) TestNext(c *C) {
	var i int64
	for i = 1; i < 10; i++ {
		n, _ := s.cnt.Next("test")
		c.Assert(n, Equals, i)
	}
}

func (s *CounterSuite) TestNextN(c *C) {
	var i int64 = 1
	for j := 1; i < 30; i += int64(j * 3) {
		n, _ := s.cnt.NextN("test", 3)
		for k := 0; k < 3; k++ {
			c.Assert(n(), Equals, i+int64(k))
		}
	}
}

func (s *CounterSuite) TestNextN_sequence(c *C) {
	for i := 1; i < 30; i += 1 {
		n, _ := s.cnt.NextN("test", i)
		for j := 0; j < i; j++ {
			n()
		}
	}
}

func (s *CounterSuite) TestNextN_panics(c *C) {
	for _, n := range []int{0, -100} {
		(func() {
			defer func() { recover() }()
			s.cnt.NextN("test", n)
			c.Fail()
		})()
	}
}

type Database struct {
	mem map[string]int64
}

func (db *Database) GetCounter(name string) (n int64, err error) {
	return db.mem[name], nil
}

func (db *Database) SetCounter(name string, n int64) error {
	db.mem[name] = n
	return nil
}
