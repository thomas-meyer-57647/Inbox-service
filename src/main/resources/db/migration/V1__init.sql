CREATE TABLE inbox_message (
    id VARCHAR(36) PRIMARY KEY,
    company_id VARCHAR(64) NOT NULL,
    recipient_user_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    source_service VARCHAR(128) NOT NULL,
    correlation_id VARCHAR(128),
    expires_at DATETIME(6) NULL,
    read_at DATETIME(6) NULL,
    read_by VARCHAR(128),
    actions_json TEXT,
    tags_json TEXT,
    external_reference VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    modified_at DATETIME(6) NOT NULL,
    modified_by VARCHAR(128) NOT NULL,
    trashed_at DATETIME(6) NULL,
    trashed_by VARCHAR(128),
    version BIGINT NOT NULL
);

CREATE TABLE inbox_attachment_ref (
    id VARCHAR(36) PRIMARY KEY,
    company_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(36) NOT NULL,
    file_id VARCHAR(128),
    storage_key VARCHAR(255),
    download_url VARCHAR(1024),
    filename VARCHAR(255),
    mime_type VARCHAR(128),
    size_bytes BIGINT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_attachment_message FOREIGN KEY (message_id) REFERENCES inbox_message(id)
);

CREATE INDEX idx_message_company_user_created
    ON inbox_message(company_id, recipient_user_id, created_at);

CREATE INDEX idx_message_company_user_status_created
    ON inbox_message(company_id, recipient_user_id, status, created_at);

CREATE INDEX idx_message_company_expires
    ON inbox_message(company_id, expires_at);

CREATE INDEX idx_message_company_user_trashed
    ON inbox_message(company_id, recipient_user_id, trashed_at);

CREATE INDEX idx_attachment_message_id
    ON inbox_attachment_ref(message_id);

CREATE INDEX idx_attachment_company_id
    ON inbox_attachment_ref(company_id);
