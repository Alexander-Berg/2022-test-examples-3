CREATE INDEX IF NOT EXISTS attachments_notification_id_idx ON attachments(notification_id);
CREATE INDEX IF NOT EXISTS authorized_users_passport_id_role ON authorized_users(passport_id, role);
CREATE INDEX IF NOT EXISTS bank_order_payment_batch_id_idx ON bank_orders(payment_batch_id);
CREATE INDEX IF NOT EXISTS billing_transactions_fin_event_idx ON billing_transactions(source_financial_event_id);
CREATE INDEX IF NOT EXISTS billing_transactions_order_pretty_id_idx ON billing_transactions(service_order_id);
CREATE INDEX IF NOT EXISTS billing_transactions_yt_id_idx ON billing_transactions(yt_id);
CREATE INDEX IF NOT EXISTS client_calls_created_at_idx ON client_calls(created_at);
CREATE INDEX IF NOT EXISTS fiscal_item_discounts_fiscal_item_id_idx ON fiscal_item_discounts(fiscal_item_id);
CREATE INDEX IF NOT EXISTS fiscal_item_discounts_promo_code_app_idx ON fiscal_item_discounts(promo_code_application_id);
CREATE INDEX IF NOT EXISTS invoice_item_invoice_id_idx ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS invoices_backgroun_job_active_idx ON invoices(background_job_active);
CREATE INDEX IF NOT EXISTS invoices_pending_invoice_id_idx ON invoices(pending_invoice_id);
CREATE INDEX IF NOT EXISTS notifications_order_id_idx ON notifications(order_id);
CREATE INDEX IF NOT EXISTS order_aggregate_state_changes_created_at_idx ON order_aggregate_state_changes(created_at);
CREATE INDEX IF NOT EXISTS order_items_background_jbo_active_idx ON order_items(background_job_active);
CREATE INDEX IF NOT EXISTS order_items_item_type_idx ON order_items(item_type);
CREATE INDEX IF NOT EXISTS order_items_next_check_confirmed_at_idx ON order_items(next_check_confirmed_at);
CREATE INDEX IF NOT EXISTS order_label_order_id_idx ON order_label_params(order_id);
CREATE INDEX IF NOT EXISTS order_refunds_order_id_idx ON order_refunds(order_id);
CREATE INDEX IF NOT EXISTS order_srch_odtype_ptype_ostate_idx ON order_search_documents(display_order_type, partner_type, order_state);
CREATE INDEX IF NOT EXISTS order_srch_otype_ptype_ostate_idx ON order_search_documents(order_type, partner_type, order_state);
CREATE INDEX IF NOT EXISTS orders_order_type_idx ON orders(order_type);
CREATE INDEX IF NOT EXISTS orders_payment_schedule_id_idx ON orders(payment_schedule_id);
CREATE INDEX IF NOT EXISTS orders_state_idx ON orders(state);
CREATE INDEX IF NOT EXISTS orders_updated_at_idx ON orders(updated_at);
CREATE INDEX IF NOT EXISTS promo_2020_08_taxi_orders_status_idx ON promo_2020_08_taxi_orders(status);
CREATE INDEX IF NOT EXISTS promo_code_applications_order_id_idx ON promo_code_applications(order_id);
CREATE INDEX IF NOT EXISTS train_ticket_refunds_order_item_idx ON train_ticket_refunds(order_item_id);
CREATE INDEX IF NOT EXISTS train_ticket_refunds_order_refund_idx ON train_ticket_refunds(order_refund_id);
CREATE INDEX IF NOT EXISTS workflows_entity_type_idx ON workflows(entity_type);
CREATE INDEX IF NOT EXISTS workflows_supervisor_id_idx ON workflows(supervisor_id);
