DELETE FROM comments;
DELETE FROM users;
DELETE FROM categories;
DELETE FROM locations;
DELETE FROM events;

INSERT INTO users (name, email) VALUES ('user1', 'user1@mail.ru');
INSERT INTO users (name, email) VALUES ('user2', 'user2@mail.ru');

INSERT INTO categories (name) VALUES ('Кино');

INSERT INTO locations (lat, lon) VALUES ('55.75', '37.61');

INSERT INTO events (annotation, category_id, created_on, description, event_date, initiator_id, location_id, paid,
                    participant_limit, request_moderation, title)
VALUES ('Премьера фантастического блокбастера', 1, '2020-03-03 10:00:00', 'Показ фильма в IMAX с последующей дискуссией о спецэффектах',
        '2024-03-03 10:00:00', 2, 1, 'false', 0, 'true', 'Премьера 2024');
INSERT INTO events (annotation, category_id, created_on, description, event_date, initiator_id, location_id, paid,
                    participant_limit, request_moderation, title)
VALUES ('Марафон классических комедий', 1, '2020-03-03 10:00:00', '12 часов непрерывного показа лучших комедий мирового кинематографа',
        '2024-03-03 10:00:00', 2, 1, 'false', 0, 'true', 'Киномарафон');

UPDATE events SET state = 'PUBLISHED' WHERE id = 1;

INSERT INTO comments (text, author_id, event_id, created, edited)
VALUES ('Фильм потрясающий, спецэффекты на высоте!', 1, 1, '2024-03-04 12:00:00', NULL);

INSERT INTO comments (text, author_id, event_id, created, edited)
VALUES ('Сюжет немного предсказуем, но визуал впечатляет.', 1, 1, '2024-03-04 14:30:00', NULL);

INSERT INTO comments (text, author_id, event_id, created, edited)
VALUES ('Марафон удался, но кресла оказались неудобными.', 2, 2, '2024-03-05 10:00:00', NULL);