from decorator import decorator


# Test case decorator, usage:
#   from fan.testutils.debug import capture_and_print_sql_queries
#   @capture_and_print_sql_queries
#   def test_no_campaigns(org_id):
#       assert get_emails_sent_count_for_month(org_id) == 0
def capture_and_print_sql_queries(func):
    def wrapper(func, *args, **kwargs):
        from django.db import connection
        from django.test.utils import CaptureQueriesContext

        with CaptureQueriesContext(connection) as ctx:
            result = func(*args, **kwargs)
            print()
            for query in ctx.captured_queries:
                print(
                    (
                        "%s\t%s"
                        % (
                            query["time"],
                            query["sql"],
                        )
                    )
                )
        return result

    return decorator(wrapper, func)
