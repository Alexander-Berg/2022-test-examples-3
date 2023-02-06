#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import MnPlace, Offer, Outlet, Region, Shop
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']

        cls.index.regiontree += [
            Region(rid=100, children=[Region(rid=101)]),
            Region(rid=110, children=[Region(rid=111)]),
        ]
        cls.index.shops += [
            Shop(fesh=10, regions=[100, 110], name='Apple Store'),
        ]
        cls.index.outlets += [
            Outlet(fesh=10, region=100, point_type=Outlet.FOR_STORE, point_id=101),
        ]

        cls.disable_randx_randomize()
        cls.settings.set_default_reqid = False

        cls.reqwizard.on_request(text='abcd samsung abcd galaxy s10 8gb').respond(
            qtree="cHic7VttbBTHGZ53d30sw2GdQKhkG7cnt4IzLa2pZOdKlaZx-UG_Ekq_wv2oOBvXRmr54VNTsNTigyR1CWC-SttAGpQIcL6MAVsBYgyEilBIpL1KrZQqyo-0TX80bSJVqaoqEp2P3dmZ2bnbM5gKUluybnfmnbln3ved92vm8Jdx2p2baVyIspCzWtE85KFmtAR9Bi1PuyiDSDvKoVb0hYaVDavQA2gt6oVdgB4D9CSgE4DOAiJ_lwH5sM57FvA3MR_mkmF0OqfY2bXOg6yY05HmRCsRnbP3zweH5oaTsgHaxK0oDx1fciGDPNbdjHLQCqv2WhxP3xLMmhfSZpRHq63BkQIc7kwRepSb3zmLfMLinN0LG61-y7UGAJFv814AvEZDO6tU_EHphxt6KGDIBohnGRC_uSDEGw4xQV7FIIcUOupP4bBnIZs8Dtwln9biEkGOFOTPAf5WNT4L2LcOoy8D_o4GN9VT_H5x4yYFcMoA-O2WEG4wwgS4iwEOCHTIHTjoEEyGwYJ1eKRzdgY8e3034a7ATR4cDzbV4PxuwF_VlgIlsmPEIsCwiEtOuAgoxfHbHW1kFHhQ0qHfgaEkWI1CLve6HNFaCDCVAd-nYbKWtcp7zmKgQAF1HoegCHEcldXxMTIMPNLZPKc1_FvWyhAGdqAfCQw_ibMlH2eLiuCPDYIteRNbmhkAyEvf35qv8v37AH9N50FPp2TKLINchl3Bgp5OEwvuYoIhnbpkPEwak0XTPNfdnBmAhVbWzjW0TvGVYrbYG7C3hkzK3Ef3QOE34A4ODMAChPpXeMcAf1fjhuuP-scrj_hjHrQ0Q66GjTi4_43nrZAxYphp332R7TtBEjDpqZBJWSy6jKwCwqqQUQ4Fm0OFiyl3F1vG-U0rvJEUXolDL2TV451q-KYJw84typvEtHNPRTu3GOeA09HJOADFSD9grUWWvghDkdkbyFurrZ1-Aak2cgM3LH0foXQQ0NmDh0ciQs3w_Dyu4NCpmE8T_oOzBH6DgkPHco4_pt8EV6ewl1PG1ZWI61CEq6sGri4Drq76cJ0wiHtdkqF-1xawjJ7xAQ4r5hYJrHVMjGZYfXfSfuF-tlUD3Zxxr12zMwMDDtnaTs5urauFIlN2kVMYtqVdtNeexl1E7Oz9up0tdsZ9jcrXN86LGQm1ibN3Mc6SToOlLdawtIG09xpcYJcibhOsgWUClTkSClDF4yCCqiuSeN3iNIjqosXtdsvQJ4nBs3CvtozGSpkY0YlKubLVP-ufVKy3KTB9f-KOcFHaUNMCXwC2Qo1SN-Q7AGsU0h48f6VgDY-wiEoJW_1REk_xpwnxdIo9WZ5VeTh88sfZEwS9mDw5_qR_QrROiDFbmdVU3Qbxg8Rt3Pbez8k4M95vxvvNeL8b8n50F814v9vC-3FR-Q63278-t3iF96KD1-vezz9JjOiL5P8McUCnEr3fpb_8R_BaG2ta4UWbuz-VUrfki3BaJpDEETo1KpCGvjVYm4iwCIJNcYF4yWeOkX_iKYePcY95lHweJZ9HyOcR8vk0-XyaetJjsjfthY3QV6g6tTN44Nxvb2juJXieOndlS2W7onNW6LL6doIKpDKkAznCviRw3ePC8Z8O3Xllp3D8p1ioQNu2a730aSgpfKDgFR-ayqSIDz0aqBTb_XuceEDln6zskFQqsgUmhVLCAW2oSaMOCI2SKXWNYpoiE0yzphinniZNkedO1JSI-JbTFKfwliP5iYozjX7iZcDfrl7wRDP1zhuqd_4Yf0WH1CaJylRqjLwCtJkKfUGpsU0uNbaZS42GoCRWjZMUzZ1RtA-SorUnKNrrUQzdXkPR2mVFa582RRuUI99_WvHjLS6wUoLTG95xRfi8cIhJPR61-PlWQKJz-eM47JHcm5IdAMkOlsapapwqmckPF2BEJS8F5PfGyQMtjZ2xcX-hqC2botkwRVldhBT1hqKopNxDQhQT05nKnzHGNGqYnK0p35ko-f81Svb2G1LFSjnJzr4l7CwhNlWCeMWCdEZawUtBTdhiusBrPPBTJfmXg7La2SLfU6_L5u3l6UzsPzh-dB_gr2PuJ4SAS21JtQAlzSH0pkW0cRmX2oIF7HGickCpzehOUT-aEe_NF2_7FMVriEwi8babxNs-DeJ9BqTE3FA45XaoVj06wQzBNJghx3UZ2OfBHRhwF9D5vV_xW00yz-0Sv2AhuG4ncZ2OqF5ko7063z-MaWsC4ylenEOFx21398AuegCycoU3yPcOBYwCwNX2TmplKmHv_DJ-14gE5rOziz6dW5Zd1pKNCnJTj9DhOiP0YDMMxu8VsYsvLc0RvOu_AQN13YAJ5BBF5k4rVTJQ5OMUxh23LORzyKlbPsm2rWridP1iueHEacCwu4lg9Ns4UxJHw9QuJO03sKWbqkYtA3NktmBLtyl7W87Z0m04AelOOJgRWZ1-radaRsf1ZhRCvSGG6EDcEDnERHRLjE080mQDTMr-WWaIWLduie7ErLkuU-QUjtsEcmjnD9ZvipJVfQziCXkxwWcknWGu4SKNnWE2VT3DrPfo8qbuhqldz6uhZpjJ7HeyzM5Op8x-b1i8P5ogtBPidi2hNUltI5Ma6TQ4en80cvRbVUevdz9Su_uheJhwOwm17EhCfdvGPbGqwZg_TrPTyhZ_tDKUdBJybI-wjtpIkzn5lxUUDRRKvWiwlWbKCoV0kHzuilKowSSlpTdD1CSZtVa2Ss-PSjdKqt88CZJnJvZLCgoywWmBArYV7MMj5zrTZGiKZPMn_Ukx0RABxdsn5Xbp60_HgdBLLMH4ZPiG8bUWosSxXAeOyht7j42_V0MHJv3xBB146cwckwqQgSYVeDyuAoQyXjfSCMxupu9qXEjSnQJ7eOQCldSFKpKyq0hq_i0jqX-AJKnXAHdpkkpLi98uycmUtW57Z7-wyMo4k5h6mZQUMl1G92KlO2L8wwUYpqXa6P5WWKoKUljKrioLfsiSFvwO1DJPle2VnQmqOfanv6cMuklHmhbdr6smJTSVNNVlKyVNsjZe72vB2kSR-SjXyDQ5G56TI7xfAF6tp5pFxa0kJ5rFmolm0ZhoFutINHF9Aak_OjW8bECNgJR2B4i3SAEpba4T8t_knfUHzmHlpzMPrl-_PiGGHhv3BGBKbgL8IAdMu_UE4ROYNVc74YiqPfTAYUmMuOr5hnLwkM6kyWrfl1f7V5MdOeFPVsr-uD-RaEf2PHVVsiPSuOrLV8j0DXU_VrrjdkRhCDO16YzjpSrb_YnodijdYQarwpe_Q3Z4_7YMVoUBYDMmBz2XH9snWRVlpIkFW0KPp1CaPJ5CUMXjjYJCOKl5PBqW8Jho9H8QikzdrXF5PCsV0oh1-4Zu3fLsVzpTqKPlTT_VgY52bt7y4rc6CyPrlq_yWx3ZVKQzjTlUuGKH1q1_hTc-nXlQtR9IqVWam_cDKd9g9_y9_m65JGIqHZ9pjAw1ITclLRtYMuYQxTseqjqIDc-atevi13kMp6VgNfIfKkynMCa7qif4EYCsew15xoAp-Co-wqR-n2PqR_ojJgyGTGjCvL0uHXQKb8p18ld5vV8VG5kr4ZdtstQItQnxhsC9SlKzbqLUxCIb6SLJ2zb4KGbX9-d9yAVvvgsL3ru2-Z6mnnIln7WX_mhy8z06RWrB1Vdeubvp9Luv5rNoKV0Op3AzWFD87KX3Pt9Ufm3-3YJiPqY_tbDmzXHd1bNcmGfft6qbtzoZN2i1pNaUaJVpcabRQJsWrSFtmn0bEZLb2If4K12A9OpkHPmVfp30ijNp-ZUwi78S3v0XfsK8Gw,,"  # noqa
        )
        cls.reqwizard.on_request(text="ноутбук 15-rb012ur ноутбук hp amd 2j").respond(
            qtree='cHic7Vp9bBxHFZ-Z3Ttvx5do649gjrq1jqBeK1zZFlajEtnUVCi0tCQRQnAg0RxJsV2StEcrQirRaz6KGye1SaIA_eCPFLsOSi-Oiek5tR0X_ikpEnsSUosEDQWpKlAoILVCFWqZmd2dm519d-uPk0rAJ9m3u_Nm9je_9-a9N2-O3koT1hp7bQtqw2nSgRpQEqXQ9agL3ZSwkI3Yc5RGHegTsU2xzegL6E7Uj0cxegyjkxhNYTSHEfu8gJGDtydPE3oXdbtZrBsfbq0z58yX9pf2OWdL-53zSdwmR69TRkebEB-9_91uf3Cto_aqDrQB9z2LLWyjpCaZQmncgTc_RVywuSOYahIt4p0b0FZjaOFihkwUMmSskI2zoVC6MWuxb-JMpg3valZeFcUVSZLSQf_KmRZX2Gul7Mp05p0p-XRW9tmfNvrRHryXWDiPEZtmMo_pZzW6SGd3ErencJvHEREc4QBHk3U-R0wa4uXDvFuSNabqO_xPZ7dgxdPgXuRBOIXplyl_DZEQ6nLZjs6u-3NcVbi9iq6eOPHKM8SH4veC8PQKNfkSnn6Omp5-rqF-Swtv4XpBvjL6rT1IYn2G0K9FW5ek7v_evJ4CzKv_ngBHBODoxJXSvPrvgXj5lKCFNXpUHPOpuI6yh3L2ZKiQwWPBeV_7DYGSMJTEQzmD6RYNpbFt5_YATAOA6XzSh8nFIZwZgZO36kA3Uv5UIsVDYQ1xpNk6xix2L1ASXxvCfszFrq4e0jXIHGhVggPrhslD2D_mctw16EFv8ZAnKXtWfZ2kElbMjreQNiMd6-B3HB6_Y31Sa6wH7TyWjdVvEwwE8e84rMzvsTWUz-NmtPCtW5IvYvpFTXPWrt337cju3n23EkQsgILj_f78ZQ-Ihe2CBSmiq_FmKptWrMuUySeSNjMHDOshOcU3Cb1Xm2KTc56twFnnQulw6VG25KdL-dKwMl0KTPfP9f50wd7Q1N90nQ8or7ug72LaEJbT3dAZRswZjxhjGW4owvm4z0rDzBiZG8p9lILQK5iudFpSDU8TRQ1HCf2Sbmm-z1WoNwHqX58-LfMV2QcifMIlXMroJB_AVLZJas2hxxd-sUL3vlheNfcumfqRYeWHfKZGDLpTY6ohGJ34mxTOrgA4e6PJZwzoC3F3jAjuAGmdxUeiouX4SkwUipSNlSJl7noKAAYNFOduoOvCsqVhZyYgT3x5QE1vxxSDfjVGN1E_vyaLyburZN1PEvoVTedUgDvLAOfVjDsGKHt-qscfWekFafmMu0IUKUC7Smut86BlLpTky4QOavzYmjIn1ZgNBayTG32SQl0hqi65VIVkl74cCqHl0Lii5VAxccylaQhupLe2rffeM-x83mRZgpk23CxBMXzCDP8H9YrhH6ivoeHP1tGvaopNOM-xCXynNOQUnWnV9OOAUl8bXuOPHegHafRiXGg0IKdrcz0NNMPs5f4do2tLB_kCYX9ccKacvOQzsYkzxZmLGXOMfWWb7Dq2_fCHnHR-4sxLtY1oawMxBSvtigHMaEpHcr3wcR4VvQnrzSBLbwn0CZkZ9lCI3gwjH6nFjoswMMlGk7iFVIJJxZ3pGmCs0kfDqCJbx5DZCu9hNt8PhDCLTTZler_AFqzPIR_TZtZAyzMIR8iazMLtUZSjL2827ijzvutx56X55d9G--VilF9-d30lv1yEVvEfYb9cXPqmfqXZtO-bG5fpm4s18c2_NKxR6ZufNYRv5ttF5Olj-b75V5juiCjWoFpVAg2x1cdRlZrOJRVqfGO9U6nW3Q5U68qzgGp1hxNVa3VE1OpwRK3OhxDcywPqZNvz0fwoUyf78O3517XihGZEAfTQRiBQqQh1hhbYFmh9scm5qtjn1_xC9tzZHVHU8Kd4RN2e_4vU0GJHMb1N0y_OKaNhgKB3DH84DFQ-jb5uYZc4p1cuPkgxXO0UFYnLw-SI0MdJVR9HDLpHw7uO7f9HmJZ5SnCu9DBLBvbxbZMyh_oow6swBGR-D7sb0Qo9dIfwa0wrSJYzskMZc6LAvIMxVljwYmulzGF-ZZmDF_vd6-FA_8bFxt1QicBV0ohaTHkH0_tCNa0wC4cjaloBFYEDQAra41W1AHFdO5-moFhZNQczeIJXmMvbHJeHw_4WKZxzSEZOY8aI7ym_h-lWzVMaucA6gwrQAQK4PDTfG90adE66QHns8SHKn0Z6PRfunGnl87aL9pRZwy3UCeCMIJflWVd7lQq281C5fp3LQo7vRuH4WKPu-ZKUPVyE6zuA6Wb9WKCjsyukk6Dvmx-XDplLh5HF-tYL78dbVffXBbs_kB_3aAxV4af4jzI_0JEY8fm5PxQZGD8VDsIC_Ghl_cpe2rTNNMpcipXt58Va1p5GMP1MOGgq9Cw9amIRNdGSo6anseMAoqy6jYAQPSGPVDFgzrjvJhdRyJqvpjirZJFjhQxS8kcV1qo9V7fn0G5lSRZuZn6meMhCLT3k425wUBkz2Qb8bCgLC1J26MhOf0ghDlnVx4VViWY9-F1FxWO4Gq3soVfNqnZu0vzfPt6M2_HV4833_3jTVcPq8Wb08abL1Orx5n_58aarpinfdaK9tySfxPRzesxkm-MLSXxdCqerRIR__k1W_USHKlGTNwNRkz-OipoMtGVbLC2exMoqhAK9M1fKRwT646NXlCHPwSenPuS58JEphzwnvNliIJuZ51Sex8MhipcnZp1zAabB3evEUZkDeH0q_3TPE9CxX029hkWgpzZlhD9ilLO0t0gNs7Rvh5P-rnDSH8yC3pDZBwZSIKMvJVIgrCZAHZUSIKh4OBhRPHz5Kvl-4DdgxC8eDgLbIPgnYFVSn0Cyk7ATTBc_VHRxyKihLo4CVcuBgYhl9PrNMhUcGKhcSWGNQCo4MBC5LbzsFGRm_qpsaX5Tyy3Nq4RmQ-fmU8z1i5PFQHEWOjZ_-6fKsbnSDdLa37F3bK7I6W7kBUwD7Wrx1fhx4ULGeJr_G-f_TrF_SkHWPXKdXFQRNlREXVIRdoml18vR3v6iOISXaukQLmH6-VCYOudMiZ_dVCuxfv-lNeUYJTpANvaAF6OEgG5cGeo1qHVjY4LnYmMLbNvwPPt-XklvsZq2skyobC0jqt4ve22zu0P4Gip-ndbwAQsnG614871Nu3pbEcr3tqF2jsaXIFJi212v9bT-oak9KMFSwIgx4rYlJa4s1PW2vrJtW09AwpISuLn54N29rcXbShvajPZvzj_Yq0vEmx9ouqGn9Xe3j28IjMGSDDnGn7YUe1rP7_rIRjbGz9_qZ2M0ijFoQ71lba2zcINxx-Yd7lPGiPY04TFjWmtzyL1ldKq3fEbKLX-1uGUc_wfQucls'  # noqa
        )
        cls.reqwizard.on_default_request().respond()

        cls.index.offers += [
            Offer(
                title="abcd samsung abcd galaxy s8 10gb",
                fesh=1,
                ts=1,
            ),
            Offer(title="xiaomi s10a 8 gb", ts=2, fesh=2),
            Offer(title="samsung galaxy s10 16gb", fesh=3, ts=3),
            Offer(title="samsung galaxy s10 8gb", fesh=4, ts=4, waremd5="EpnWVxDQxj4wg7vVI1ElnA"),
            Offer(title="samsung galaxy s8 10gb", fesh=5, ts=5, waremd5="xzFUFhFuAvI1sVcwDnxXPQ"),
            Offer(title="samsung a50", ts=6, fesh=6),
            Offer(title="samsung a60", ts=7, fesh=7),
            Offer(title="samsung a70", ts=8, fesh=8),
            Offer(title="samsung a80", ts=9, fesh=9),
            Offer(title="samsung a90", ts=10, fesh=10),
        ]

        # подготовка данных как в тесте lite/test_meta_rearrange.py
        # (чтобы проверять переранжирование на мете)
        for i in range(10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, i + 1).respond(1.0 - i * 0.1)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, i + 1).respond((i + 1) * 0.1)

    def test_multitoken_completely_match(self):
        def get_expected_answer(title, match_multitoken):
            return {
                'entity': 'offer',
                'titles': {'raw': title},
                'debug': {
                    'factors': {
                        'MULTITOKEN_COMPLETELY_MATCH_RATIO': str(match_multitoken)
                        if match_multitoken
                        else NoKey("MULTITOKEN_COMPLETELY_MATCH_RATIO")
                    }
                },
            }

        # Первые 5 офферов в итоге будут отсортированы при запросе numdoc=2 в обратном порядке: 5 4 3 2 1
        # проверяем что без флага эксперимента порядок не меняется
        response = self.report.request_json(
            'place=prime&text=abcd samsung abcd galaxy s10 8gb&rid=100&debug=da&numdoc=2'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        get_expected_answer("samsung galaxy s8 10gb", 0),
                        get_expected_answer("samsung galaxy s10 8gb", 1),
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=abcd samsung abcd galaxy s10 8gb&rid=100&debug=da&numdoc=2&page=2'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        get_expected_answer("samsung galaxy s10 16gb", 0.5),
                        get_expected_answer("xiaomi s10a 8 gb", 1),
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=abcd samsung abcd galaxy s10 8gb&rid=100&debug=da&numdoc=2&page=3'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        get_expected_answer("abcd samsung abcd galaxy s8 10gb", 0),
                        get_expected_answer("samsung a50", 0),
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # запрос без мультитокенов: ничего не должно меняться
        # результаты отличаются, так как этому запросу релевантны не все документы (ts=2 не релевантен, значит топ 2 - это 6 5)
        response = self.report.request_json('place=prime&text=samsung galaxy&rid=100&debug=da&numdoc=2&page=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        get_expected_answer("samsung a50", 1),
                        get_expected_answer("samsung galaxy s8 10gb", 1),
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
