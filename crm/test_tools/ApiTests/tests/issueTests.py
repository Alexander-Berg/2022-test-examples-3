import requests
import json
import data.issue
import data.properties
import time
#import pytest


def test_createNewIssueTicket():
    newIssue = data.issue.test_creatingIssue()


def test_getNewIssueTicket():
    start_time = time.time()
    newIssue = data.issue.test_creatingIssue()
    issueJson = data.issue.test_getCreatedIssue(newIssue)
    issueId = issueJson['id']
    issueType = issueJson['type']['id']
    assert issueId == newIssue
    assert issueType == 3
    print("--- %s seconds ---" % (time.time() - start_time))


def test_deleteIssueTicket():
    newIssue = data.issue.test_creatingIssue()
    deleteIssue = data.issue.test_deleteCreatedIssue(newIssue)
    getDeletedIssue = data.issue.test_getDeletedIssue(newIssue)
    message = getDeletedIssue['Message']
    assert message == 'Выбранная задача удалена'


def test_patchIssueTicket():
    newIssue = data.issue.test_creatingIssue()
    patchIssue = data.issue.test_patchIssue(newIssue)
    name = patchIssue['name']
    text = patchIssue['text']
    owner = patchIssue['owner']['Login']
    assert name == 'test patched'
    assert text == 'Ticket from API patched'
    assert owner == 'robot-crmcrown'

def test_getIssueTicketTransitions():
    newIssue = data.issue.test_creatingIssue()
    getIssueTransitions = data.issue.test_issueTransitions(newIssue)
    currentState = getIssueTransitions['current']['id']
    transitionsInProcess = getIssueTransitions['transitions'][0]['id']
    transitionsClose = getIssueTransitions['transitions'][1]['id']
    assert currentState == '2'
    assert transitionsInProcess == '3'
    assert transitionsClose == '4_1'


def test_createIssueTicketWithAllAttributes():
    newIssue = data.issue.test_postIssueTicketWithAttributes()
    getIssue = data.issue.test_getCreatedIssue(newIssue)
    issueId = getIssue['id']
    issueState = getIssue['state']['id']
    followers = getIssue['followers'][0]['Login']
    issueType = getIssue['type']['id']
    workflowId = getIssue['workflow_id']
    issueName = getIssue['name']
    issueAccount = getIssue['account']['Id']
    issueText = getIssue['text']
    owner = getIssue['owner']['Login']
    deadline_dt = getIssue['deadline_dt']
    parentIssue = getIssue['parent_issue']['Id']
    businessUnitId = getIssue['business_unit_id']
    yaServiceId = getIssue['ya_service_id']
    categoryId = getIssue['category_id']
    standardHours = getIssue['standard_hours']
    spentHours = getIssue['spent_hours']
    startBeforeDt = getIssue['start_before_dt']
    complexity = getIssue['complexity']
    queueId = getIssue['queue_id']
    # contracteid = getIssue['contracteid'] Do not work
    # inn = getIssue['inn'] Do not work
    communicationTypeId = getIssue['communication_type_id']
    assert issueId == newIssue
    assert issueState == '2'
    assert followers == 'robot-crmcrown'
    assert issueType == 3
    assert workflowId == 14
    assert issueName == 'Name API'
    assert issueAccount == 88098607
    assert issueText == 'Text API'
    assert owner == 'agroroza'
    assert deadline_dt == '2021-05-31T00:00:00'
    assert parentIssue == 12827151
    assert businessUnitId == 7
    assert yaServiceId == 7
    assert categoryId == 13652170
    assert standardHours == 5.0
    assert spentHours == 5.0
    assert startBeforeDt == '2021-05-31T11:26:51+03:00'
    assert complexity == 1.0
    assert queueId == 20001
    assert communicationTypeId == 1


def test_changeIssueTicketState():
    newIssue = data.issue.test_creatingIssue()
    changeIssueState = data.issue.test_changeIssueState(newIssue)
    currentState = changeIssueState['state']['id']
    assert currentState == '4_3'


def test_getIssueTicketWorkflows():
    newIssue = data.issue.test_creatingIssue()
    getIssueWorkflows = data.issue.test_issueWorkflows(newIssue)
    #print(getIssueWorkflows)


