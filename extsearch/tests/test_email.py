from extsearch.audio.deepdive.graphs.operations import email


def test_compose_message():
    text = ['hello', 'world']
    return list(email.compose_message.function(text, 'workflow_url', 'file', 'wiki_page', 'contacts'))
