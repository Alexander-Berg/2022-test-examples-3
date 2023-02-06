from bot.utils import parse_hash


def extract_hash(url):
    return parse_hash(url)


def test_chat_regexp():
    assert extract_hash('tg://join?invite=EwesklUsMAK9-GlsLYSRYg') == 'EwesklUsMAK9-GlsLYSRYg'
    assert extract_hash('https://telegram.me/joinchat/BvmbJED8I3aGqqtk_ssAbg') == 'BvmbJED8I3aGqqtk_ssAbg'
    assert extract_hash('https://t.me/joinchat/9Db5hz9j9IFhZTdi') == '9Db5hz9j9IFhZTdi'
    assert extract_hash('https://t.me/joinchat/9Db5hz9j9IFhZTdi ') == '9Db5hz9j9IFhZTdi'
    assert extract_hash('https://t.me/joinchat/9Db5hz9j9IFhZTdi/ ') == '9Db5hz9j9IFhZTdi'
    assert extract_hash('https://t.me/+2vaBOsbHs-cxY2Y6') == '2vaBOsbHs-cxY2Y6'
    assert extract_hash('https://t.me/+2vaBOsbHs-cxY2Y6 ') == '2vaBOsbHs-cxY2Y6'
    assert extract_hash('https://t.me/+2vaBOsbHs-cxY2Y6/ ') == '2vaBOsbHs-cxY2Y6'


