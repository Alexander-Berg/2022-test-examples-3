from transfer_manager.python.recipe.interface import TransferManagerTest
import yt.transfer_manager.client as tm_client


class TestWithTm(TransferManagerTest):
    def get_subdir_path(self, yt_subdir):
        return "//{}/{}".format(self.__class__.__name__, yt_subdir)

    def get_tm_env(self):
        return {
            "YT_TOKEN": "FAKE_FOR_TM",
            tm_client.YT_TRANSFER_MANAGER_URL_ENV: self.tm_client.backend_url,
        }
