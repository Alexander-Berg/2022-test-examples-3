// Code generated by some mysterious tool; DO NOT EDIT.
package nilness

func TriggersGen() bool {
	var test []int
	if test == nil {
		return true
	}
	return false
}

func NotTriggersGen() bool {
	var test []int
	return test == nil
}