package taskrunner

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCheckDeps(t *testing.T) {
	specs := map[string]Spec{
		"a": Spec{},
		"b": Spec{},
		"c": Spec{
			Depends: []string{"a", "b"},
		},
		"d": Spec{
			Depends: []string{"a", "b", "c"},
		},
	}
	err := Check(specs)
	require.NoError(t, err)
}

func TestCheckRecursiveError(t *testing.T) {
	specs := map[string]Spec{
		"a": Spec{
			Depends: []string{"b"},
		},
		"b": Spec{
			Depends: []string{"a"},
		},
	}
	require.Error(t, Check(specs))
}

func TestCheckUnknownError(t *testing.T) {
	specs := map[string]Spec{
		"a": Spec{
			Depends: []string{"bad"},
		},
	}
	err := Check(specs)
	require.Error(t, err)
	require.Equal(t, "unknown task 'bad'", err.Error())
}

func TestRun(t *testing.T) {
	solve := func(runner RunnerFunc) (int, RunResult, error) {
		var a, b, c int
		specs := map[string]Spec{
			"a": Spec{
				Run: func() error {
					a = 1
					return nil
				},
			},
			"b": Spec{
				Run: func() error {
					b = 2
					return nil
				},
			},
			"c": Spec{
				Depends: []string{"a", "b"},
				Run: func() error {
					c = a + b
					return nil
				},
			},
		}
		res, err := runner(specs, RunOptions{})
		return c, res, err
	}
	runners := []RunnerFunc{
		Run,
		RunParallel,
	}
	for _, runner := range runners {
		value, _, err := solve(runner)
		require.NoError(t, err)
		require.Equal(t, 3, value)
	}
}

func TestRunError(t *testing.T) {
	type Value struct {
		a, b, c int
	}
	solve := func(runner RunnerFunc, options RunOptions, panicOnError bool) (Value, RunResult, error) {
		var value Value
		specs := map[string]Spec{
			"a": Spec{
				Run: func() error {
					value.a = 1
					return nil
				},
			},
			"b": Spec{
				Depends: []string{"a"},
				Run: func() error {
					if panicOnError {
						panic("panic in b")
					}
					return errors.New("error in b")
				},
			},
			"c": Spec{
				Depends: []string{"a", "b"},
				Run: func() error {
					if value.a != 0 && value.b != 0 {
						value.c = value.a + value.b
					} else {
						value.c = 42
					}
					return nil
				},
			},
		}
		res, err := runner(specs, options)
		return value, res, err
	}
	runners := []RunnerFunc{
		Run,
		RunParallel,
	}
	for _, runner := range runners {
		for _, panicOnError := range []bool{false, true} {
			value, res, err := solve(runner, RunOptions{}, panicOnError)
			require.Error(t, err)
			require.Equal(t, Value{1, 0, 42}, value)
			require.Len(t, res.Skipped, 0)
			require.Len(t, res.Failed, 1)
		}
	}
	for _, runner := range runners {
		for _, panicOnError := range []bool{false, true} {
			value, res, err := solve(runner, RunOptions{SkipOnError: true}, panicOnError)
			require.Error(t, err)
			require.Equal(t, Value{1, 0, 0}, value)
			require.Len(t, res.Skipped, 1)
			require.Len(t, res.Failed, 1)
		}
	}
}
