CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_files (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    size BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, filename)
);

CREATE INDEX IF NOT EXISTS idx_user_files_user_id ON user_files(user_id);
CREATE INDEX IF NOT EXISTS idx_user_files_filename ON user_files(filename);
CREATE INDEX IF NOT EXISTS idx_users_login ON users(login);

-- Создание тестового пользователя
-- Пароль: "test" (зашифрован через BCrypt)
INSERT INTO users (login, password)
VALUES ('test@mail.ru', '$2a$10$N9qo8uLOickgx2ZMRZoMye0rN5KJ1pW6LQJYfJ7RlZ7QbK1VYzWXe')
ON CONFLICT (login) DO NOTHING;