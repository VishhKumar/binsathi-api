-- V1: tenants, users, locations, refresh tokens, audit log

CREATE TABLE organisation (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name          varchar(200) NOT NULL,
    type          varchar(30)  NOT NULL,
    contact_email varchar(255),
    logo_url      varchar(500),
    status        varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT chk_org_type CHECK (type IN ('SOCIETY', 'COMPANY', 'MUNICIPALITY'))
);

CREATE TABLE app_user (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id uuid REFERENCES organisation (id),
    name            varchar(200) NOT NULL,
    email           varchar(255) NOT NULL,
    phone           varchar(30),
    password_hash   varchar(100) NOT NULL,
    role            varchar(30)  NOT NULL,
    status          varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at   timestamptz,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT chk_user_role CHECK (role IN ('SUPER_ADMIN', 'ORG_ADMIN', 'COLLECTOR', 'RESIDENT')),
    -- only the platform super admin has no organisation
    CONSTRAINT chk_user_org CHECK ((role = 'SUPER_ADMIN') = (organisation_id IS NULL))
);
CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));
CREATE INDEX idx_app_user_org ON app_user (organisation_id);

CREATE TABLE area (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id uuid NOT NULL REFERENCES organisation (id),
    name            varchar(200) NOT NULL,
    city            varchar(100),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_area_org ON area (organisation_id);

CREATE TABLE building (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id uuid NOT NULL REFERENCES organisation (id),
    area_id         uuid NOT NULL REFERENCES area (id),
    name            varchar(200) NOT NULL,
    address         varchar(500),
    latitude        double precision,
    longitude       double precision,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_building_org ON building (organisation_id);
CREATE INDEX idx_building_area ON building (area_id);

CREATE TABLE household (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id  uuid NOT NULL REFERENCES organisation (id),
    building_id      uuid NOT NULL REFERENCES building (id),
    unit_number      varchar(50) NOT NULL,
    resident_user_id uuid REFERENCES app_user (id),
    monthly_fee      numeric(10, 2) NOT NULL DEFAULT 0,
    status           varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    UNIQUE (building_id, unit_number)
);
CREATE INDEX idx_household_org ON household (organisation_id);
CREATE INDEX idx_household_building ON household (building_id);

CREATE TABLE refresh_token (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL REFERENCES app_user (id),
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked    boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

CREATE TABLE audit_log (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organisation_id uuid,
    user_id         uuid,
    action          varchar(100) NOT NULL,
    entity          varchar(100),
    entity_id       varchar(100),
    details         jsonb,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_org_time ON audit_log (organisation_id, created_at);