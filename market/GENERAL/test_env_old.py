# coding: utf-8

from market.idx.datacamp.controllers.piper.yatf.resources.config import PiperConfig
from market.idx.datacamp.controllers.piper.yatf.test_env import PiperTestEnv as PiperTestEnvNew
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import MarketColor
from market.idx.datacamp.yatf.datacamp_env import DataCampTestEnv
from market.idx.pylibrary.trace_log.yatf.tracelog_parser import parse_tracelog
from market.idx.yatf.resources.yt_token_resource import YtTokenResource


class PiperTestEnv(DataCampTestEnv):

    def __init__(self, yt_stuff, log_broker_stuff, trace_log_path=None, update_only=False, **resources):
        super(PiperTestEnv, self).__init__(yt_stuff, **resources)

        self.trace_log_path = trace_log_path
        self.new_env = None
        self.new_config = None
        self.yt_server = yt_stuff
        self.log_broker_stuff = log_broker_stuff
        self.yt_token = YtTokenResource()
        self.update_only = update_only

    @property
    def description(self):
        return 'piper_env'

    def __enter__(self):
        super(PiperTestEnv, self).__enter__()
        color = MarketColor.Name(self.config.color).lower()
        self.new_config = PiperConfig(self.log_broker_stuff, self.yt_token.path, self.yt_server, color)
        # должно стоять, как можно раньше, что бы создать united_updater и добавлять себя в UpdateOnlySources
        if self.config.offer_bids_topic:
            self.new_config.create_offer_bids_pipeline(
                self.config,
                self.config.yt_offers_tablepath,
                self.config.offer_bids_topic,
                self.config.qoffers_topic,
                self.config.yt_basic_offers_tablepath,
                self.config.yt_service_offers_tablepath,
                self.config.yt_actual_service_offers_tablepath,
                self.config.yt_categories_tablepath,
                self.config.yt_partners_tablepath,
                fill_business_id=True,
                fill_warehouse_id=True
            )
        if self.config.use_quoter:
            self.new_config.create_quoter_pipelines(
                self.config,
                self.log_broker_stuff,
            )
            # для этого теста больше ничего
            self.new_env = PiperTestEnvNew(**{
                'piper_config': self.new_config,
                'yt_token': self.yt_token,
            })
            self.new_env.__enter__()
            return self
        if self.config.offers_topic:
            self.new_config.create_offers_pipeline(
                self.config,
                self.config.offers_topic,
            )
        if self.config.api_data_topic:
            self.new_config.create_api_data_pipeline(
                self.config,
                self.config.yt_offers_tablepath,
                self.config.api_data_topic,
                self.config.yt_basic_offers_tablepath,
                self.config.yt_service_offers_tablepath,
                self.config.yt_actual_service_offers_tablepath,
                self.config.yt_categories_tablepath,
            )
        if self.config.qoffers_topic:
            self.new_config.create_qoffers_pipeline(
                self.config,
                self.config.yt_offers_tablepath,
                self.config.qoffers_topic,
                self.config.yt_basic_offers_tablepath,
                self.config.yt_service_offers_tablepath,
                self.config.yt_actual_service_offers_tablepath,
                self.config.yt_categories_tablepath,
                self.config.yt_partners_tablepath,
            )
        if self.config.datacamp_messages_topic or self.config.blue_datacamp_messages_topic:
            self.new_config.create_united_offers_pipeline(
                self.config,
                self.config.datacamp_messages_topic,
                self.update_only
            )
            self.new_config.create_tech_commands_pipeline(
                self.config,
                self.config.yt_offers_tablepath,
                self.config.yt_partners_tablepath,
                self.config.datacamp_messages_topic,
                self.config.blue_datacamp_messages_topic,
                self.config.yt_basic_offers_tablepath,
                self.config.yt_service_offers_tablepath,
                self.config.yt_actual_service_offers_tablepath,
                self.config.yt_categories_tablepath,
            )
            self.new_config.create_categories_pipeline(
                self.config,
                self.config.yt_offers_tablepath,
                self.config.datacamp_messages_topic,
                self.config.yt_basic_offers_tablepath,
                self.config.yt_service_offers_tablepath,
                self.config.yt_actual_service_offers_tablepath,
                self.config.yt_categories_tablepath,
            )
        if self.config.picrobot_response_topic:
            self.new_config.create_picrobot_response_pipeline(
                self.config,
                self.config.picrobot_response_topic,
            )
        if self.config.picrobot_video_response_topic:
            self.new_config.create_picrobot_video_response_pipeline(
                self.config,
                self.config.picrobot_video_response_topic,
            )
        if self.config.external_in_message_topic:
            self.new_config.create_rtyhub_in_message_topic_pipeline(
                self.config,
                self.config.external_in_message_topic,
                "EXTERNAL_DATACAMP_MESSAGE_UNPACKER",
                is_vertical=False,
                partners_table=self.config.yt_partners_tablepath
            )
        if self.config.vertical_external_in_message_topic:
            self.new_config.create_rtyhub_in_message_topic_pipeline(
                self.config,
                self.config.vertical_external_in_message_topic,
                "VERTICAL_EXTERNAL_DATACAMP_MESSAGE_UNPACKER",
                is_vertical=True
            )
        if self.config.sortdc_fast_updates_topic:
            self.new_config.create_sortdc_fast_updates_pipeline(
                self.config,
                self.config.sortdc_fast_updates_topic
            )

        if self.config.promo_in_messages_topic:
            self.new_config.create_united_offers_pipeline(
                self.config,
                self.config.promo_in_messages_topic,
            )
        if self.config.quick_pipeline_topic:
            self.new_config.create_united_offers_pipeline(
                self.config,
                self.config.quick_pipeline_topic
            )
        if self.config.foodtech_quick_pipeline_topic:
            self.new_config.create_foodtech_quick_pipeline(
                self.config,
                self.config.foodtech_quick_pipeline_topic
            )
        if self.config.promo_topic:
            self.new_config.create_promo_sender(self.config)
        if self.config.united_miner_reader_topic:
            self.new_config.create_united_miner_reader(self.config, self.config.united_miner_reader_topic)
        if self.config.enable_subscription_dispatcher:
            self.new_config.create_subscription_dispatcher(self.config)
        if self.config.united_miner_topic and (
            self.config.api_data_topic or self.config.qoffers_topic or self.config.datacamp_messages_topic
        ):
            self.new_config.create_united_miner_sender(self.config)
        if self.config.partner_stock_topic and self.config.qoffers_topic:
            self.new_config.create_partner_stock_sender(self.config.partner_stock_topic)
        if self.config.picrobot_topic and (self.config.qoffers_topic or self.config.datacamp_messages_topic):
            self.new_config.create_picrobot_sender(self.config)
        if self.config.stock_storage_topic:
            self.new_config.create_stock_storage_pipeline(
                self.config,
                self.config.yt_offers_tablepath,
                self.config.stock_storage_topic,
                self.config.yt_basic_offers_tablepath,
                self.config.yt_service_offers_tablepath,
                self.config.yt_actual_service_offers_tablepath,
                self.config.yt_categories_tablepath,
                self.config.yt_partners_tablepath,
                fill_business_id=True,
                fill_warehouse_id=True,
            )
        if self.config.amore_topic:
            self.new_config.create_amore_pipeline(self.config)
        if self.config.amore_united_topic:
            self.new_config.create_amore_united_pipeline(self.config)
        if self.config.promos_topic:
            self.new_config.create_promos_pipeline(self.config)
        if self.config.rty_topic:
            self.new_config.create_rty_sender(self.config)
        if self.config.mbo_hiding_topic:
            self.new_config.create_mbo_hiding_pipeline(self.config.mbo_hiding_topic)
        if self.config.bannerland_topic:
            self.new_config.create_bannerland_sender(self.config.bannerland_topic, self.config.yt_partners_tablepath)
        if self.config.sort_dc_topic:
            self.new_config.create_sort_dc_sender(self.config.sort_dc_topic)
        if self.config.sort_dc_cpa_topic:
            self.new_config.create_sort_dc_cpa_sender(self.config.sort_dc_cpa_topic)
        if self.config.bannerland_preview_topic:
            self.new_config.create_bannerland_preview_sender(self.config.bannerland_preview_topic)
        if self.config.mboc_topic:
            self.new_config.create_mboc_sender(
                self.config,
                self.config.mboc_topic,
                self.config.send_consistent_offers_only,
                'diff',
                self.config.mboc_sender_not_accepted_colors
            )
        if self.config.mboc_regular_topic:
            self.new_config.create_mboc_sender(
                self.config,
                self.config.mboc_regular_topic,
                self.config.send_consistent_offers_only,
                'regular',
                self.config.mboc_sender_not_accepted_colors
            )
        if self.config.mboc_foodtech_topic:
            self.new_config.create_mboc_sender(
                self.config,
                self.config.mboc_foodtech_topic,
                self.config.send_consistent_offers_only,
                mode='diff',
                not_accepted_colors=self.config.mboc_sender_not_accepted_colors,
                accepted_colors=self.config.mboc_sender_accepted_colors,
            )
        if self.config.samovar_topic:
            self.new_config.create_samovar_sender(self.config)
        if self.config.mboc_offer_state_updates_topic:
            self.new_config.create_mboc_offer_state_updates(self.config.mboc_offer_state_updates_topic)
        if self.config.mboc_offer_1p_new_and_updates_topic:
            self.new_config.create_mboc_offer_1p_new_and_updates(self.config.mboc_offer_1p_new_and_updates_topic)
        if self.config.mdm_topic:
            self.new_config.create_mdm_sender(self.config)
        if self.config.cm_topic:
            self.new_config.create_cm_sender(self.config)
        if self.config.pricelabs_topic:
            self.new_config.create_pricelabs_sender(self.config)
        if self.config.iris_topic:
            self.new_config.create_iris_sender(self.config)
        if self.config.loyalty_promos_topic:
            self.new_config.create_loyalty_promos_sender(self.config)
        if self.config.mdm_mbo_msku_content_topic:
            self.new_config.create_mbo_mdm_msku_pipeline(
                self.config.yt_datacamp_msku_tablepath,
                self.config.mdm_mbo_msku_content_topic,
                self.config.msku_allowed_current_types
            )
        if self.config.direct_out_topic:
            self.new_config.create_direct_sender(self.config)
        if self.config.direct_in_topic:
            self.new_config.create_direct_moderation_response_pipeline(self.config.direct_in_topic)
        if self.config.deepmind_topic:
            self.new_config.create_deepmind_sender(self.config.deepmind_topic)

        if self.config.fresh_offers_tablepath_white and self.config.fresh_offers_tablepath_blue:
            self.new_config.create_fresh_offers_sender_pipeline(
                self.config.fresh_offers_tablepath_white,
                self.config.fresh_offers_tablepath_blue
            )

        if self.config.retry_topic:
            self.new_config.create_retry_sender(self.config)
        if self.config.retry_input_topic:
            self.new_config.create_united_offers_pipeline(self.config, self.config.retry_input_topic, source_from_tech_info=True)

        if self.config.abo_hidings_input_topic:
            self.new_config.create_abo_hidings_pipeline(self.config, self.config.abo_hidings_input_topic)

        if self.config.api_assortment_input_topic:
            self.new_config.create_api_assortment_updates(self.config, self.config.api_assortment_input_topic)
        if self.config.offer_history_topic:
            self.new_config.create_offer_history_sender(self.config.offer_history_topic)
        if self.config.cvdups_topic:
            self.new_config.create_cvdups_response_pipeline(self.config, self.config.cvdups_topic)

        self.new_env = PiperTestEnvNew(**{
            'piper_config': self.new_config,
            'yt_token': self.yt_token,
        })
        self.new_env.__enter__()

        return self

    def __exit__(self, *args):
        self.new_env.__exit__(*args)
        super(PiperTestEnv, self).__exit__(*args)

    @property
    def api_data_processed(self):
        return self.new_env.api_data_processed()

    @property
    def qoffers_processed(self):
        return self.new_env.qoffers_processed()

    @property
    def united_offers_processed(self):
        return self.new_env.united_offers_processed()

    @property
    def subscription_dispatcher_processed(self):
        return self.new_env.subscription_dispatcher_processed()

    @property
    def stock_storage_processed(self):
        return self.new_env.united_offers_processed()

    @property
    def offer_trace_log(self):
        return parse_tracelog(self.trace_log_path)

    @property
    def promo_processed(self):
        return self.new_env.promo_processed()

    @property
    def categories_processed(self):
        return self.new_env.categories_processed()

    @property
    def msku_processed(self):
        return self.new_env.msku_processed()

    @property
    def quoter_api_processed(self):
        return self.new_env.quoter_api_processed()

    @property
    def quoter_datacamp_message_processed(self):
        return self.new_env.quoter_datacamp_message_processed()

    @property
    def quoter_external_message_processed(self):
        return self.new_env.quoter_external_message_processed()

    @property
    def quoter_united_offer_processed(self):
        return self.new_env.quoter_united_offer_processed()
