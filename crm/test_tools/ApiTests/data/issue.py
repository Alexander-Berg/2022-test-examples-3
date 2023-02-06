import requests
import json
import data.properties
#import pytest

host = data.properties.host
headers = data.properties.headers

createIssueTicketJson = {"issue_type_id": 3,"name": "test","text": "Ticket from API","account":{"Id":"10294557"},"owner":{"Login": "agroroza"},"queue_id": "20001"}
patchIssueTicketJson = {"name": "test patched","text": "Ticket from API patched","account":{"Id":"9955466"},"owner":{"Login": "robot-crmcrown"},"queue_id": "20002"}
createIssueTicketWithAttributesJson = {"followers": [{"Login": "robot-crmcrown"}],"issue_type_id": 3,"workflow_id": 14,"name": "Name API","account": {"Id": 88098607},"text": "Text API","owner": {"Login": "agroroza"},"deadline_dt": "2021-05-31T11:26:51+03:00","parent_issue": {"Id": 12827151},"business_unit_id": 7,"ya_service_id": 7,"category_id": 13652170,"standard_hours": 5,"spent_hours": 5,"start_before_dt": "2021-05-31T11:26:51.892Z","complexity": 1,"queue_id": 20001,"inn": "615421267330","contracteid": "ОФ-605805","communication_type_id": 1}
createIssueOpportunityJson = {"issue_type_id": 6,"name": "Opportunity","text": "Opportunity from API","account":{"Id":"10294598"},"owner":{"Login": "agroroza"}, "opportunity_source_id": "1","workflow_id": 16}
patchIssueOpportunityJson = {"name": "Opportunity patched","text": "Opportunity from API patched","account":{"Id":"9955466"},"owner":{"Login": "robot-crmcrown"},"opportunity_source_id": "2"}
addProductJson = {"issue_id": 0,"product_id": 179,"product_amount": 10000,"product_planned_date": "2020-10-10T10:00:00","product_fact_date": "2021-10-10T11:00:00","product_period_id": 1,"product_status": 3,"product_type_id": 4,"comment": "Product from API"}
putProductOpportunityJson = {"issue_id": 0,"product_id": 181,"product_amount": 15000,"product_planned_date": "2020-10-10T10:00:00","product_fact_date": "2021-10-10T11:00:00","product_period_id": 1,"product_status": 3,"product_type_id": 4,"comment": "Overwrited product from API"}
createIssueTaskJson = {"issue_type_id": 1,"name": "Test task","text": "Task from API","account":{"Id":"10294557"},"owner":{"Login": "agroroza"}}
patchIssueTaskJson = {"name": "Test task patched","text": "Task from API patched","account":{"Id":"9955466"},"owner":{"Login": "robot-crmcrown"}}
changeIssueWorkflowJson = {"workflow_id": "10"}
patchIssueTransitionsJson = {"state_id":"4_3"}
postIssueFollowersJson = {"followers": [{"Login": "robot-crmcrown"}]}
patchIssueFollowersJson = {"followers": [{"Login": "zomb-crmtest"},{"Login": "robot-crmlead"}]}
deleteIssueFollowersJson = {"followers": [{"Login": "zomb-crmtest"}]}
postTagJson = {"tag_ids": [36181]}
patchTagJson = {"tag_ids": [36183,36184]}
deleteTagJson = {"tag_ids": [36184]}
postCommentJson = {"text": "Test comment from API"}
patchCommentJson = {"text": "Overwritten comment from API"}


def test_creatingIssue():
    print('Try to create new issue')
    newIssue = requests.post(host, headers=headers, verify=False, json=createIssueTicketJson)
    newIssueJson = newIssue.json()
    response = newIssue.status_code
    assert response == 200
    issueId = str(newIssueJson['id'])
    print('Issue created, id: '+issueId)
    return newIssueJson['id']

