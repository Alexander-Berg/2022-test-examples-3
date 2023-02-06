package future

import (
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

//============= Mocks =====================================================
type futureMock struct {
	val     interface{}
	err     error
	timeout time.Duration
}

func (mock *futureMock) Get() (interface{}, error) {
	return mock.val, mock.err
}

func (mock *futureMock) GetTimed(t time.Duration) (interface{}, error) {
	if mock.timeout < t {
		return mock.Get()
	}
	return mock.val, TimeoutError(t.String())
}

func mockFuture(i interface{}, e error) Future {
	return &futureMock{i, e, 0}
}

func mockFutureTimed(i interface{}, tm time.Duration) Future {
	return &futureMock{i, nil, tm}
}

func mockFunc(i interface{}, e error) Functor {
	return func() (interface{}, error) { return i, e }
}

//=========== future.Future and future.Promise tests =====================

func TestPromise_DoubleSetCall_WillNotBlock(t *testing.T) {
	p, _ := NewPromiseFuture()
	p.Set(nil, nil)
	p.Set(nil, nil)
}

func TestPromiseFuture_SetGet_ProvidesValue(t *testing.T) {
	p, f := NewPromiseFuture()
	p.Set(666, nil)
	i, _ := f.Get()
	AssertThat(t, i, Is{V: 666})
}

func TestPromiseFuture_SetErrorGet_ProvidesValue(t *testing.T) {
	p, f := NewPromiseFuture()
	p.Set(nil, Error(""))
	_, e := f.Get()
	AssertThat(t, e, Is{V: Error("")})
}

func TestPromiseFuture_SetGetTimed_ProvidesValue(t *testing.T) {
	p, f := NewPromiseFuture()
	p.Set(666, nil)
	i, _ := f.GetTimed(1 * time.Second)
	AssertThat(t, i, Is{V: 666})
}

func TestPromiseFuture_SetGetTimedWithTimeoutExpired_ProvidesValue(t *testing.T) {
	_, f := NewPromiseFuture()
	_, e := f.GetTimed(0 * time.Second)
	AssertThat(t, e, TypeOf{V: TimeoutError("0")})
}

func TestFuture_GetDoubleCall_ProvidesError(t *testing.T) {
	p, f := NewPromiseFuture()
	p.Set(nil, nil)
	_, _ = f.Get()
	_, e := f.Get()
	AssertThat(t, e, Is{V: Error("Promise is not set")})
}

func TestFutureUninitialized_Get_ProvidesError(t *testing.T) {
	f := future{}
	_, e := f.Get()
	AssertThat(t, e, Is{V: Error("Promise is not set")})
}

//======================== future.Async tests ============================

func TestAsync_CreatesFutureForFunctorWhichProvidesValue_AndFutureProvidesValue(t *testing.T) {
	f := mockFunc(123, nil).Async()
	i, _ := f.Get()
	AssertThat(t, i, Is{V: 123})
}

func TestAsync_CreatesFutureForFunctorWhichProvidesError_AndFutureProvidesError(t *testing.T) {
	f := mockFunc(nil, Error("TestError")).Async()
	_, e := f.Get()
	AssertThat(t, e, Is{V: Error("TestError")})
}

//======================== future.Group tests ============================

func TestFutureGroup_GetWithEmptyGroup_ReturnsNoResults(t *testing.T) {
	group := FutureGroup{}
	i, _ := group.Get()
	AssertThat(t, len(i), Is{V: 0})
}

func TestFutureGroup_GetWithEmptyGroup_ReturnsNoErrors(t *testing.T) {
	group := FutureGroup{}
	_, e := group.Get()
	AssertThat(t, len(e), Is{V: 0})
}

func TestFutureGroup_GetWithNonEmptyGroup_ClearsGroup(t *testing.T) {
	group := FutureGroup{mockFuture(nil, nil)}
	group.Get()
	AssertThat(t, len(group), Is{V: 0})
}

func TestFutureGroup_WithFuturesWithResultsGet_ReturnsSliceOfResults(t *testing.T) {
	group := FutureGroup{mockFuture(666, nil), mockFuture(777, nil)}
	i, e := group.Get()
	AssertThat(t, i, ElementsAre{777, 666})
	AssertThat(t, len(e), Is{V: 0})
}

func TestFutureGroup_WithFuturesWithResultsGetTimed_ReturnsSliceOfResults(t *testing.T) {
	group := FutureGroup{mockFuture(666, nil), mockFuture(777, nil)}
	i, e := group.GetTimed(1 * time.Second)
	AssertThat(t, i, ElementsAre{777, 666})
	AssertThat(t, len(e), Is{V: 0})
}

func TestFutureGroup_WithFuturesWithTimeoutedResultsGetTimed_ReturnsTimeoutErrors(t *testing.T) {
	ms := time.Millisecond
	group := FutureGroup{mockFutureTimed(666, 5*ms), mockFutureTimed(777, 5*ms)}
	i, e := group.getTimed(func(time.Time) time.Duration { return 5 * ms }, 8*ms)
	AssertThat(t, i, AnyOf{ElementsAre{666}, ElementsAre{777}})
	AssertThat(t, e, ElementsAre{TypeOf{V: TimeoutError("")}})
}

func TestFutureGroup_WithFuturesWithErrorsGet_ReturnsSliceOfErrors(t *testing.T) {
	group := FutureGroup{mockFuture(nil, Error("1")), mockFuture(nil, Error("2"))}
	i, e := group.Get()
	AssertThat(t, len(i), Is{V: 0})
	AssertThat(t, e, ElementsAre{Error("1"), Error("2")})
}
