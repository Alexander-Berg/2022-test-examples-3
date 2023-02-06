import unittest
from mock.mock import patch
from static import *
import logging

from server import full_withEnv
from server import load_config

log = logging.getLogger("MageUnitTest")
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(levelname)s %(message)s')

#conf = load_config("config_prod.yaml")


# TODO: We need use mongomock and test whole stack
class MailGenmapReclusterHandlerCheckReclusterFunction(unittest.TestCase):

#    @patch('generator.INUM_START', 525)
#    @patch('generator.INUM_END', 527)
    def test_genservice_maps(self):
        from libs.context.generator import Generator
        self.generator = Generator(load_config("./config/mail.yaml"))
        self.generator.INUM_START = 525
        self.generator.INUM_END = 527
        # from handlers.mail.MailGenmapReclusterHandler import MailGenmapReclusterHandler
        #from libs.context.mail import Mail
        from libs.context.generator import Generator
        from libs.context.tools import HashDict

        # from handlers.mail.static import MAIL_GROUPS, HashDict

        self.replicafactor = self.generator.REPLICAFACTOR

        self.curr_revision_list = curr_gencfg_inst_list + curr_gencfg_added_inst_list
        for el in curr_gencfg_removed_inst_list:
            self.curr_revision_list.remove(el)

        self.prev_revision_list = prev_searchmap_list

        self.current_result_searchmap, self.current_result_recluster_searchmap = self.generator.gen_maps(self.curr_revision_list,
                                                                                                    self.prev_revision_list,
                                                                                                    oauth_token=full_withEnv("%(_OAUTH_TOKEN_)s")
                                                                                                    )
        curr_set = set()
        for element in self.current_result_searchmap:
            element['_id'] = " "
            curr_set.add(HashDict(element))

        rec_set = set()
        for element in self.current_result_recluster_searchmap:
            element['_id'] = " "
            rec_set.add(HashDict(element))


        abook_curr_set_inum525 = set()
        for el2 in curr_set:
            if el2['service'] == "abook" and el2['iNum'] == "525":
                abook_curr_set_inum525.add(el2)

        # print "assert============"
        # for el in assert_abook_curr_set_inum525:
        #     print el['hostname'],el['shards'],el['zk'],el['zk_mtn'],el['group']
        #
        # print "current============"
        # for el2 in abook_curr_set_inum525:
        #     print el2['hostname'], el2['shards'], el2['zk'],el2['zk_mtn'], el2['group']
        #     # print ("asset", el)
        #     # for el2 in abook_curr_set_inum525:
        #     #     if el['hostname'] == el2['hostname'] & el['shards'] == el2['shards']:
        #     #         print("current", el2)
        #
        #print "===================="
        #for el in abook_curr_set_inum525:
        #    print ("current", el, el.__hash__())

        #print "assert", assert_abook_curr_set_inum525
        #print "=========="
        #print "curr", abook_curr_set_inum525
#
        print "==========="
        print "assert minus current", assert_abook_curr_set_inum525 - abook_curr_set_inum525
        print "current minus assert", abook_curr_set_inum525 - assert_abook_curr_set_inum525


        self.assertTrue(assert_abook_curr_set_inum525 == abook_curr_set_inum525)


if __name__ == "__main__":
    unittest.main()