def test_getCreatedIssue(issueId):
    print('Try to get issue')
    issueId = str(issueId)
    url = host + issueId
    issueJson = requests.get(url, headers=headers, verify=False)
    response = issueJson.status_code
    assert response == 200
    issueJson = issueJson.json()
    issueId = str(issueJson['id'])
    print('Issue got, id :'+issueId)
    return issueJson

def test_deleteCreatedIssue(issueId):
    issueId = str(issueId)
    print('Try ro delete issue, id '+issueId)
    url = host + issueId
    deleteIssue = requests.delete(url, headers=headers, verify=False)
    response = deleteIssue.status_code
    assert response == 200
    deleteIssue = deleteIssue.json
    print('Issue deleted successfully, id: '+issueId)
    return deleteIssue

def test_getDeletedIssue(issueId):
    print('Try to get deleted issue')
    issueId = str(issueId)
    url = host + issueId
    issueJson = requests.get(url, headers=headers, verify=False)
    response = issueJson.status_code
    assert response == 400
    issueJson = issueJson.json()
    print('Issue got, id :'+issueId)
    return issueJson

def test_patchIssue(issueId):
    issueId = str(issueId)
    print('Try to patch issue, id '+issueId)
    url = host + issueId
    patchIssue = requests.patch(url, headers=headers, verify=False, json=patchIssueTicketJson)
    response = patchIssue.status_code
    assert response == 200
    patchIssue = patchIssue.json()
    print('Issue patched successfully, id '+issueId)
    return patchIssue

def test_issueTransitions(issueId):
    issueId = str(issueId)
    print('Try to get transitions for issue, id '+issueId)
    url = host + issueId + '/transitions'
    issueTransitions = requests.get(url, headers=headers, verify=False)
    response = issueTransitions.status_code
    assert response == 200
    issueTransitions = issueTransitions.json()
    print('Transitions got successfully for issue '+issueId)
    return issueTransitions

def test_changeIssueState(issueId):
    issueId = str(issueId)
    print('Try to change state for issue, id '+issueId)
    url = host + issueId + '/transition/execute'
    issueState = requests.post(url, headers=headers, verify=False, json=patchIssueTransitionsJson)
    response = issueState.status_code
    assert response == 200
    issueState = issueState.json()
    print('State changed successfully for issue '+issueId)
    return issueState


def test_postIssueTicketWithAttributes():
    print('Try to create new issue with all attributes')
    newIssue = requests.post(host, headers=headers, verify=False, json=createIssueTicketWithAttributesJson)
    newIssueJson = newIssue.json()
    response = newIssue.status_code
    assert response == 200
    issueId = str(newIssueJson['id'])
    print('Issue created, id: '+issueId)
    return newIssueJson['id']


def test_issueWorkflows(issueId):
    issueId = str(issueId)
    print('Try to get workflows for issue, id '+issueId)
    url = host + issueId + '/workflows'
    issueWorkflows = requests.get(url, headers=headers, verify=False)
    issueWorkflows = issueWorkflows.json()
    print('Workflows got successfully for issue '+issueId)
    return issueWorkflows

def test_changeIssueWorkflow(issueId):
    issueId = str(issueId)
    print('Trying to change workflows for issue, id: '+issueId)
    url = host + issueId + '/workflow'
    issueWorkflows = requests.post(url, headers=headers, verify=False, json=changeIssueWorkflowJson)
    issueWorkflows = issueWorkflows.json()
    print('Workflow changed successfully for issue '+issueId)
    return issueWorkflows

def test_addIssueFollowers(issueId):
    issueId = str(issueId)
    print('Trying to add followers for issue, id: '+issueId)
    url = host + issueId + '/followers'
    issueFollowers = requests.post(url, headers=headers, verify=False, json=postIssueFollowersJson)
    issueFollowers = issueFollowers.json()
    print('Follower robot-crmcrown added successfully for issue '+issueId)
    return issueFollowers

