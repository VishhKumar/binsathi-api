-- V3: complaints and their comment threads

CREATE TABLE complaint (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id uuid NOT NULL REFERENCES organisation (id),
    household_id    uuid NOT NULL REFERENCES household (id),
    category        varchar(30) NOT NULL,
    description     varchar(1000) NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'OPEN',
    resolution_note varchar(1000),
    resolved_at     timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_complaint_category CHECK (category IN
        ('MISSED_PICKUP', 'OVERFLOWING_BIN', 'ILLEGAL_DUMPING', 'COLLECTOR_BEHAVIOUR', 'OTHER')),
    CONSTRAINT chk_complaint_status CHECK (status IN
        ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED'))
);
CREATE INDEX idx_complaint_org_status ON complaint (organisation_id, status, created_at);
CREATE INDEX idx_complaint_household ON complaint (household_id);

CREATE TABLE complaint_comment (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id uuid NOT NULL REFERENCES organisation (id),
    complaint_id    uuid NOT NULL REFERENCES complaint (id),
    author_id       uuid NOT NULL REFERENCES app_user (id),
    message         varchar(1000) NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_comment_complaint ON complaint_comment (complaint_id, created_at);