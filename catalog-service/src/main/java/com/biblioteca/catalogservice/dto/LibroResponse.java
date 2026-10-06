package com.biblioteca.catalogservice.dto;

import com.biblioteca.catalogservice.entity.Libro;

public class LibroResponse {
    private Long id;
    private String isbn;
    private String titulo;
    private String autor;
    private String categoria;
    private Integer stockTotal;
    private Integer stockDisponible;

    public static LibroResponse from(Libro l) {
        LibroResponse r = new LibroResponse();
        r.id = l.getId();
        r.isbn = l.getIsbn();
        r.titulo = l.getTitulo();
        r.autor = l.getAutor();
        r.categoria = l.getCategoria();
        r.stockTotal = l.getStockTotal();
        r.stockDisponible = l.getStockDisponible();
        return r;
    }

    public Long getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public String getCategoria() { return categoria; }
    public Integer getStockTotal() { return stockTotal; }
    public Integer getStockDisponible() { return stockDisponible; }
}
