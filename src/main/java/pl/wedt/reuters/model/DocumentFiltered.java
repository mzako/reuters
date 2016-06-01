package pl.wedt.reuters.model;

/**
 * @author Michał Żakowski
 *
 * Klasa reprezentująca dokument po przetworzeniu, w postaci wektora częstości słów
 */
public class DocumentFiltered {
    private DocumentType documentType;
    private CategoryType categoryType;
    private Integer category;
    private double vector[];

    public DocumentType getDocumentType() {
        return documentType;
    }

    public CategoryType getCategoryType() {
        return categoryType;
    }

    public Integer getCategory() {
        return category;
    }

    public double[] getVector() {
        return vector;
    }

    public DocumentFiltered(DocumentType documentType, CategoryType categoryType, Integer category,
                            double[] vector) {
        this.documentType = documentType;
        this.categoryType = categoryType;
        this.category = category;
        this.vector = vector;
    }
}