def test_postIssueTicketFollowers():
    newIssue = data.issue.test_creatingIssue()
    addedFollowers = data.issue.test_addIssueFollowers(newIssue)
    issueFollowers = addedFollowers['followers'][0]['Login']
    assert issueFollowers == 'robot-crmcrown'

def test_patchIssueTicketFollowers():
    newIssue = data.issue.test_creatingIssue()
    newFollowers = data.issue.test_patchIssueFollowers(newIssue)
    issueFollowers = newFollowers['followers']
    firstFollower = issueFollowers[0]['Login']
    secondFollower = issueFollowers[1]['Login']
    issueFollowers = [firstFollower,secondFollower]
    assert 'robot-crmlead' in issueFollowers
    assert 'zomb-crmtest' in issueFollowers


def test_deleteIssueTicketFollowers():
    newIssue = data.issue.test_creatingIssue()
    addFollowers = data.issue.test_patchIssueFollowers(newIssue)
    deletedFollowers = data.issue.test_deleteIssueFolowers(newIssue)
    firstFollower = deletedFollowers['followers'][0]['Login']
    assert firstFollower == 'robot-crmlead'
    assert len(deletedFollowers['followers']) == 1


def test_addIssueTicketTag():
    newIssue = data.issue.test_creatingIssue()
    addTag = data.issue.test_addTag(newIssue)
    tagId = addTag['tag_ids'][0]
    assert tagId == 36181


def test_overwriteIssueTicketTags():
    newIssue = data.issue.test_creatingIssue()
    addTag = data.issue.test_addTag(newIssue)
    overwriteTags = data.issue.test_patchTags(newIssue)
    tags = overwriteTags['tag_ids']
    # Обожемой, чому я такой? Переделать нормально
    tag1 = tags.index(36183)
    tag1 = tags[tag1]
    tag2 = tags.index(36184)
    tag2 = tags[tag2]
    assert tag1 == 36183
    assert tag2 == 36184


def test_deleteIssueTicketTag():
    newIssue = data.issue.test_creatingIssue()
    overwriteTags = data.issue.test_patchTags(newIssue)
    deleteTag = data.issue.test_deleteTag(newIssue)
    tags = deleteTag['tag_ids'][0]
    assert len(deleteTag['tag_ids']) == 1
    assert tags == 36184


def test_newIssueTicketComment():
    newIssue = data.issue.test_creatingIssue()
    newComment = data.issue.test_postComment(newIssue)
    textComment = newComment['text']
    authorLogin = newComment['author']['Login']
    assert textComment == 'Test comment from API'
    assert authorLogin == 'agroroza'


def test_getIssueTicketComment():
    newIssue = data.issue.test_creatingIssue()
    newComment = data.issue.test_postComment(newIssue)
    commentId = newComment['id']
    getComment = data.issue.test_getComment(newIssue,commentId)
    textComment = getComment['text']
    authorLogin = getComment['author']['Login']
    assert textComment == 'Test comment from API'
    assert authorLogin == 'agroroza'


def test_updateIssueTicketComment():
    newIssue = data.issue.test_creatingIssue()
    newComment = data.issue.test_postComment(newIssue)
    commentId = newComment['id']
    # dateCommentOriginal = newComment['date']
    modifiedDateOriginal = newComment['modified_on']
    updateComment = data.issue.test_patchComment(newIssue, commentId)
    # dateCommentUpdated = updateComment['date']
    modifiedDateUpdated = updateComment['modified_on']
    textComment = updateComment['text']
    assert textComment == 'Overwritten comment from API'
    assert len(updateComment['invites']) == 0
    assert modifiedDateOriginal != modifiedDateUpdated


def test_deleteIssueTicketComment():
    newIssue = data.issue.test_creatingIssue()
    newComment = data.issue.test_postComment(newIssue)
    commentId = newComment['id']
    deleteComment = data.issue.test_deleteComment(newIssue, commentId)
    deletingState = deleteComment['Success']
    assert deletingState


def test_createNewIssueOpportunity():
    newIssue = data.issue.test_creatingIssueOpportunity()


def test_getNewIssueOpportunity():
    newIssue = data.issue.test_creatingIssueOpportunity()
    issueJson = data.issue.test_getCreatedIssue(newIssue)
    issueId = issueJson['id']
    issueType = issueJson['type']['id']
    assert issueId == newIssue
    assert issueType == 6

