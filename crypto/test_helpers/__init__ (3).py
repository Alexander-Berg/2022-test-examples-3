from yql.client import profile


def add_ydb_token_to_yql(id, ydb_token):
    request = profile.YqlSetProfileRequest({})
    request.add_token(id, "kikimr", ydb_token)
    request.run()