def test_patchIssueFollowers(issueId):
    issueId = str(issueId)
    print('Trying to overwrite issue followers for issue '+issueId)
    url = host + issueId + '/followers'
    newIssueFollowers = requests.patch(url, headers=headers, verify=False, json=patchIssueFollowersJson)
    newIssueFollowers = newIssueFollowers.json()
    print('Overwriting followers successful for issue '+issueId)
    return newIssueFollowers

def test_deleteIssueFolowers(issueId):
    issueId = str(issueId)
    print('Trying to delete follower from issue '+issueId)
    url = host + issueId + '/followers'
    deletedFollowers = requests.delete(url, headers=headers, verify=False, json=deleteIssueFollowersJson)
    deletedFollowers = deletedFollowers.json()
    print('Follower deleted successfully for issue '+issueId)
    return deletedFollowers


def test_addTag(issueId):
    issueId = str(issueId)
    print('Trying to add new tag for issue '+issueId)
    url = host + issueId + '/tags'
    addTag = requests.post(url, headers=headers, verify=False, json=postTagJson)
    addTag = addTag.json()
    print('Tag added successfully for issue ' +issueId)
    return addTag


def test_patchTags(issueId):
    issueId = str(issueId)
    print('Trying to overwrite tags for issue '+issueId)
    url = host + issueId + '/tags'
    overwriteTags = requests.patch(url,headers=headers, verify=False, json=patchTagJson)
    overwriteTags = overwriteTags.json()
    print('Tags overwrited successfully from issue ' +issueId)
    return overwriteTags

def test_deleteTag(issueId):
    issueId = str(issueId)
    print('Trying to delete tag for issue '+issueId)
    url = host + issueId + '/tags'
    deleteTag = requests.patch(url,headers=headers, verify=False, json=deleteTagJson)
    deleteTag = deleteTag.json()
    print('Tag deleted successfully for issue ' +issueId)
    return deleteTag


def test_postComment(issueId):
    issueId = str(issueId)
    print('Trying to add new comment for issue '+issueId)
    url = host + issueId + '/comment'
    addComment = requests.post(url,headers=headers, verify=False, json=postCommentJson)
    addComment = addComment.json()
    print('Comment added successfully for issue ' +issueId)
    return addComment


def test_getComment(issueId, commentId):
    issueId = str(issueId)
    commentId = str(commentId)
    print('Trying to get comment from issue '+issueId +' and comment ' +commentId)
    url = host + issueId + '/comment/' + commentId
    getComment = requests.get(url, headers=headers,verify=False)
    getComment = getComment.json()
    print('Comment got successfully from issue ' +issueId +' and comment ' +commentId)
    return getComment


def test_patchComment(issueId, commentId):
    issueId = str(issueId)
    commentId = str(commentId)
    print('Trying to overwrite comment for issue '+issueId +' and comment ' +commentId)
    url = host + issueId + '/comment/' + commentId
    overwriteComment = requests.patch(url, headers=headers,verify=False,json=patchCommentJson)
    overwriteComment = overwriteComment.json()
    print('Comment overwritten successfully for issue ' + issueId + ' and comment ' + commentId)
    return overwriteComment


def test_deleteComment(issueId, commentId):
    issueId = str(issueId)
    commentId = str(commentId)
    print('Trying to delete comment from issue '+issueId +' and comment ' +commentId)
    url = host + issueId + '/comment/' + commentId
    deleteComment = requests.delete(url,headers=headers, verify=False)
    deleteComment = deleteComment.json()
    print('Comment deleted successfully from issue ' + issueId + ' and comment ' + commentId)
    return deleteComment


def test_creatingIssueOpportunity():
    print('Try to create new opportunity')
    newIssue = requests.post(host, headers=headers, verify=False, json=createIssueOpportunityJson)
    newIssueJson = newIssue.json()
    issueId = str(newIssueJson['id'])
    print('Opportunity created, id: '+issueId)
    return newIssueJson['id']


