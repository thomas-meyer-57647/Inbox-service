CREATE INDEX idx_attachment_company_message
    ON inbox_attachment_ref(company_id, message_id);
