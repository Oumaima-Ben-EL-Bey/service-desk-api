INSERT INTO users (email, full_name, team_id, password_hash) VALUES
                                                                 ('admin@servicedesk.local', 'Admin User', NULL,
                                                                  '$2a$10$tISpfxAD.QJg4hEBoA7IB.1ObQUWEVcxpVPMz9t33QAsddMnavNNq'),
                                                                 ('agent1@servicedesk.local', 'Agent One',
                                                                  (SELECT id FROM teams WHERE category = 'NETWORK'),
                                                                  '$2a$10$tISpfxAD.QJg4hEBoA7IB.1ObQUWEVcxpVPMz9t33QAsddMnavNNq'),
                                                                 ('agent2@servicedesk.local', 'Agent Two',
                                                                  (SELECT id FROM teams WHERE category = 'HARDWARE'),
                                                                  '$2a$10$tISpfxAD.QJg4hEBoA7IB.1ObQUWEVcxpVPMz9t33QAsddMnavNNq'),
                                                                 ('requester@servicedesk.local', 'Requester User', NULL,
                                                                  '$2a$10$tISpfxAD.QJg4hEBoA7IB.1ObQUWEVcxpVPMz9t33QAsddMnavNNq');

INSERT INTO user_roles (user_id, role_id) VALUES
                                              ((SELECT id FROM users WHERE email = 'admin@servicedesk.local'),
                                               (SELECT id FROM roles WHERE name = 'ADMIN')),
                                              ((SELECT id FROM users WHERE email = 'agent1@servicedesk.local'),
                                               (SELECT id FROM roles WHERE name = 'AGENT')),
                                              ((SELECT id FROM users WHERE email = 'agent2@servicedesk.local'),
                                               (SELECT id FROM roles WHERE name = 'AGENT')),
                                              ((SELECT id FROM users WHERE email = 'requester@servicedesk.local'),
                                               (SELECT id FROM roles WHERE name = 'REQUESTER'));

INSERT INTO tickets (title, description, status, category, requester_id, assignee_id, team_id) VALUES
                                                                                                   ('Cannot connect to VPN',
                                                                                                    'The VPN client fails with a timeout error when connecting from home.',
                                                                                                    'NEW', 'NETWORK',
                                                                                                    (SELECT id FROM users WHERE email = 'requester@servicedesk.local'),
                                                                                                    NULL,
                                                                                                    (SELECT id FROM teams WHERE category = 'NETWORK')),
                                                                                                   ('New laptop keeps freezing',
                                                                                                    'The replacement laptop freezes several times a day and needs a restart.',
                                                                                                    'IN_PROGRESS', 'HARDWARE',
                                                                                                    (SELECT id FROM users WHERE email = 'requester@servicedesk.local'),
                                                                                                    (SELECT id FROM users WHERE email = 'agent2@servicedesk.local'),
                                                                                                    (SELECT id FROM teams WHERE category = 'HARDWARE')),
                                                                                                   ('Wifi drops in meeting room B',
                                                                                                    'Wireless connection drops every few minutes in the second-floor meeting room.',
                                                                                                    'RESOLVED', 'NETWORK',
                                                                                                    (SELECT id FROM users WHERE email = 'requester@servicedesk.local'),
                                                                                                    (SELECT id FROM users WHERE email = 'agent1@servicedesk.local'),
                                                                                                    (SELECT id FROM teams WHERE category = 'NETWORK'));