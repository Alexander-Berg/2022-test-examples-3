import logging
import startrek_client as st


logging.getLogger("startrek_client").setLevel(logging.WARNING)
logging.getLogger("requests").setLevel(logging.WARNING)


def connect(user_agent, base_url, token):
    return st.Startrek(useragent=user_agent, base_url=base_url, token=token)