def test_getCreatedIssueOpportunity(issueId):
    print('Try to get opportunity')
    issueId = str(issueId)
    url = host + issueId
    issueJson = requests.get(url, headers=headers, verify=False)
    issueJson = issueJson.json()
    issueId = str(issueJson['id'])
    print('Opportunity got, id :'+issueId)
    return issueJson

def test_deleteCreatedIssueOpportunity(issueId):
    issueId = str(issueId)
    print('Try ro delete opportunity, id '+issueId)
    url = host + issueId
    deleteIssue = requests.delete(url, headers=headers, verify=False)
    deleteIssue = deleteIssue.json
    print('Opportunity deleted successfully, id: '+issueId)
    return deleteIssue

def test_patchIssueOpportunity(issueId):
    issueId = str(issueId)
    print('Try to patch opportunity, id '+issueId)
    url = host + issueId
    patchIssue = requests.patch(url, headers=headers, verify=False, json=patchIssueOpportunityJson)
    patchIssue = patchIssue.json()
    print('Opportunity patched successfully, id '+issueId)
    return patchIssue


def test_creatingIssueTask():
    print('Try to create new task')
    newIssue = requests.post(host, headers=headers, verify=False, json=createIssueTaskJson)
    newIssueJson = newIssue.json()
    issueId = str(newIssueJson['id'])
    print('Task created, id: '+issueId)
    return newIssueJson['id']


def test_getCreatedIssueTask(issueId):
    print('Try to get task')
    issueId = str(issueId)
    url = host + issueId
    issueJson = requests.get(url, headers=headers, verify=False)
    issueJson = issueJson.json()
    issueId = str(issueJson['id'])
    print('Task got, id :'+issueId)
    return issueJson

def test_deleteCreatedIssueTask(issueId):
    issueId = str(issueId)
    print('Try ro delete task, id '+issueId)
    url = host + issueId
    deleteIssue = requests.delete(url, headers=headers, verify=False)
    deleteIssue = deleteIssue.json
    print('Task deleted successfully, id: '+issueId)
    return deleteIssue

def test_patchIssueTask(issueId):
    issueId = str(issueId)
    print('Try to patch task, id '+issueId)
    url = host + issueId
    patchIssue = requests.patch(url, headers=headers, verify=False, json=patchIssueTaskJson)
    patchIssue = patchIssue.json()
    print('Task patched successfully, id '+issueId)
    return patchIssue

def test_getProductOpportunity(issueId):
    issueId = str(issueId)
    print('Trying to get opportunity product for issue id: '+issueId)
    url = host + issueId + '/product'
    getProduct = requests.get(url, headers=headers, verify=False)
    response = getProduct.status_code
    assert response == 200
    getProduct = getProduct.json()
    print('Product got successfully for issue id: '+issueId)
    return getProduct

def test_addProduct(issueId):
    issueId = str(issueId)
    print('Trying to add product to opportunity id: '+issueId)
    url = host +'product'
    addProductJson['issue_id'] = issueId
    addProduct = requests.post(url, headers=headers, verify=False, json=addProductJson)
    response = addProduct.status_code
    assert response == 200
    addProduct = addProduct.json()
    print('Product add successfully for issue id ' +issueId)
    return addProduct


def test_deleteProduct(productId,issueId):
    productId = str(productId)
    issueId = str(issueId)
    print('Trying to deleting product ' +productId +' from opportunity id '+ issueId)
    url = host + 'product/' + productId
    deleteProduct = requests.delete(url, headers=headers, verify=False)
    response = deleteProduct.status_code
    assert response == 200
    print('Product deleted successfully')


def test_putProduct(productId,issueId):
    productId = str(productId)
    issueId = str(issueId)
    putProductOpportunityJson['IssueId'] = issueId
    print('Trying to overwrite product ' +productId +' in opportunity id '+ issueId)
    url = host + 'product/' + productId
    putProduct = requests.put(url, headers=headers, verify=False, json=putProductOpportunityJson)
    response = putProduct.status_code
    assert response == 200
    putProduct = putProduct.json()
    print('Product overwrited successfully')
    return putProduct