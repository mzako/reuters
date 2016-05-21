package pl.wedt.reuters.model;

import java.util.List;

/**
 * @author Michał Żakowski
 *
 * Klasa reprezentująca dokument Reuters-21578 w postaci nieprzetworzonej
 */
public class DocumentRaw {
    private DocumentType documentType;
    private List<Integer> exchanges;
    private List<Integer> orgs;
    private List<Integer> people;
    private List<Integer> places;
    private List<Integer> topics;
    private String body;

    public DocumentRaw(String body, DocumentType documentType, List<Integer> exchanges, List<Integer> orgs,
                       List<Integer> people, List<Integer> places, List<Integer> topics) {
        this.body = body;
        this.documentType = documentType;
        this.exchanges = exchanges;
        this.orgs = orgs;
        this.people = people;
        this.places = places;
        this.topics = topics;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public List<Integer> getTopics() {
        return topics;
    }

    public List<Integer> getPlaces() {
        return places;
    }

    public List<Integer> getPeople() {
        return people;
    }

    public List<Integer> getOrgs() {
        return orgs;
    }

    public List<Integer> getExchanges() {
        return exchanges;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "DocumentRaw{" +
                "documentType=" + documentType +
                ", exchanges=" + exchanges +
                ", orgs=" + orgs +
                ", people=" + people +
                ", places=" + places +
                ", topics=" + topics +
                ", body='" + body + '\'' +
                '}';
    }
}
