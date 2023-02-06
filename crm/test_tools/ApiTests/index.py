import tests.issueTests as test
import tests.accountDomainTests

test.test_createNewIssue()
test.test_getNewIssue()
test.test_patchIssue()
tests.accountDomainTests.test_addDomainToAccount()
