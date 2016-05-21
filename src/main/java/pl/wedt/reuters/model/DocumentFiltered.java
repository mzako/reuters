package pl.wedt.reuters.model;

import java.util.List;

/**
 * @author Michał Żakowski
 *
 * Klasa reprezentująca dokument po przetworzeniu, w postaci wektora częstości słów
 */
public class DocumentFiltered {
    private DocumentType documentType;
    private CategoryType categoryType;
    private Integer category;
    private List<Integer> vector;

    public DocumentFiltered(DocumentType documentType, CategoryType categoryType, Integer category,
                            List<Integer> vector) {
        this.documentType = documentType;
        this.categoryType = categoryType;
        this.category = category;
        this.vector = vector;
    }
}
