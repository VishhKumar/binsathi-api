-- V2: collection schedules, pickups and collector assignments

CREATE TABLE schedule (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id uuid NOT NULL REFERENCES organisation (id),
    area_id         uuid NOT NULL REFERENCES area (id),
    waste_type      varchar(30) NOT NULL,
    days_of_week    varchar(40) NOT NULL,
    window_start    time NOT NULL,
    window_end      time NOT NULL,
    active          boolean NOT NULL DEFAULT true,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_schedule_waste CHECK (waste_type IN
        ('WET', 'DRY', 'RECYCLABLE', 'HAZARDOUS', 'E_WASTE', 'BULKY', 'GARDEN')),
    CONSTRAINT chk_schedule_window CHECK (window_end > window_start),
    UNIQUE (area_id, waste_type)
);
CREATE INDEX idx_schedule_org ON schedule (organisation_id);

CREATE TABLE pickup (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id uuid NOT NULL REFERENCES organisation (id),
    household_id    uuid NOT NULL REFERENCES household (id),
    schedule_id     uuid REFERENCES schedule (id),
    waste_type      varchar(30) NOT NULL,
    scheduled_date  date NOT NULL,
    window_start    time NOT NULL,
    window_end      time NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'SCHEDULED',
    collector_id    uuid REFERENCES app_user (id),
    completed_at    timestamptz,
    skip_reason     varchar(300),
    proof_photo_url varchar(500),
    proof_lat       double precision,
    proof_lng       double precision,
    is_on_demand    boolean NOT NULL DEFAULT false,
    notes           varchar(500),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_pickup_status CHECK (status IN
        ('SCHEDULED', 'COMPLETED', 'SKIPPED', 'RESCHEDULED', 'CANCELLED')),
    UNIQUE (household_id, schedule_id, scheduled_date)
);
CREATE INDEX idx_pickup_org_date ON pickup (organisation_id, scheduled_date, status);
CREATE INDEX idx_pickup_collector_date ON pickup (collector_id, scheduled_date);
CREATE INDEX idx_pickup_household ON pickup (household_id);

CREATE TABLE collector_assignment (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id uuid NOT NULL REFERENCES organisation (id),
    collector_id    uuid NOT NULL REFERENCES app_user (id),
    area_id         uuid NOT NULL REFERENCES area (id),
    created_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (collector_id, area_id)
);
CREATE INDEX idx_assignment_org ON collector_assignment (organisation_id);