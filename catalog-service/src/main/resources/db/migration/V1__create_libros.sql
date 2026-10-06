CREATE TABLE libros (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    titulo VARCHAR(255) NOT NULL,
    autor VARCHAR(255) NOT NULL,
    categoria VARCHAR(100) NOT NULL,
    stock_total INTEGER NOT NULL CHECK (stock_total >= 0),
    stock_disponible INTEGER NOT NULL CHECK (stock_disponible >= 0 AND stock_disponible <= stock_total)
);

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible) VALUES
('978-0132350884', 'Clean Code', 'Robert C. Martin', 'Programacion', 5, 5),
('978-0201633610', 'Design Patterns', 'GoF', 'Programacion', 3, 3),
('978-8445073804', 'Cien anios de soledad', 'Gabriel Garcia Marquez', 'Novela', 4, 4);
