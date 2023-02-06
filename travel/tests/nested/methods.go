package nested

import (
	"a.yandex-team.ru/travel/library/go/funcnames"
)

var (
	PrivateMethodName     funcnames.Caller
	PublicMethodName      funcnames.Caller
	PtrMethodName         funcnames.Caller
	FullPrivateMethodName funcnames.Caller
	FullPublicMethodName  funcnames.Caller
	FullPtrMethodName     funcnames.Caller
)

func init() {
	PrivateMethodName = funcnames.BuildName(TestedStruct.getPrivateFnName)
	PublicMethodName = funcnames.BuildName(TestedStruct.GetPublicFnName)
	PtrMethodName = funcnames.BuildName((&TestedStruct{}).GetPtrMethodName)
	FullPrivateMethodName = funcnames.BuildFullName(TestedStruct.getPrivateFnName)
	FullPublicMethodName = funcnames.BuildFullName(TestedStruct.GetPublicFnName)
	FullPtrMethodName = funcnames.BuildFullName((&TestedStruct{}).GetPtrMethodName)
}

type TestedStruct struct{}

// funcs return variables for checking initialization loop
func (s TestedStruct) getPrivateFnName() string  { return PrivateMethodName.String() }
func (s TestedStruct) GetPublicFnName() string   { return PublicMethodName.String() }
func (s *TestedStruct) GetPtrMethodName() string { return FullPtrMethodName.String() }