def test_warden_chats():
    urls = """https://t.me/joinchat/1cARNOzOD20wZGYy
                https://t.me/joinchat/2jyHrNZeByUzMzky
                https://t.me/joinchat/37NkrqN-R4VhZDAy
                https://t.me/joinchat/44QfFmcsg9k1NTIy
                https://t.me/joinchat/5GrDvXC2ELM3NGNi
                https://t.me/joinchat/6-Sl5J0OYW8zODJi
                https://t.me/joinchat/6ohvuhkbgxM2ZGEy
                https://t.me/joinchat/8pt3bcrEI_BmMTQy
                https://t.me/joinchat/9I_L6LfoJq42NmRi
                https://t.me/joinchat/A-4-iEMhmhgZMRV4IQOzlQ
                https://t.me/joinchat/A3MwBVFnRxu-OkLpzCKOGw
                https://t.me/joinchat/A3W8CRXtWc248d5kBsZJSA
                https://t.me/joinchat/A53Yz0RRIrb2vil03EymQw
                https://t.me/joinchat/A7R2dxhOB7TvPZrjFzsJXw
                https://t.me/joinchat/AAAAAAxmOS2XhnJ92M_Hiw
                https://t.me/joinchat/AAAAAEDje_JvGnMnVZxVvA
                https://t.me/joinchat/AAAAAEDjhLwxZPvwn29CIw
                https://t.me/joinchat/AAAAAEE8Y809gNu7_SNDSg
                https://t.me/joinchat/AAAAAEHVRedhLExLOG2krg
                https://t.me/joinchat/AAAAAEIr9yhRvMsJlRnsBA
                https://t.me/joinchat/AAArXhneqUsrg_MGdtmflA
                https://t.me/joinchat/AAYTqU_d3lVprRylyw_z5g
                https://t.me/joinchat/AAf0vUpdtQGaKjBxdJ6Bnw
                https://t.me/joinchat/AAiKM1DNF6sPcgCD3x9n_g
                https://t.me/joinchat/AAiKMz8nRz-hIw1ncjZwxw
                https://t.me/joinchat/ABHM7kuzJUgoihtK4r1n9g
                https://t.me/joinchat/ABc8KBVoHrkYeahFBPcjEw
                https://t.me/joinchat/ADn1Y064wKv0Bqip_e5MBQ
                https://t.me/joinchat/AUH5-AZMWlk4NzAy
                https://t.me/joinchat/AW7EULrg2A45ZWUy
                https://t.me/joinchat/AWy4SEtM8fcCVIFjPwd2fg
                https://t.me/joinchat/AgxQqk6RPltgm71neRaABg
                https://t.me/joinchat/Ah-T2Buy-aXxH4NGjX5U4A
                https://t.me/joinchat/AjkoqBdRreWWajPcmTi8qQ
                https://t.me/joinchat/AjkoqBle4UgMAb4trqrRIw
                https://t.me/joinchat/Am1N4FcxRjwz3y61zynSDw
                https://t.me/joinchat/AqgqJEnmMtiBfXmAeu5HlQ
                https://t.me/joinchat/ArGQhVHPeFd68QJupH8ztw
                https://t.me/joinchat/AufO4z8tmC0PtnS0KDUSxw
                https://t.me/joinchat/AvZhKBpV-WXCh5fjNTCkmw
                https://t.me/joinchat/B-N1EEzZGVGzBcHdTQ1eiw
                https://t.me/joinchat/B2BIbEDkZbsZlHrK5IB8Hw
                https://t.me/joinchat/B2BIbEt9dZuv8Mp8DvAf-w
                https://t.me/joinchat/B2BIbEtDu0s3m7k_9dH59A
                https://t.me/joinchat/B5CPo07Qed5wBolgq2jGWg
                https://t.me/joinchat/B5RVv075I5ee6wYw2yFB3w
                https://t.me/joinchat/B83snkpg9VLNLMQpUJ9sdw
                https://t.me/joinchat/BEdJXBlXz0dBmZGck_IiDQ
                https://t.me/joinchat/BFRY1hM0SrVZ4BcKZUYIhA
                https://t.me/joinchat/BFyJ6UbQI_Cz2wg7AQy8fQ
                https://t.me/joinchat/BIVemRkIZWijEItlv4KHJg
                https://t.me/joinchat/BLQp10XNMTrbGctZrctfGg
                https://t.me/joinchat/BLgtFhQ3fQOTDPxyIsxJMA
                https://t.me/joinchat/BLv7Gk_bz7AOS-kQaF1SZQ
                https://t.me/joinchat/BMYjdEHNRonoEgiQqcg68g
                https://t.me/joinchat/BNLvjhiE-eggWPZnG_TEDw
                https://t.me/joinchat/BQDHDRhhKK_7Em5aWp1nNg
                https://t.me/joinchat/BUyqs0Ki-gfkdBK19t7y9Q
                https://t.me/joinchat/BVdB5g9N7gsYpYIpgZO51w
                https://t.me/joinchat/BWamcUzgWx28lfCy-1pCDw
                https://t.me/joinchat/B_BNDg7bNV2B8A2KDUgrQw
                https://t.me/joinchat/B_BNDgv5jjPemyHjFbx2_g
                https://t.me/joinchat/B_BNDkZq3zjt-jbmM9ThMQ
                https://t.me/joinchat/BapqhVAjJ2Lt5mzhtfXXKQ
                https://t.me/joinchat/BapqhVAjJ2Lt5mzhtfXXKQ
                https://t.me/joinchat/BbHwRD-yeFhOUcVS1_fdsA
                https://t.me/joinchat/BbV9dVeKACEOGX362Sad0A
                https://t.me/joinchat/BdtakRQm0yD8dS3x0BGCcQ
                https://t.me/joinchat/BernrxPWPph03mBlMVOKBA
                https://t.me/joinchat/BfrKfUm4vsiHynwEIFszig
                https://t.me/joinchat/BgXpJ1VpwyBkRpCA4mrKxQ
                https://t.me/joinchat/Biaqg0J9ASqJ00G1aYqE6A
                https://t.me/joinchat/BlKgj0Is2sExYzRi
                https://t.me/joinchat/BlyRekEwILp56Vu030x2gg
                https://t.me/joinchat/Bmh_tUtHIdq1MqeBHPyiqQ
                https://t.me/joinchat/Bp3660Bouxm--_9lZ4oHAg
                https://t.me/joinchat/BtEOKEYWip5BKLwIEqCXAA
                https://t.me/joinchat/BuMzDk7LfPMKCTROQzjYTg
                https://t.me/joinchat/Buu2z0vpiwZjz64azhz91Q
                https://t.me/joinchat/Buu2zwwpEOpfK34VkI14AQ
                https://t.me/joinchat/Byx55RDJgNHt2BMSyHT-3w
                https://t.me/joinchat/C0sFQz7fxxGhS4U_MfqnTQ
                https://t.me/joinchat/C3Kiohi94Y8nfGl38BwDlQ
                https://t.me/joinchat/C3yasv_XvLBboWUj
                https://t.me/joinchat/C50Gxz7Gow-aN4NlwQc0rQ
                https://t.me/joinchat/C5yyIUy9ythghPB6TfsD7w
                https://t.me/joinchat/CAt9W0EA9cmQisyfId6GvA
                https://t.me/joinchat/CBsxYUp1hZtEqwMmPghksA
                https://t.me/joinchat/CCDG-0Dje_KWvyMQ-bFMtA
                https://t.me/joinchat/CDTEl07Qed6szYHRGUzBPw
                https://t.me/joinchat/CDeJuRpsOpow8T4kCzypkA
                https://t.me/joinchat/CEw0gUCImnDB_so-LR-GUw
                https://t.me/joinchat/CFakQVbsPaz6iDnGHu3iJg
                https://t.me/joinchat/CFhBr1jnnOA0iuGt_tT98g
                https://t.me/joinchat/CQ3QZQ9dSJhqnHg1_kGUlQ
                https://t.me/joinchat/CTrARBxsXrVGNRegv8jpUQ
                https://t.me/joinchat/CVZG0E5ZOoq8QzWRP6OucQ
                https://t.me/joinchat/CW352WKxudAyYjhi
                https://t.me/joinchat/CaUODkLvYBLWZGsTWQ90QQ
                https://t.me/joinchat/CaUODkNMIpwWNzLu4pngow
                https://t.me/joinchat/CbnMo0bMfQ53S3Ens9IhDg
                https://t.me/joinchat/CfwpTT8thXxCIp5TQi-CtQ
                https://t.me/joinchat/CpctSheAa8MQmDjAR--jBA
                https://t.me/joinchat/CrULOkRlaP4_S37z6ycnZQ
                https://t.me/joinchat/D11ImNT1RMD-QZSV
                https://t.me/joinchat/D9353xHzOWQ1R_cwSaOm6Q
                https://t.me/joinchat/DA7TAk8Nvnu-KEozlvQaXQ
                https://t.me/joinchat/DBm7fxTOQgGAYd-6ldQpcA
                https://t.me/joinchat/DCBSykgRgqKO9dUjuIl-fQ
                https://t.me/joinchat/DJeIIhtomy5MwZt4LkN83A
                https://t.me/joinchat/DJeIIkUY3MzGA0bVNYM37A
                https://t.me/joinchat/DJg5HxcOzdCsPlz00HcEIg
                https://t.me/joinchat/DQpHXUTZ9aLI5PmNAh-U5w
                https://t.me/joinchat/D_7u_1KGpaQeZmhGYETosw
                https://t.me/joinchat/Dd--B0fE9-g9XFl7BVMnLQ
                https://t.me/joinchat/Di0DqRQSTRl04tDOBnuMRg
                https://t.me/joinchat/Dk_2tk6yX2vCswhOy9VlZw
                https://t.me/joinchat/DuD8GBEO-rdW7ZNgMYMayQ
                https://t.me/joinchat/E-ZhJRCSZxcfynwwk8sHWQ
                https://t.me/joinchat/E1xvrTDVcA5_N_x0
                https://t.me/joinchat/EGt2oRj8q00eAX61UvnJ1w
                https://t.me/joinchat/EGt2oRzIJB0t-BCAaLJNaw
                https://t.me/joinchat/EOkaEx28p3lr2GOzs6oT9g
                https://t.me/joinchat/EnHrVRdZXZaS2nbAaOHIUg
                https://t.me/joinchat/EwesklUsMAK9-GlsLYSRYg
                https://t.me/joinchat/FEvIvDXFBug4MGM6
                https://t.me/joinchat/FQMLrxsDXVXKE1-EZq1EVg
                https://t.me/joinchat/FRyUIsFo2U05YTgy
                https://t.me/joinchat/FWBeJdOB2rV-KKlP
                https://t.me/joinchat/FY5v-9KrW-3tty9B
                https://t.me/joinchat/FYURbMD19KVeoAoz
                https://t.me/joinchat/FZeFhwT9aN0F0iu2
                https://t.me/joinchat/GPgCVS1P-6SPKouB
                https://t.me/joinchat/GVKxywLsv6ZkMzFi
                https://t.me/joinchat/GW6rb5vfK9CAe9TK
                https://t.me/joinchat/HDjTDhL68XfdQ6UWfchp9A
                https://t.me/joinchat/HGe5k27Mrmq8ALXG
                https://t.me/joinchat/HM4OUy_rku9mi2kk
                https://t.me/joinchat/IA7LHYVImN37EWSE
                https://t.me/joinchat/IwXXtIRGsMUwMDU6
                https://t.me/joinchat/J-4vteuwlstkMmYy
                https://t.me/joinchat/KKZv9q-98fs2NjIy
                https://t.me/joinchat/KQ4yjs1m0dBkMGEy
                https://t.me/joinchat/Lz2_8w1qRw7-PDRGLP3b3w
                https://t.me/joinchat/P3rKDryk6MnVqFmo
                https://t.me/joinchat/PLa49D8jh1tjOGEy
                https://t.me/joinchat/Pkmm8GcE3zCgjypk
                https://t.me/joinchat/Pt_HES4z2GQx-qdN
                https://t.me/joinchat/Q0hbtW7S5PBy5fN1
                https://t.me/joinchat/QGOkuYW0lBXn5tbp
                https://t.me/joinchat/QHACA56UUV8ufs4K
                https://t.me/joinchat/QJXdYCa2rEOr0nLg
                https://t.me/joinchat/QePbohc9R_kC_1sr
                https://t.me/joinchat/R4tKO7nVx1ehiP_q
                https://t.me/joinchat/R5U7NF_ZplVzjkAI
                https://t.me/joinchat/RDsN_ncAU5gwYmZi
                https://t.me/joinchat/R_is_tE_zxKxqnvd
                https://t.me/joinchat/S11irjnYmfwbTYj-
                https://t.me/joinchat/S7IZGlWR0IqNd0Ef
                https://t.me/joinchat/SHbL5ZFLyj_5hcqr
                https://t.me/joinchat/SUvk8H6nYXqSQkRi
                https://t.me/joinchat/Sj9jbjf2BrNVvwo7
                https://t.me/joinchat/Sl21Aavbpumzbihy
                https://t.me/joinchat/Sl21AfAqQOx0noGf
                https://t.me/joinchat/StYc0yt_TeIplF73
                https://t.me/joinchat/ThRKUdcY0SwNUIlh
                https://t.me/joinchat/TiZa7MpG5diAH27q
                https://t.me/joinchat/To0s7YLPwkw0MDhi
                https://t.me/joinchat/TrjAq4DRi9WpBQCa
                https://t.me/joinchat/TtrMJAgaoVniPmoj
                https://t.me/joinchat/Tvkjl1dGPPlCs_lj
                https://t.me/joinchat/Tvni3chiAmfIiRDI
                https://t.me/joinchat/U0aqYplq-tswU6Ry
                https://t.me/joinchat/UL-w9Xtl5TksTcm-
                https://t.me/joinchat/UQ8kaq4R8UWpXLru
                https://t.me/joinchat/Um0IX1trcFthNDBi
                https://t.me/joinchat/VDkGuD1zQfsTPVxb
                https://t.me/joinchat/VHqBaX5aHXaZ81qG
                https://t.me/joinchat/VU8HufznG6nJN6VJ
                https://t.me/joinchat/W9wqMfg2R6I0ZmQy
                https://t.me/joinchat/WVn0btDK9mFT0BCQ
                https://t.me/joinchat/XrVWU9UQVdQ1MWRi
                https://t.me/joinchat/Yi2ejdEBLJIyNTli
                https://t.me/joinchat/fFf1PB3VwUI2ZmIy
                https://t.me/joinchat/fX19FDA7hY05ZTgy
                https://t.me/joinchat/fo1FLF_3T9A2MmEy
                https://t.me/joinchat/grPyHd4nS3xlMTky
                https://t.me/joinchat/hNPX6AhL_nk5MDMy
                https://t.me/joinchat/kqDSZZsZk-EzZjQy
                https://t.me/joinchat/l11f8poBo_A1MTIy
                https://t.me/joinchat/lWuJ1QTDCVc2MGEy
                https://t.me/joinchat/p5JR3ZndGZY5NjQy
                https://t.me/joinchat/pt0-o9o_C-tkN2Ri
                https://t.me/joinchat/sJJKUjAFzopiMDcy
                https://t.me/joinchat/sqCUCJu-1pIxNjYy
                https://t.me/joinchat/utCHur_pc7xjYjdi
                https://t.me/joinchat/w3GxywHv0cxjOWRi
                https://t.me/joinchat/xBMrUzCFukoxNDVi
                https://t.me/joinchat/zqQb_tHR6x1lMDIy
                https://telegram.me/joinchat/BvmbJED8I3aGqqtk_ssAbg
                tg://join?invite=EwesklUsMAK9-GlsLYSRYg""" \
        .splitlines(keepends=False)
    for url in urls:
        assert extract_hash(url.strip())
