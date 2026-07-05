ALTER TABLE teams ADD COLUMN category VARCHAR(20) UNIQUE
    CONSTRAINT chk_teams_category
        CHECK (category IN ('NETWORK', 'HARDWARE', 'ACCESS', 'SOFTWARE'));

INSERT INTO teams (name, category) VALUES
                                       ('Network Support',     'NETWORK'),
                                       ('Workstation Support', 'HARDWARE'),
                                       ('Access Management',   'ACCESS'),
                                       ('Software Support',    'SOFTWARE');