def test_deleteIssueOpportunity():
    newIssue = data.issue.test_creatingIssueOpportunity()
    deleteIssue = data.issue.test_deleteCreatedIssue(newIssue)
    print(deleteIssue)


def test_patchIssueOpportunity():
    newIssue = data.issue.test_creatingIssueOpportunity()
    patchIssue = data.issue.test_patchIssueOpportunity(newIssue)
    name = patchIssue['name']
    text = patchIssue['text']
    owner = patchIssue['owner']['Login']
    source = patchIssue['opportunity_source_id']
    assert name == 'Opportunity patched'
    assert text == 'Opportunity from API patched'
    assert source == 2
    assert owner == 'robot-crmcrown'


def test_createNewIssueTask():
    newIssue = data.issue.test_creatingIssueTask()


def test_getNewIssueTask():
    newIssue = data.issue.test_creatingIssueTask()
    issueJson = data.issue.test_getCreatedIssue(newIssue)
    issueId = issueJson['id']
    issueType = issueJson['type']['id']
    issueName = issueJson['name']
    issueWorkflow = issueJson['workflow_id']
    issueText = issueJson['text']
    issueOwner = issueJson['owner']['Login']
    issueAccount = issueJson['account']['Id']
    assert issueId == newIssue
    assert issueType == 1
    assert issueName == 'Test task'
    assert issueWorkflow == 1
    assert issueText == 'Task from API'
    assert issueOwner == 'agroroza'
    assert issueAccount == 10294557


def test_deleteIssueTask():
    newIssue = data.issue.test_creatingIssueTask()
    deleteIssue = data.issue.test_deleteCreatedIssue(newIssue)


def test_changeIssueTicketWorkflows():
    newIssue = data.issue.test_creatingIssueTask()
    newWorkflow = data.issue.test_changeIssueWorkflow(newIssue)
    currentWorkflow = data.issue.test_getCreatedIssue(newIssue)
    workflowId = currentWorkflow['workflow_id']
    taskId = currentWorkflow['id']
    assert workflowId == 10
    assert taskId == newIssue


def test_getProductOpportunity():
    newIssue = data.issue.test_creatingIssueOpportunity()
    addProduct = data.issue.test_addProduct(newIssue)
    productId = addProduct['product_id']
    issueId = addProduct['issue_id']
    productAmount = addProduct['product_amount']
    productTypeId = addProduct['product_type_id']
    productPeriodId = addProduct['product_period_id']
    productStatus = addProduct['product_status']
    productDeclinedReasonId = addProduct['product_declined_reason_id']
    comment = addProduct['comment']
    isDeleted = addProduct['is_deleted']
    createdBy = addProduct['created_by']
    assert productId == 179
    assert issueId == newIssue
    assert productAmount == 10000.0
    assert productTypeId == 4
    assert productPeriodId == 1
    assert productStatus == 3
    assert productDeclinedReasonId is None
    assert comment == 'Product from API'
    assert isDeleted is False
    assert createdBy == 1499


def test_addProductOpportunity():
    newIssue = data.issue.test_creatingIssueOpportunity()
    addProduct = data.issue.test_addProduct(newIssue)


def test_getProductOpportunityWithNoProducts():
    newIssue = data.issue.test_creatingIssueOpportunity()
    getProduct = data.issue.test_getProductOpportunity(newIssue)
    products = getProduct['products']
    assert len(products) == 0


def test_deleteProductOpportunity():
    newIssue = data.issue.test_creatingIssueOpportunity()
    addProduct = data.issue.test_addProduct(newIssue)
    productId = addProduct['id']
    deleteProduct = data.issue.test_deleteProduct(productId,newIssue)
    getProduct = data.issue.test_getProductOpportunity(newIssue)
    products = getProduct['products']
    assert len(products) == 0


def test_overwriteProductOpportunity():
    newIssue = data.issue.test_creatingIssueOpportunity()
    addProduct = data.issue.test_addProduct(newIssue)
    productId = addProduct['id']
    overwriteProduct = data.issue.test_putProduct(productId,newIssue)
    print(overwriteProduct)
    productId = overwriteProduct['product_id']
    productAmount = overwriteProduct['product_amount']
    comment = overwriteProduct['comment']
    #assert productId == 31 don't work - bug
    assert productAmount == 15000.0
    assert comment == 'Overwrited product from API'
