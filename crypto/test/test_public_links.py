from crypta.graph.publish.lib.link_publisher import MatchingPublicLinks
from crypta.graph.acl.matching_acl import MatchingAcl


def test_set_links(yt_client, config):
    base_path = config.publish_config.paths.directbytypes.base_path
    public_path = config.publish_config.paths.directbytypes.public_path
    acl = MatchingAcl()
    public_links = MatchingPublicLinks(yt_client, acl)
    public_links.publish_matching_links(base_path, public_path)

    public_links.publish_households_links(
        config.publish_config.paths.households.base_path, config.publish_config.paths.households.public_path
    )
